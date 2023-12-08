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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.group.UserGroup;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.Preferences;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Database {

    protected HuskClaims plugin;
    private boolean loaded;

    protected Database(@NotNull HuskClaims plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the schema statements from the schema file
     *
     * @return the {@link #format formatted} schema statements
     */
    @NotNull
    protected final String[] getScript(@NotNull String name) {
        name = (name.startsWith("database/") ? "" : "database/") + name + (name.endsWith(".sql") ? "" : ".sql");
        try (InputStream schemaStream = Objects.requireNonNull(plugin.getResource(name))) {
            final String schema = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
            return format(schema).split(";");
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to load database schema", e);
        }
        return new String[0];
    }

    protected abstract void executeScript(@NotNull Connection connection, @NotNull String name) throws SQLException;

    /**
     * Format a string for use in an SQL query
     *
     * @param statement The SQL statement to format
     * @return The formatted SQL statement
     */
    @NotNull
    protected final String format(@NotNull String statement) {
        final Pattern pattern = Pattern.compile("%(\\w+)%");
        final Matcher matcher = pattern.matcher(statement);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final Table table = Table.match(matcher.group(1));
            matcher.appendReplacement(sb, plugin.getSettings().getDatabase().getTableName(table));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Backup a flat file database
     *
     * @param file the file to back up
     */
    protected final void backupFlatFile(@NotNull Path file) {
        if (!file.toFile().exists()) {
            return;
        }

        final Path backup = file.getParent().resolve(String.format("%s.bak", file.getFileName().toString()));
        try {
            final File backupFile = backup.toFile();
            if (!backupFile.exists() || backupFile.delete()) {
                Files.copy(file, backup);
            }
        } catch (IOException e) {
            plugin.log(Level.WARNING, "Failed to backup flat file database", e);
        }
    }

    /**
     * Initialize the database connection
     *
     * @throws IllegalStateException if the database initialization fails
     */
    public abstract void initialize() throws IllegalStateException;

    /**
     * Check if the database has been created
     *
     * @return {@code true} if the database has been created; {@code false} otherwise
     */
    public abstract boolean isCreated();

    /**
     * Perform database migrations
     *
     * @param connection the database connection
     * @throws SQLException if an SQL error occurs during migration
     */
    protected final void performMigrations(@NotNull Connection connection, @NotNull Type type) throws SQLException {
        final int currentVersion = getSchemaVersion();
        final int latestVersion = Migration.getLatestVersion();
        if (currentVersion < latestVersion) {
            plugin.log(Level.INFO, "Performing database migrations (Target version: v" + latestVersion + ")");
            for (Migration migration : Migration.getOrderedMigrations()) {
                if (!migration.isSupported(type)) {
                    continue;
                }
                if (migration.getVersion() > currentVersion) {
                    try {
                        plugin.log(Level.INFO, "Performing database migration: " + migration.getMigrationName()
                                + " (v" + migration.getVersion() + ")");
                        final String scriptName = "migrations/" + migration.getVersion() + "-" + type.name().toLowerCase() +
                                "-" + migration.getMigrationName() + ".sql";
                        executeScript(connection, scriptName);
                    } catch (SQLException e) {
                        plugin.log(Level.WARNING, "Migration " + migration.getMigrationName()
                                + " (v" + migration.getVersion() + ") failed; skipping", e);
                    }
                }
            }
            setSchemaVersion(latestVersion);
            plugin.log(Level.INFO, "Completed database migration (Target version: v" + latestVersion + ")");
        }
    }

    /**
     * Get the database schema version
     *
     * @return the database schema version
     */
    public abstract int getSchemaVersion();

    /**
     * Set the database schema version
     *
     * @param version the database schema version
     */
    public abstract void setSchemaVersion(int version);

    /**
     * Get a user by their UUID
     *
     * @param uuid The UUID of the user
     * @return The user, if they exist
     */
    public abstract Optional<SavedUser> getUser(@NotNull UUID uuid);

    /**
     * Get a user by their name
     *
     * @param username The name of the user
     * @return The user, if they exist
     */
    public abstract Optional<SavedUser> getUser(@NotNull String username);

    /**
     * Get a list of {@link SavedUser}s who have not logged in for a given number of days
     *
     * @param daysInactive The number of days a user has not logged in for
     * @return A list of {@link SavedUser}s who have not logged in for a given number of days
     */
    public abstract List<SavedUser> getInactiveUsers(long daysInactive);

    /**
     * Add a user to the database
     *
     * @param user        The user to add
     * @param preferences The user's preferences
     */
    public abstract void createUser(@NotNull User user, long claimBlocks, @NotNull Preferences preferences);

    /**
     * Update a user's name and preferences in the database
     *
     * @param user        The user to update
     * @param lastLogin   The user's last login time
     * @param claimBlocks The user's claim blocks
     * @param preferences The user's preferences to update
     */
    public abstract void updateUser(@NotNull User user, @NotNull OffsetDateTime lastLogin,
                                    long claimBlocks, @NotNull Preferences preferences);

    /**
     * Update a user's name and preferences in the database, marking their last login time as now
     *
     * @param user        The user to update
     * @param preferences The user's preferences to update
     */
    public final void updateUser(@NotNull User user, long claimBlocks, @NotNull Preferences preferences) {
        this.updateUser(user, OffsetDateTime.now(), claimBlocks, preferences);
    }

    /**
     * Update a user's preferences in the database
     *
     * @param user        The user to update
     * @param preferences The user's preferences to update
     */
    public abstract void updateUserPreferences(@NotNull User user, @NotNull Preferences preferences);

    /**
     * Update a user's claim blocks in the database
     *
     * @param user        The user to update
     * @param claimBlocks The user's claim blocks to update
     */
    public abstract void updateUserClaimBlocks(@NotNull User user, long claimBlocks);
    /**
     * Get a list of a user's {@link UserGroup user groups}.
     *
     * @param uuid The UUID of the user
     * @return A list of the user's {@link UserGroup user groups}
     */
    @NotNull
    public abstract ConcurrentLinkedQueue<UserGroup> getUserGroups(@NotNull UUID uuid);

    /**
     * Get a map of all {@link UserGroup user groups} for all users.
     *
     * @return A map of everyone's {@link UserGroup user groups}.
     */
    @NotNull
    public abstract ConcurrentLinkedQueue<UserGroup> getAllUserGroups();

    /**
     * Add a {@link UserGroup} to the database
     *
     * @param group the group to add
     */
    public abstract void addUserGroup(@NotNull UserGroup group);

    /**
     * Edit a {@link UserGroup} in the database
     *
     * @param owner    The owner of the group
     * @param name     The name of the group
     * @param newGroup The new group data
     */
    public abstract void updateUserGroup(@NotNull UUID owner, @NotNull String name, @NotNull UserGroup newGroup);

    /**
     * Delete a {@link UserGroup} from the database
     *
     * @param group the group to delete
     */
    public abstract void deleteUserGroup(@NotNull UserGroup group);

    /**
     * Get a list of all claim worlds on a server
     *
     * @return A list of all claim worlds on a server, excluding unclaimable worlds.
     * @throws IllegalStateException if the plugin fails to fetch claim world data
     */
    @NotNull
    public abstract Map<World, ClaimWorld> getClaimWorlds(@NotNull String server) throws IllegalStateException;

    /**
     * Get a list of all claim worlds
     *
     * @return A map of world-server entries to each claim world
     * @throws IllegalStateException if the plugin fails to fetch claim world data
     */
    @NotNull
    public abstract Map<ServerWorld, ClaimWorld> getAllClaimWorlds() throws IllegalStateException;

    /**
     * Create a new claim world and add it to the database
     *
     * @param world The world to create the claim world for
     * @return The created claim world
     */
    @NotNull
    public abstract ClaimWorld createClaimWorld(@NotNull World world);

    /**
     * Update a claim world in the database
     *
     * @param claimWorld The claim world to update
     */
    public abstract void updateClaimWorld(@NotNull ClaimWorld claimWorld);

    /**
     * Close the database connection
     */
    public abstract void close();

    /**
     * Check if the database has been loaded
     *
     * @return {@code true} if the database has loaded successfully; {@code false} if it failed to initialize
     */
    public boolean hasLoaded() {
        return loaded;
    }

    /**
     * Set if the database has loaded
     *
     * @param loaded whether the database has loaded successfully
     */
    protected void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * Identifies types of databases
     */
    public enum Type {
        MYSQL("MySQL"),
        MARIADB("MariaDB"),
        SQLITE("SQLite");
        @NotNull
        private final String displayName;

        Type(@NotNull String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Represents the names of tables in the database
     */
    public enum Table {
        META_DATA("huskclaims_metadata"),
        USER_DATA("huskclaims_users"),
        USER_GROUP_DATA("huskclaims_user_groups"),
        CLAIM_DATA("huskclaims_claim_worlds");
        @NotNull
        private final String defaultName;

        Table(@NotNull String defaultName) {
            this.defaultName = defaultName;
        }

        @NotNull
        public static Database.Table match(@NotNull String placeholder) throws IllegalArgumentException {
            return Table.valueOf(placeholder.toUpperCase());
        }

        @NotNull
        public String getDefaultName() {
            return defaultName;
        }
    }

    /**
     * Represents database migrations that need to be run
     */
    public enum Migration {
        ADD_METADATA_TABLE(
                0, "add_metadata_table",
                Type.MYSQL, Type.MARIADB, Type.SQLITE
        );

        private final int version;
        private final String migrationName;
        private final Type[] supportedTypes;

        Migration(int version, @NotNull String migrationName, @NotNull Type... supportedTypes) {
            this.version = version;
            this.migrationName = migrationName;
            this.supportedTypes = supportedTypes;
        }

        private int getVersion() {
            return version;
        }

        private String getMigrationName() {
            return migrationName;
        }

        private boolean isSupported(@NotNull Type type) {
            return Arrays.stream(supportedTypes).anyMatch(supportedType -> supportedType == type);
        }

        @NotNull
        public static List<Migration> getOrderedMigrations() {
            return Arrays.stream(Migration.values())
                    .sorted(Comparator.comparingInt(Migration::getVersion))
                    .collect(Collectors.toList());
        }

        public static int getLatestVersion() {
            return getOrderedMigrations().get(getOrderedMigrations().size() - 1).getVersion();
        }

    }

}
