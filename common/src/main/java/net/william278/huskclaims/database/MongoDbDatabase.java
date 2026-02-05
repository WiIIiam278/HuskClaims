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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import java.util.regex.Pattern;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Level;

public class MongoDbDatabase extends Database {

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private MongoCollection<Document> metadataCollection;
    private MongoCollection<Document> userCollection;
    private MongoCollection<Document> userGroupCollection;
    private MongoCollection<Document> claimCollection;
    private MongoCollection<Document> counterCollection;

    public MongoDbDatabase(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    private void setConnection() {
        final Settings.DatabaseSettings databaseSettings = plugin.getSettings().getDatabase();
        final Settings.DatabaseSettings.DatabaseCredentials credentials = databaseSettings.getCredentials();

        final String connectionString = buildConnectionString(credentials);
        mongoClient = MongoClients.create(connectionString);
        mongoDatabase = mongoClient.getDatabase(credentials.getDatabase());

        final String metadataTable = databaseSettings.getTableName(Table.META_DATA);
        final String userTable = databaseSettings.getTableName(Table.USER_DATA);
        final String userGroupTable = databaseSettings.getTableName(Table.USER_GROUP_DATA);
        final String claimTable = databaseSettings.getTableName(Table.CLAIM_DATA);

        metadataCollection = mongoDatabase.getCollection(metadataTable);
        userCollection = mongoDatabase.getCollection(userTable);
        userGroupCollection = mongoDatabase.getCollection(userGroupTable);
        claimCollection = mongoDatabase.getCollection(claimTable);
        counterCollection = mongoDatabase.getCollection("huskclaims_counters");
    }

    private String buildConnectionString(@NotNull Settings.DatabaseSettings.DatabaseCredentials credentials) {
        final StringBuilder connectionString = new StringBuilder("mongodb://");
        if (!credentials.getUsername().isEmpty() && !credentials.getPassword().isEmpty()) {
            connectionString.append(credentials.getUsername())
                    .append(":")
                    .append(credentials.getPassword())
                    .append("@");
        }
        connectionString.append(credentials.getHost())
                .append(":")
                .append(credentials.getPort());
        if (!credentials.getParameters().isEmpty()) {
            final String params = credentials.getParameters();
            connectionString.append("/");
            if (params.startsWith("?")) {
                connectionString.append(params);
            } else if (params.startsWith("&")) {
                connectionString.append("?").append(params.substring(1));
            } else {
                connectionString.append("?").append(params);
            }
        }
        return connectionString.toString();
    }

    @Override
    protected void executeScript(@NotNull Connection connection, @NotNull String name) throws SQLException {
        throw new UnsupportedOperationException("MongoDB does not use SQL scripts");
    }

    @Override
    public void initialize() throws RuntimeException {
        this.setConnection();

        if (!isCreated()) {
            plugin.log(Level.INFO, "Creating MongoDB collections");
            createCollections();
            setSchemaVersion(Migration.getLatestVersion());
            plugin.log(Level.INFO, "MongoDB collections created!");
            setLoaded(true);
            return;
        }

        try {
            performMigrations(null, Type.MONGODB);
            setLoaded(true);
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to perform MongoDB database migrations", e);
            setLoaded(false);
        }
    }

    private void createCollections() {
        try {
            userCollection.createIndex(Indexes.ascending("uuid"), new com.mongodb.client.model.IndexOptions().unique(true));
            userCollection.createIndex(Indexes.ascending("username"));
            userGroupCollection.createIndex(Indexes.ascending("uuid"));
            userGroupCollection.createIndex(Indexes.ascending("name"));
            claimCollection.createIndex(Indexes.ascending("server_name"));
            claimCollection.createIndex(Indexes.ascending("world_uuid"));
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to create some MongoDB indexes (they may already exist)", e);
        }
    }

    @Override
    public boolean isCreated() {
        try {
            final Document document = userCollection.find().limit(1).first();
            return document != null || metadataCollection.countDocuments() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getSchemaVersion() {
        try {
            final Document metadata = metadataCollection.find().first();
            if (metadata != null && metadata.containsKey("schema_version")) {
                return metadata.getInteger("schema_version");
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "The database schema version could not be fetched; migrations will be carried out.");
        }
        return -1;
    }

    @Override
    public void setSchemaVersion(int version) {
        final Document metadata = new Document("schema_version", version);
        if (getSchemaVersion() == -1) {
            metadataCollection.insertOne(metadata);
        } else {
            metadataCollection.updateOne(
                    new Document(),
                    new Document("$set", metadata)
            );
        }
    }

    @Override
    protected void performMongoMigration(@NotNull Migration migration) {
        switch (migration) {
            case ADD_METADATA_TABLE -> {
                if (metadataCollection.countDocuments() == 0) {
                    metadataCollection.insertOne(new Document("schema_version", 0));
                }
            }
            case REMOVE_HOURS_PLAYED_COLUMN -> {
                userCollection.updateMany(
                        new Document(),
                        new Document("$unset", new Document("hours_played", ""))
                );
            }
            case ADD_SPENT_CLAIM_BLOCKS_COLUMN -> {
                userCollection.updateMany(
                        Filters.exists("spent_claim_blocks", false),
                        Updates.set("spent_claim_blocks", 0L)
                );
            }
            default -> {
                throw new UnsupportedOperationException("MongoDB migration " + migration.name()
                        + " is not implemented in performMongoMigration");
            }
        }
    }

    @Override
    public Optional<SavedUser> getUser(@NotNull UUID uuid) {
        try {
            final Document document = userCollection.find(Filters.eq("uuid", uuid.toString())).first();
            if (document != null) {
                return Optional.of(documentToSavedUser(document));
            }
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to fetch user data from collection by UUID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SavedUser> getUser(@NotNull String username) {
        try {
            final Document document = userCollection.find(
                    Filters.regex("username", Pattern.compile("^" + Pattern.quote(username) + "$", Pattern.CASE_INSENSITIVE))).first();
            if (document != null) {
                return Optional.of(documentToSavedUser(document));
            }
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to fetch user data from collection by username", e);
        }
        return Optional.empty();
    }

    @Override
    public List<SavedUser> getInactiveUsers(long daysInactive) {
        final List<SavedUser> inactiveUsers = Lists.newArrayList();
        try {
            final long cutoffTime = System.currentTimeMillis() - (daysInactive * 24L * 60L * 60L * 1000L);
            final Date cutoffDate = new Date(cutoffTime);
            userCollection.find(Filters.lt("last_login", cutoffDate)).forEach(document -> {
                inactiveUsers.add(documentToSavedUser(document));
            });
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to fetch list of inactive users", e);
            inactiveUsers.clear();
        }
        return inactiveUsers;
    }

    @Override
    public void createUser(@NotNull SavedUser saved) {
        try {
            final Document document = savedUserToDocument(saved);
            userCollection.insertOne(document);
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to create user in collection", e);
        }
    }

    @Override
    public void updateUser(@NotNull SavedUser user) {
        try {
            final Document update = new Document("$set", new Document()
                    .append("claim_blocks", user.getClaimBlocks())
                    .append("preferences", plugin.getGson().toJson(user.getPreferences()))
                    .append("spent_claim_blocks", user.getSpentClaimBlocks()));
            userCollection.updateOne(
                    Filters.eq("uuid", user.getUser().getUuid().toString()),
                    update
            );
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to update Saved User data in collection", e);
        }
    }

    @Override
    public void createOrUpdateUser(@NotNull SavedUser data) {
        try {
            final Document document = new Document()
                    .append("uuid", data.getUser().getUuid().toString())
                    .append("username", data.getUser().getName())
                    .append("last_login", Date.from(OffsetDateTime.now().toInstant()))
                    .append("claim_blocks", data.getClaimBlocks())
                    .append("preferences", plugin.getGson().toJson(data.getPreferences()))
                    .append("spent_claim_blocks", data.getSpentClaimBlocks());
            userCollection.replaceOne(
                    Filters.eq("uuid", data.getUser().getUuid().toString()),
                    document,
                    new com.mongodb.client.model.ReplaceOptions().upsert(true)
            );
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to create or update user in collection", e);
        }
    }

    @NotNull
    @Override
    public Set<UserGroup> getUserGroups(@NotNull UUID uuid) {
        try {
            final Set<UserGroup> userGroups = Sets.newHashSet();
            userGroupCollection.find(Filters.eq("uuid", uuid.toString())).forEach(document -> {
                userGroups.add(documentToUserGroup(document));
            });
            return userGroups;
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to fetch user groups from collection", e);
        }
        return Sets.newHashSet();
    }

    @NotNull
    @Override
    public Map<UUID, Set<UserGroup>> getAllUserGroups() {
        try {
            final Map<UUID, Set<UserGroup>> userGroups = Maps.newHashMap();
            userGroupCollection.find().forEach(document -> {
                final UserGroup userGroup = documentToUserGroup(document);
                userGroups.compute(userGroup.groupOwner(), (key, value) -> {
                    if (value == null) {
                        value = Sets.newHashSet();
                    }
                    value.add(userGroup);
                    return value;
                });
            });
            return userGroups;
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to fetch user groups from collection", e);
        }
        return Maps.newHashMap();
    }

    @Override
    public void addUserGroup(@NotNull UserGroup group) {
        try {
            final Document document = new Document()
                    .append("uuid", group.groupOwner().toString())
                    .append("name", group.name())
                    .append("members", plugin.getGson().toJson(group.members()));
            userGroupCollection.insertOne(document);
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to create user group in collection", e);
        }
    }

    @Override
    public void updateUserGroup(@NotNull UUID owner, @NotNull String name, @NotNull UserGroup newGroup) {
        try {
            final Document update = new Document("$set", new Document()
                    .append("name", newGroup.name())
                    .append("members", plugin.getGson().toJson(newGroup.members())));
            userGroupCollection.updateOne(
                    Filters.and(
                            Filters.eq("uuid", owner.toString()),
                            Filters.eq("name", name)
                    ),
                    update
            );
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to update user group in collection", e);
        }
    }

    @Override
    public void deleteUserGroup(@NotNull UserGroup group) {
        try {
            userGroupCollection.deleteOne(
                    Filters.and(
                            Filters.eq("uuid", group.groupOwner().toString()),
                            Filters.eq("name", group.name())
                    )
            );
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to remove user group from collection", e);
        }
    }

    @NotNull
    @Override
    public Map<World, ClaimWorld> getClaimWorlds(@NotNull String server) throws IllegalStateException {
        final Map<World, ClaimWorld> worlds = Maps.newHashMap();
        try {
            claimCollection.find(Filters.eq("server_name", server)).forEach(document -> {
                final World world = World.of(
                        document.getString("world_name"),
                        UUID.fromString(document.getString("world_uuid")),
                        document.getString("world_environment")
                );
                final int id = document.getInteger("id");
                final String dataJson = document.getString("data");
                final ClaimWorld claimWorld = plugin.getClaimWorldFromJson(id, dataJson);
                claimWorld.updateId(id);
                if (!plugin.getSettings().getClaims().isWorldUnclaimable(world)) {
                    worlds.put(world, claimWorld);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to fetch claim world map for %s", server), e);
        }
        return worlds;
    }

    @NotNull
    @Override
    public Map<ServerWorld, ClaimWorld> getAllClaimWorlds() throws IllegalStateException {
        final Map<ServerWorld, ClaimWorld> worlds = Maps.newHashMap();
        try {
            claimCollection.find().forEach(document -> {
                final World world = World.of(
                        document.getString("world_name"),
                        UUID.fromString(document.getString("world_uuid")),
                        document.getString("world_environment")
                );
                final int id = document.getInteger("id");
                final String dataJson = document.getString("data");
                final ClaimWorld claimWorld = plugin.getClaimWorldFromJson(id, dataJson);
                claimWorld.updateId(id);
                worlds.put(new ServerWorld(document.getString("server_name"), world), claimWorld);
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch map of all claim worlds", e);
        }
        return worlds;
    }

    @Override
    @NotNull
    public ClaimWorld createClaimWorld(@NotNull World world) {
        final ClaimWorld claimWorld = ClaimWorld.create(plugin);
        try {
            final int id = getNextClaimWorldId();
            final Document document = new Document()
                    .append("id", id)
                    .append("server_name", plugin.getServerName())
                    .append("world_uuid", world.getUuid().toString())
                    .append("world_name", world.getName())
                    .append("world_environment", world.getEnvironment())
                    .append("data", plugin.getGson().toJson(claimWorld));
            claimCollection.insertOne(document);
            claimWorld.updateId(id);
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to create claim world in collection", e);
        }
        return claimWorld;
    }

    @Override
    public void updateClaimWorld(@NotNull ClaimWorld claimWorld) {
        try {
            final Document update = new Document("$set", new Document()
                    .append("data", plugin.getGson().toJson(claimWorld)));
            claimCollection.updateOne(
                    Filters.eq("id", claimWorld.getId()),
                    update
            );
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to update claim world in collection", e);
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    private SavedUser documentToSavedUser(@NotNull Document document) {
        final UUID uuid = UUID.fromString(document.getString("uuid"));
        final String username = document.getString("username");
        final String preferencesJson = document.getString("preferences");
        final Date lastLoginDate = document.getDate("last_login");
        final OffsetDateTime lastLogin = lastLoginDate != null
                ? OffsetDateTime.ofInstant(lastLoginDate.toInstant(), ZoneOffset.systemDefault())
                : OffsetDateTime.now();
        final long claimBlocks = document.getLong("claim_blocks");
        final long spentClaimBlocks = document.containsKey("spent_claim_blocks")
                ? document.getLong("spent_claim_blocks")
                : 0L;

        return new SavedUser(
                User.of(uuid, username),
                plugin.getPreferencesFromJson(preferencesJson),
                lastLogin,
                claimBlocks,
                spentClaimBlocks
        );
    }

    private Document savedUserToDocument(@NotNull SavedUser user) {
        return new Document()
                .append("uuid", user.getUser().getUuid().toString())
                .append("username", user.getUser().getName())
                .append("last_login", Date.from(user.getLastLogin().toInstant()))
                .append("claim_blocks", user.getClaimBlocks())
                .append("preferences", plugin.getGson().toJson(user.getPreferences()))
                .append("spent_claim_blocks", user.getSpentClaimBlocks());
    }

    private UserGroup documentToUserGroup(@NotNull Document document) {
        final UUID uuid = UUID.fromString(document.getString("uuid"));
        final String name = document.getString("name");
        final String membersJson = document.getString("members");
        return new UserGroup(
                uuid,
                name,
                plugin.getUserListFromJson(membersJson)
        );
    }

    private int getNextClaimWorldId() {
        final Document counter = counterCollection.findOneAndUpdate(
                Filters.eq("_id", "claim_world_id"),
                Updates.inc("seq", 1),
                new com.mongodb.client.model.FindOneAndUpdateOptions()
                        .upsert(true)
                        .returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
        );
        if (counter == null || !counter.containsKey("seq")) {
            return 1;
        }
        return counter.getInteger("seq");
    }

}

