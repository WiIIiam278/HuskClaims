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

package net.william278.huskclaims.hook;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.user.Preferences;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.IntStream;

public class GriefPreventionImporter extends Importer {

    // Parameters for the GP database
    private static final int USERS_PER_PAGE = 500;
    private static final int CLAIMS_PER_PAGE = 500;

    private HikariDataSource dataSource;
    private ExecutorService pool;
    private List<GriefPreventionUser> users;

    public GriefPreventionImporter(@NotNull BukkitHuskClaims plugin) {
        super(
                "GriefPrevention",
                List.of(ImportData.USERS, ImportData.CLAIMS),
                plugin,
                Map.of(
                        "uri", true,
                        "username", true,
                        "password", false
                ),
                Map.of(
                        "password", ""
                )
        );
    }

    @Override
    public void prepare() throws IllegalArgumentException {
        final String uri = configParameters.get("uri");
        final String username = configParameters.get("username");
        final String password = configParameters.get("password");
        if (uri == null || username == null) {
            throw new IllegalArgumentException("Missing required config parameters");
        }

        // Setup GriefPrevention database connection
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(uri);
        config.setUsername(username);
        if (password != null) {
            config.setPassword(password);
        }
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(30000);
        config.setPoolName(String.format("HuskClaims-%s-Importer", getName()));
        config.setReadOnly(true);

        this.dataSource = new HikariDataSource(config);
        this.pool = Executors.newFixedThreadPool(20);
    }

