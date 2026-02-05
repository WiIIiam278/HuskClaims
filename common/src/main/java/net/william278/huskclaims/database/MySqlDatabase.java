/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.database;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.zaxxer.hikari.HikariDataSource;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("DuplicatedCode")
public class MySqlDatabase extends Database {

    private static final String DATA_POOL_NAME = "HuskClaimsHikariPool";
    private final String flavor;
    private final String driverClass;
    private HikariDataSource dataSource;

    public MySqlDatabase(@NotNull HuskClaims plugin) {
        super(plugin);
        this.flavor = plugin.getSettings().getDatabase().getType() == Type.MARIADB
                ? "mariadb" : "mysql";
        this.driverClass = plugin.getSettings().getDatabase().getType() == Type.MARIADB
                ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver";
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void setConnection() {
        // Initialize the Hikari pooled connection
        final Settings.DatabaseSettings databaseSettings = plugin.getSettings().getDatabase();
        final Settings.DatabaseSettings.DatabaseCredentials credentials = databaseSettings.getCredentials();

        dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setJdbcUrl(String.format("jdbc:%s://%s:%s/%s%s",
                flavor,
                credentials.getHost(),
                credentials.getPort(),
                credentials.getDatabase(),
                credentials.getParameters()
        ));

        // Authenticate with the database
        dataSource.setUsername(credentials.getUsername());
        dataSource.setPassword(credentials.getPassword());

        // Set connection pool options
        final Settings.DatabaseSettings.PoolOptions poolOptions = databaseSettings.getPoolOptions();
        dataSource.setMaximumPoolSize(poolOptions.getSize());
        dataSource.setMinimumIdle(poolOptions.getIdle());
        dataSource.setMaxLifetime(poolOptions.getLifetime());
        dataSource.setKeepaliveTime(poolOptions.getKeepAlive());
        dataSource.setConnectionTimeout(poolOptions.getTimeout());
        dataSource.setPoolName(DATA_POOL_NAME);

        // Set additional connection pool properties
        final Properties properties = new Properties();
        properties.putAll(
                Map.of("cachePrepStmts", "true",
                        "prepStmtCacheSize", "250",
                        "prepStmtCacheSqlLimit", "2048",
                        "useServerPrepStmts", "true",
                        "useLocalSessionState", "true",
                        "useLocalTransactionState", "true"
                ));
        properties.putAll(
                Map.of(
                        "rewriteBatchedStatements", "true",
                        "cacheResultSetMetadata", "true",
                        "cacheServerConfiguration", "true",
                        "elideSetAutoCommits", "true",
                        "maintainTimeStats", "false")
        );
        dataSource.setDataSourceProperties(properties);
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    protected void executeScript(@NotNull Connection connection, @NotNull String name) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String schemaStatement : getScript(name)) {
                statement.execute(schemaStatement);
            }
        }
    }

    @Override
    public void initialize() throws RuntimeException {
        // Establish connection
        this.setConnection();

        // Create tables
        if (!isCreated()) {
            plugin.log(Level.INFO, "Creating MySql database tables");
            try {
                executeScript(getConnection(), String.format("%s_schema.sql", flavor));
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "Failed to create MySql database tables", e);
                setLoaded(false);
                return;
            }
            setSchemaVersion(Migration.getLatestVersion());
            plugin.log(Level.INFO, "MySql database tables created!");
            setLoaded(true);
            return;
        }

        // Perform migrations
        try {
            performMigrations(getConnection(), plugin.getSettings().getDatabase().getType());
            setLoaded(true);
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to perform MySql database migrations", e);
            setLoaded(false);
        }
    }

    @Override
    public boolean isCreated() {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `uuid`
                    FROM `%user_data%`
                    LIMIT 1;"""))) {
                statement.executeQuery();
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public int getSchemaVersion() {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `schema_version`
                    FROM `%meta_data%`
                    LIMIT 1;"""))) {
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getInt("schema_version");
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "The database schema version could not be fetched; migrations will be carried out.");
        }
        return -1;
    }

    @Override
    public void setSchemaVersion(int version) {
        if (getSchemaVersion() == -1) {
            try (Connection connection = getConnection()) {
                try (PreparedStatement insertStatement = connection.prepareStatement(format("""
                        INSERT INTO `%meta_data%` (`schema_version`)
                        VALUES (?)"""))) {
                    insertStatement.setInt(1, version);
                    insertStatement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "Failed to insert schema version in table", e);
            }
            return;
        }

        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    UPDATE `%meta_data%`
                    SET `schema_version` = ?;"""))) {
                statement.setInt(1, version);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to update schema version in table", e);
        }
    }

    @Override
    public Optional<SavedUser> getUser(@NotNull UUID uuid) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `uuid`, `username`, `last_login`, `claim_blocks`, `preferences`, `spent_claim_blocks`
                    FROM `%user_data%`
                    WHERE uuid = ?"""))) {
                statement.setString(1, uuid.toString());
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final String name = resultSet.getString("username");
                    final String preferences = new String(resultSet.getBytes("preferences"), StandardCharsets.UTF_8);
                    return Optional.of(new SavedUser(
                            User.of(uuid, name),
                            plugin.getPreferencesFromJson(preferences),
                            resultSet.getTimestamp("last_login").toLocalDateTime()
                                    .atOffset(OffsetDateTime.now().getOffset()),
                            resultSet.getLong("claim_blocks"),
                            resultSet.getLong("spent_claim_blocks")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch user data from table by UUID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SavedUser> getUser(@NotNull String username) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `uuid`, `username`, `last_login`, `claim_blocks`, `preferences`, `spent_claim_blocks`
                    FROM `%user_data%`
                    WHERE LOWER(`username`) = LOWER(?)"""))) {
                statement.setString(1, username);
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    final String name = resultSet.getString("username");
                    final String preferences = new String(resultSet.getBytes("preferences"), StandardCharsets.UTF_8);
                    return Optional.of(new SavedUser(
                            User.of(uuid, name),
                            plugin.getPreferencesFromJson(preferences),
                            resultSet.getTimestamp("last_login").toLocalDateTime()
                                    .atOffset(OffsetDateTime.now().getOffset()),
                            resultSet.getLong("claim_blocks"),
                            resultSet.getLong("spent_claim_blocks")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch user data from table by username", e);
        }
        return Optional.empty();
    }

    @Override
    public List<SavedUser> getInactiveUsers(long daysInactive) {
        final List<SavedUser> inactiveUsers = Lists.newArrayList();
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `uuid`, `username`, `last_login`, `preferences`, `claim_blocks`, `spent_claim_blocks`
                    FROM `%user_data%`
                    WHERE `last_login` < DATE_SUB(NOW(), INTERVAL ? DAY);"""))) {
                statement.setLong(1, daysInactive);
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    final String name = resultSet.getString("username");
                    final String preferences = new String(resultSet.getBytes("preferences"), StandardCharsets.UTF_8);
                    inactiveUsers.add(new SavedUser(
                            User.of(uuid, name),
                            plugin.getPreferencesFromJson(preferences),
                            resultSet.getTimestamp("last_login").toLocalDateTime()
                                    .atOffset(OffsetDateTime.now().getOffset()),
                            resultSet.getLong("claim_blocks"),
                            resultSet.getLong("spent_claim_blocks")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch list of inactive users", e);
            inactiveUsers.clear(); // Clear for safety to prevent any accidental data being returned
        }
        return inactiveUsers;
    }

    @Override
    public void createUser(@NotNull SavedUser saved) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    INSERT INTO `%user_data%` (`uuid`, `username`, `last_login`, `claim_blocks`, `preferences`, `spent_claim_blocks`)
                    VALUES (?, ?, ?, ?, ?, ?)"""))) {
                statement.setString(1, saved.getUser().getUuid().toString());
                statement.setString(2, saved.getUser().getName());
                statement.setTimestamp(3, Timestamp.valueOf(saved.getLastLogin().toLocalDateTime()));
                statement.setLong(4, saved.getClaimBlocks());
                statement.setBytes(5, plugin.getGson().toJson(saved.getPreferences())
                        .getBytes(StandardCharsets.UTF_8));
                statement.setLong(6, saved.getSpentClaimBlocks());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to create user in table", e);
        }
    }

    @Override
    public void updateUser(@NotNull SavedUser user) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    UPDATE `%user_data%`
                    SET `claim_blocks` = ?, `preferences` = ?, `spent_claim_blocks` = ?
                    WHERE `uuid` = ?"""))) {
                statement.setLong(1, user.getClaimBlocks());
                statement.setBytes(2, plugin.getGson().toJson(user.getPreferences())
                        .getBytes(StandardCharsets.UTF_8));
                statement.setLong(3, user.getSpentClaimBlocks());
                statement.setString(4, user.getUser().getUuid().toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to update Saved User data in table", e);
        }
    }

    @Override
    public void createOrUpdateUser(@NotNull SavedUser data) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    INSERT INTO `%user_data%` (`uuid`, `username`, `last_login`, `claim_blocks`, `preferences`, `spent_claim_blocks`)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE `username` = ?, `last_login` = ?, `claim_blocks` = ?, `preferences` = ?, `spent_claim_blocks` = ?;"""))) {
                final byte[] prefs = plugin.getGson().toJson(data.getPreferences()).getBytes(StandardCharsets.UTF_8);
                statement.setString(1, data.getUser().getUuid().toString());
                statement.setString(2, data.getUser().getName());
                statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                statement.setLong(4, data.getClaimBlocks());
                statement.setBytes(5, prefs);
                statement.setLong(6, data.getSpentClaimBlocks());
                statement.setString(7, data.getUser().getName());
                statement.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                statement.setLong(9, data.getClaimBlocks());
                statement.setBytes(10, prefs);
                statement.setLong(11, data.getSpentClaimBlocks());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to create or update user in table", e);
        }
    }

    @NotNull
    @Override
    public Set<UserGroup> getUserGroups(@NotNull UUID uuid) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `name`, `members`
                    FROM `%user_group_data%`
                    WHERE `uuid` = ?"""))) {
                statement.setString(1, uuid.toString());
                final ResultSet resultSet = statement.executeQuery();
                final Set<UserGroup> userGroups = Sets.newHashSet();
                while (resultSet.next()) {
                    userGroups.add(new UserGroup(
                            uuid,
                            resultSet.getString("name"),
                            plugin.getUserListFromJson(new String(
                                    resultSet.getBytes("members"), StandardCharsets.UTF_8
                            ))
                    ));
                }
                return userGroups;
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch user groups from table", e);
        }
        return Sets.newHashSet();
    }

    @NotNull
    @Override
    public Map<UUID, Set<UserGroup>> getAllUserGroups() {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `uuid`, `name`, `members`
                    FROM `%user_group_data%`"""))) {
                final ResultSet resultSet = statement.executeQuery();
                final Map<UUID, Set<UserGroup>> userGroups = Maps.newHashMap();
                while (resultSet.next()) {
                    final UserGroup userGroup = new UserGroup(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name"),
                            plugin.getUserListFromJson(new String(
                                    resultSet.getBytes("members"), StandardCharsets.UTF_8
                            ))
                    );
                    userGroups.compute(userGroup.groupOwner(), (key, value) -> {
                        if (value == null) {
                            value = Sets.newHashSet();
                        }
                        value.add(userGroup);
                        return value;
                    });
                }
                return userGroups;
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch user groups from table", e);
        }
        return Maps.newHashMap();
    }

    @Override
    public void addUserGroup(@NotNull UserGroup group) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    INSERT INTO `%user_group_data%` (`uuid`, `name`, `members`)
                    VALUES (?, ?, ?)"""))) {
                statement.setString(1, group.groupOwner().toString());
                statement.setString(2, group.name());
                statement.setBytes(3, plugin.getGson().toJson(group.members()).getBytes(StandardCharsets.UTF_8));
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to create user group in table", e);
        }
    }

    @Override
    public void updateUserGroup(@NotNull UUID owner, @NotNull String name, @NotNull UserGroup newGroup) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    UPDATE `%user_group_data%`
                    SET `name` = ?, `members` = ?
                    WHERE `uuid` = ? AND `name` = ?"""))) {
                statement.setString(1, newGroup.name());
                statement.setBytes(2, plugin.getGson().toJson(newGroup.members()).getBytes(StandardCharsets.UTF_8));
                statement.setString(3, owner.toString());
                statement.setString(4, name);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to update user group in table", e);
        }
    }

    @Override
    public void deleteUserGroup(@NotNull UserGroup group) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    DELETE FROM `%user_group_data%`
                    WHERE `uuid` = ? AND `name` = ?"""))) {
                statement.setString(1, group.groupOwner().toString());
                statement.setString(2, group.name());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to remove user group from table", e);
        }
    }

    @NotNull
    @Override
    public Map<World, ClaimWorld> getClaimWorlds(@NotNull String server) throws IllegalStateException {
        final Map<World, ClaimWorld> worlds = Maps.newHashMap();
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `id`, `world_uuid`, `world_name`, `world_environment`, `data`
                    FROM `%claim_data%`
                    WHERE `server_name` = ?"""))) {
                statement.setString(1, server);
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final World world = World.of(
                            resultSet.getString("world_name"),
                            UUID.fromString(resultSet.getString("world_uuid")),
                            resultSet.getString("world_environment")
                    );
                    final int id = resultSet.getInt("id");
                    final ClaimWorld claimWorld = plugin.getClaimWorldFromJson(id,
                            new String(resultSet.getBytes("data"), StandardCharsets.UTF_8)
                    );
                    claimWorld.updateId(id);
                    if (!plugin.getSettings().getClaims().isWorldUnclaimable(world)) {
                        worlds.put(world, claimWorld);
                    }
                }
            }
        } catch (SQLException | JsonSyntaxException e) {
            throw new IllegalStateException(String.format("Failed to fetch claim world map for %s", server), e);
        }
        return worlds;
    }

    @NotNull
    @Override
    public Map<ServerWorld, ClaimWorld> getAllClaimWorlds() throws IllegalStateException {
        final Map<ServerWorld, ClaimWorld> worlds = Maps.newHashMap();
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    SELECT `id`, `server_name`, `world_uuid`, `world_name`, `world_environment`, `data`
                    FROM `%claim_data%`"""))) {
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final World world = World.of(
                            resultSet.getString("world_name"),
                            UUID.fromString(resultSet.getString("world_uuid")),
                            resultSet.getString("world_environment")
                    );
                    final int id = resultSet.getInt("id");
                    final ClaimWorld claimWorld = plugin.getClaimWorldFromJson(id,
                            new String(resultSet.getBytes("data"), StandardCharsets.UTF_8)
                    );
                    claimWorld.updateId(id);
                    worlds.put(new ServerWorld(resultSet.getString("server_name"), world), claimWorld);
                }
            }
        } catch (SQLException | JsonSyntaxException e) {
            throw new IllegalStateException("Failed to fetch map of all claim worlds", e);
        }
        return worlds;
    }


    @Override
    @NotNull
    public ClaimWorld createClaimWorld(@NotNull World world) {
        final ClaimWorld claimWorld = ClaimWorld.create(plugin);
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    INSERT INTO `%claim_data%` (`world_uuid`, `world_name`, `world_environment`, `server_name`, `data`)
                    VALUES (?, ?, ?, ?, ?)"""), Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, world.getUuid().toString());
                statement.setString(2, world.getName());
                statement.setString(3, world.getEnvironment());
                statement.setString(4, plugin.getServerName());
                statement.setBytes(5, plugin.getGson().toJson(claimWorld).getBytes(StandardCharsets.UTF_8));
                statement.executeUpdate();
                final ResultSet resultSet = statement.getGeneratedKeys();
                if (resultSet.next()) {
                    claimWorld.updateId(resultSet.getInt(1));
                }
            }
        } catch (SQLException | JsonSyntaxException e) {
            plugin.log(Level.SEVERE, "Failed to create claim world in table", e);
        }
        return claimWorld;
    }

    @Override
    public void updateClaimWorld(@NotNull ClaimWorld claimWorld) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(format("""
                    UPDATE `%claim_data%`
                    SET `data` = ?
                    WHERE `id` = ?"""))) {
                statement.setBytes(1, plugin.getGson().toJson(claimWorld).getBytes(StandardCharsets.UTF_8));
                statement.setInt(2, claimWorld.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to update claim world in table", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

}