    @Override
    public void finish() {
        if (dataSource != null) {
            dataSource.close();
        }
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Override
    protected int importData(@NotNull ImportData importData) {
        return switch (importData) {
            case USERS -> importUsers();
            case CLAIMS -> importClaims();
        };
    }


    private int importUsers() {
        final int totalUsers = getTotalUsers();
        final int totalPages = (int) Math.ceil(totalUsers / (double) USERS_PER_PAGE);
        final List<CompletableFuture<List<GriefPreventionUser>>> userPages = IntStream.rangeClosed(1, totalPages)
                .mapToObj(this::getUserPage)
                .toList();
        CompletableFuture.allOf(userPages.toArray(CompletableFuture[]::new)).join();
        users = new ArrayList<>();
        userPages.stream()
                .map(CompletableFuture::join)
                .forEach(users::addAll);
        return users.size();
    }

    private int importClaims() {
        if (users == null) {
            throw new IllegalStateException("Users must be imported before claims");
        }
        final int totalClaims = getTotalClaims();
        final int totalPages = (int) Math.ceil(totalClaims / (double) CLAIMS_PER_PAGE);

        final List<CompletableFuture<List<GriefPreventionClaim>>> claimPages = IntStream.rangeClosed(1, totalPages)
                .mapToObj(this::getClaimPage)
                .toList();
        CompletableFuture.allOf(claimPages.toArray(CompletableFuture[]::new)).join();
        final List<GriefPreventionClaim> claims = claimPages.stream()
                .map(CompletableFuture::join)
                .toList().stream()
                .flatMap(Collection::stream)
                .toList();

        final List<GriefPreventionClaim> claimToSave = Lists.newArrayList();
        final List<CompletableFuture<Void>> saveFutures = Lists.newArrayList();
        users.forEach(user -> {
            final UUID uuid = UUID.fromString(user.name);
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            final String name = offlinePlayer.hasPlayedBefore() ? offlinePlayer.getName() : user.name.substring(0, 8);
            plugin.log(Level.INFO, String.format("Importing claims for %s (%s)", name, user.name));
            final int totalArea = claims.stream()
                    .filter(c -> c.owner.equals(user.name))
                    .peek(claimToSave::add)
                    .mapToInt(c -> {
                        String[] lesserCorner = c.lesserCorner.split(";");
                        String[] greaterCorner = c.greaterCorner.split(";");
                        int x1 = Integer.parseInt(lesserCorner[1]);
                        int z1 = Integer.parseInt(lesserCorner[3]);
                        int x2 = Integer.parseInt(greaterCorner[1]);
                        int z2 = Integer.parseInt(greaterCorner[3]);
                        int x = Math.abs(x1 - x2) + 1;
                        int z = Math.abs(z1 - z2) + 1;
                        return x * z;
                    })
                    .sum();
            user.claimBlocks -= totalArea;
            user.name = name == null ? user.name : name;
            saveFutures.add(CompletableFuture.runAsync(
                    () -> plugin.getDatabase().createOrUpdateUser(
                            user.uuid, user.name, user.claimBlocks, user.getLastLogin(), Preferences.DEFAULTS
                    ), pool
            ));
        });

        // Wait for all users to be saved
        CompletableFuture.allOf(saveFutures.toArray(CompletableFuture[]::new)).join();

        // Adding admin claims & child claims
        final Map<Claim, GriefPreventionClaim> claimMap = Maps.newHashMap();
        claims.stream().filter(c -> c.owner.isEmpty()).forEach(claimToSave::add);
        claims.stream().filter(gpc -> !claimToSave.contains(gpc)).forEach(gpc -> plugin
                .log(Level.WARNING, "Skipped claim %s (missing owner: %s)".formatted(gpc.lesserCorner, gpc.owner)));

        claimToSave.forEach(gpc -> {
            final Map<UUID, String> trusted = new HashMap<>();
            gpc.builders.forEach(uuid -> trusted.put(uuid, "build"));
            gpc.containers.forEach(uuid -> trusted.put(uuid, "container"));
            gpc.accessors.forEach(uuid -> trusted.put(uuid, "access"));
            gpc.managers.forEach(uuid -> trusted.put(uuid, "manage"));

            final Claim claim = gpc.toClaim(trusted, plugin);
//            if (publicTrust.get()) {
//                final Optional<TrustTag> publicTag = plugin.getPublicTrustTag();
//                if (publicTag.isEmpty()) {
//                    plugin.log(Level.WARNING, "Skipped claim %s (missing public trust tag)".formatted(c.lesserCorner));
//                    return;
//                }
//                final Optional<TrustLevel> publicTrustLevel = plugin.getTrustLevel("build");
//                if (publicTrustLevel.isEmpty()) {
//                    plugin.log(Level.WARNING, "Skipped claim at %s on missing world %s".formatted(gpc.lesserCorner, world));
//                    return;
//                }
//                claim.setTagTrustLevel(publicTag.get(), publicTrustLevel.get());
//            }
            claimMap.put(claim, gpc);
        });

        final Map<Claim, GriefPreventionClaim> children = claimMap.entrySet().stream()
                .filter(e -> e.getValue().parentId != -1)
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);

        final Set<ClaimWorld> claimWorlds = new HashSet<>();
        final AtomicInteger amount = new AtomicInteger();
        claimMap.forEach((hcc, gpc) -> {
            final String world = gpc.lesserCorner.split(";")[0];
            final Optional<ClaimWorld> optionalWorld = plugin.getClaimWorld(world);
            if (optionalWorld.isEmpty()) {
                plugin.log(Level.WARNING, "Skipped claim at %s on missing world %s".formatted(gpc.lesserCorner, world));
                return;
            }

            final ClaimWorld claimWorld = optionalWorld.get();
            claimWorlds.add(claimWorld);
            amount.getAndIncrement();
            claimWorld.getClaims().add(hcc);

            // If the claim has a parent, adds it to the parent's children list
            if (gpc.parentId != -1) {
                return;
            }

            final List<Claim> childClaims = children.entrySet().stream()
                    .filter(e -> e.getValue().parentId == gpc.id)
                    .map(Map.Entry::getKey).toList();
            hcc.getChildren().addAll(childClaims);
        });

        final List<CompletableFuture<Void>> claimWorldFutures = new ArrayList<>();
        claimWorlds.forEach(claimWorld -> claimWorldFutures.add(
                CompletableFuture.runAsync(() -> plugin.getDatabase().updateClaimWorld(claimWorld), pool)
        ));
        CompletableFuture.allOf(claimWorldFutures.toArray(CompletableFuture[]::new)).join();
        return amount.get();
    }

    private int getTotalUsers() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM griefprevention_playerdata""")) {

            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (Throwable e) {
            plugin.log(Level.WARNING, "Exception querying total user count from DB database", e);
        }
        return 0;
    }

    private int getTotalClaims() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM griefprevention_claimdata""")) {

            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (Throwable e) {
            plugin.log(Level.WARNING, "Exception querying total claim count from DB database", e);
        }
        return 0;
    }

    private CompletableFuture<List<GriefPreventionClaim>> getClaimPage(int page) {
        final List<GriefPreventionClaim> claims = Lists.newArrayList();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT *
                         FROM griefprevention_claimdata
                         LIMIT ?, ?;""")) {
                statement.setInt(1, (page - 1) * CLAIMS_PER_PAGE);
                statement.setInt(2, CLAIMS_PER_PAGE);

                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    claims.add(new GriefPreventionClaim(
                            resultSet.getInt("id"),
                            resultSet.getString("owner"),
                            resultSet.getString("lessercorner"),
                            resultSet.getString("greatercorner"),
                            getUUIDs(resultSet.getString("builders")),
                            getUUIDs(resultSet.getString("containers")),
                            getUUIDs(resultSet.getString("accessors")),
                            getUUIDs(resultSet.getString("managers")),
                            resultSet.getBoolean("inheritnothing"),
                            resultSet.getInt("parentid")
                    ));
                }
                return claims;
            } catch (Throwable e) {
                plugin.log(Level.WARNING, "Exception getting claim page #%s from GP database".formatted(page), e);
            }
            return claims;
        }, pool);
    }

    private List<UUID> getUUIDs(String uuids) {
        return Arrays.stream(uuids.split(";"))
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
    }

    private CompletableFuture<List<GriefPreventionUser>> getUserPage(int page) {
        final List<GriefPreventionUser> users = Lists.newArrayList();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT `name`, `lastlogin`, (`accruedblocks` + `bonusblocks`) AS `claimblocks`
                         FROM griefprevention_playerdata
                         LIMIT ?, ?;""")) {
                statement.setInt(1, (page - 1) * USERS_PER_PAGE);
                statement.setInt(2, USERS_PER_PAGE);

                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    users.add(new GriefPreventionUser(
                            resultSet.getString("name"),
                            resultSet.getTimestamp("lastlogin"),
                            resultSet.getInt("claimblocks")
                    ));
                }
            } catch (Throwable e) {
                plugin.log(Level.WARNING, "Exception getting user page #%s from GP database".formatted(page), e);
            }
            return users;
        }, pool);
    }

    private record GriefPreventionClaim(int id, @NotNull String owner,
                                        @NotNull String lesserCorner, @NotNull String greaterCorner,
                                        @NotNull List<UUID> builders, @NotNull List<UUID> containers,
                                        @NotNull List<UUID> accessors, @NotNull List<UUID> managers,
                                        boolean inheritNothing, int parentId) {

        @NotNull
        private Claim toClaim(@NotNull Map<UUID, String> trusted, @NotNull HuskClaims plugin) {
            final String[] lesserCorner = this.lesserCorner.split(";");
            final String[] greaterCorner = this.greaterCorner.split(";");
            final Region region = Region.from(Region.Point.at(Integer.parseInt(lesserCorner[1]), Integer.parseInt(lesserCorner[3])),
                    Region.Point.at(Integer.parseInt(greaterCorner[1]), Integer.parseInt(greaterCorner[3])));
            final UUID ownerUuid = !owner.isEmpty() ? UUID.fromString(owner) : null;
            final Claim claim = Claim.create(ownerUuid, region, plugin);
            claim.getTrustedUsers().putAll(trusted);
            claim.setInheritParent(!inheritNothing);
            return claim;
        }

    }

    @Getter
    @AllArgsConstructor
    private static final class GriefPreventionUser {
        private final static Timestamp NEVER = new Timestamp(100);
        private UUID uuid;
        private String name;
        private final Timestamp lastLogin;
        private int claimBlocks;

        public GriefPreventionUser(@NotNull String name, @NotNull Timestamp lastLogin, int claimBlocks) {
            this.name = name;
            this.lastLogin = lastLogin;
            this.claimBlocks = claimBlocks;
            this.uuid = UUID.fromString(name);
        }

        @NotNull
        public Timestamp getLastLogin() {
            return lastLogin.before(NEVER) ? Timestamp.valueOf(LocalDateTime.now()) : lastLogin;
        }
    }
}
