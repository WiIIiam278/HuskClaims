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
import com.google.common.collect.Sets;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.IntStream;

@PluginHook(
        name = "GriefPrevention",
        register = PluginHook.Register.ON_ENABLE
)
public class BukkitGriefPreventionImporter extends Importer {

    // Parameters for the GP database
    private static final int USERS_PER_PAGE = 500;
    private static final int CLAIMS_PER_PAGE = 500;
    private static final String GP_PUBLIC_STRING = "public";

    private HikariDataSource dataSource;
    private ExecutorService pool;
    private List<GriefPreventionUser> users;

    public BukkitGriefPreventionImporter(@NotNull BukkitHuskClaims plugin) {
        super(
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
        this.configParameters = Maps.newHashMap(defaultParameters);
        this.users = Lists.newArrayList();
    }

    @Override
    protected int importData(@NotNull ImportData importData, @NotNull CommandUser executor) {
        return switch (importData) {
            case USERS -> importUsers();
            case CLAIMS -> importClaims(executor);
        };
    }

    private int importUsers() {
        final int totalUsers = getTotalUsers();
        final int totalPages = (int) Math.ceil(totalUsers / (double) USERS_PER_PAGE);
        final List<CompletableFuture<List<GriefPreventionUser>>> userPages = IntStream.rangeClosed(1, totalPages)
                .mapToObj(this::getUserPage).toList();
        CompletableFuture.allOf(userPages.toArray(CompletableFuture[]::new)).join();
        users = Lists.newArrayList();
        userPages.stream().map(CompletableFuture::join).forEach(users::addAll);
        return users.size();
    }

    private int importClaims(@NotNull CommandUser executor) {
        if (users == null) {
            throw new IllegalStateException("Users must be imported before claims");
        }
        final int totalClaims = getTotalClaims();
        final int totalPages = (int) Math.ceil(totalClaims / (double) CLAIMS_PER_PAGE);

        final List<CompletableFuture<List<GriefPreventionClaim>>> claimPages = IntStream.rangeClosed(1, totalPages)
                .mapToObj(this::getClaimPage).toList();
        CompletableFuture.allOf(claimPages.toArray(CompletableFuture[]::new)).join();
        final List<GriefPreventionClaim> claims = claimPages.stream()
                .map(CompletableFuture::join)
                .toList().stream()
                .flatMap(Collection::stream)
                .toList();

        log(executor, Level.INFO, "Adjusting claim block balances for %s users...".formatted(users.size()));
        final List<CompletableFuture<Void>> saveFutures = Lists.newArrayList();
        final AtomicInteger amount = new AtomicInteger();
        users.forEach(user -> {
            // Update claim blocks
            final int totalArea = claims.stream()
                    .filter(c -> c.owner.equals(user.uuid.toString()))
                    .mapToInt(c -> {
                        final String[] lesserCorner = c.lesserCorner.split(";");
                        final String[] greaterCorner = c.greaterCorner.split(";");
                        int x1 = Integer.parseInt(lesserCorner[1]);
                        int z1 = Integer.parseInt(lesserCorner[3]);
                        int x2 = Integer.parseInt(greaterCorner[1]);
                        int z2 = Integer.parseInt(greaterCorner[3]);
                        int x = Math.abs(x1 - x2) + 1;
                        int z = Math.abs(z1 - z2) + 1;
                        return x * z;
                    })
                    .sum();
            user.claimBlocks = Math.max(0, user.claimBlocks - totalArea);

            saveFutures.add(CompletableFuture.runAsync(
                    () -> {
                        plugin.getDatabase().getUser(user.uuid.toString()).ifPresent(existing -> user.claimBlocks += (int) existing.getClaimBlocks());
                        plugin.getDatabase().createOrUpdateUser(user.toSavedUser());
                        plugin.invalidateClaimListCache(user.uuid);
                        if (amount.incrementAndGet() % USERS_PER_PAGE == 0) {
                            log(executor, Level.INFO, "Adjusted %s users...".formatted(amount.get()));
                        }
                    }, pool
            ));
        });

        // Wait for all users to be saved
        CompletableFuture.allOf(saveFutures.toArray(CompletableFuture[]::new)).join();
        plugin.invalidateAdminClaimListCache();

        // Adding admin claims & child claims
        log(executor, Level.INFO, "Converting %s claims...".formatted(claims.size()));
        final Map<Claim, GriefPreventionClaim> allClaims = Maps.newHashMap();
        claims.forEach(gpc -> allClaims.put(gpc.toClaim(this), gpc));

        // Convert child claims and save to claim worlds
        log(executor, Level.INFO, "Saving %s claims...".formatted(amount.getAndSet(0)));
        final Set<ClaimWorld> claimWorlds = Sets.newHashSet();
        allClaims.forEach((hcc, gpc) -> {
            final String world = gpc.lesserCorner.split(";")[0];

            // Get the claim world
            final Optional<ClaimWorld> optionalWorld = plugin.getClaimWorld(world);
            if (optionalWorld.isEmpty()) {
                plugin.log(Level.WARNING, "Skipped claim at %s on missing world %s".formatted(gpc.lesserCorner, world));
                return;
            }
            final ClaimWorld claimWorld = optionalWorld.get();

            // Add the claim world to the list of worlds, then cache the claim owner/trustees
            claimWorlds.add(claimWorld);
            hcc.getOwner().flatMap(owner -> users.stream().filter(gpu -> gpu.uuid.equals(owner)).findFirst())
                    .ifPresent(user -> claimWorld.cacheUser(user.toUser()));
            hcc.getTrustedUsers().keySet().forEach(u -> users.stream().filter(gpu -> gpu.uuid.equals(u))
                    .findFirst().ifPresent(user -> claimWorld.cacheUser(user.toUser())));

            // Add the claim to either its parent or the claim world
            if (gpc.isChildClaim()) {
                allClaims.entrySet().stream()
                        .filter(entry -> entry.getValue().id == gpc.parentId)
                        .map(Map.Entry::getKey).findFirst()
                        .ifPresent(parent -> {
                            parent.getOwner().ifPresent(hcc::setOwner);
                            parent.getChildren().add(hcc);
                        });
            } else {
                claimWorld.addClaim(hcc);
            }

            if (amount.incrementAndGet() % CLAIMS_PER_PAGE == 0) {
                log(executor, Level.INFO, "Saved %s claims...".formatted(amount.get()));
            }
        });

        // Save claim worlds
        plugin.clearAllMapMarkers();
        log(executor, Level.INFO, "Saving %s claim worlds...".formatted(claimWorlds.size()));
        final List<CompletableFuture<Void>> claimWorldFutures = Lists.newArrayList();
        claimWorlds.forEach(claimWorld -> claimWorldFutures.add(
                CompletableFuture.runAsync(() -> plugin.getDatabase().updateClaimWorld(claimWorld), pool)
        ));
        CompletableFuture.allOf(claimWorldFutures.toArray(CompletableFuture[]::new)).join();
        plugin.markAllClaims();
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
                            parseTrusted(resultSet.getString("builders")),
                            parseTrusted(resultSet.getString("containers")),
                            parseTrusted(resultSet.getString("accessors")),
                            parseTrusted(resultSet.getString("managers")),
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

    @NotNull
    private List<String> parseTrusted(@NotNull String names) {
        return Arrays.stream(names.split(";"))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void setTrust(@NotNull String id, @NotNull TrustLevel level, @NotNull Map<Trustable, TrustLevel> map) {
        if (id.equals(GP_PUBLIC_STRING)) {
            plugin.getPublicTrustTag().ifPresent(tag -> map.put(tag, level));
            return;
        }

        try {
            final UUID uuid = UUID.fromString(id);
            users.stream().filter(gpu -> gpu.uuid.equals(uuid)).findFirst()
                    .ifPresent(user -> map.put(user.toUser(), level));
        } catch (IllegalArgumentException ignored) {
            plugin.log(Level.WARNING, "Invalid UUID in GP database: " + id);
        }
    }

    @NotNull
    private Map<Trustable, TrustLevel> convertTrustees(@NotNull GriefPreventionClaim gpc, @NotNull HuskClaims plugin) {
        final Map<Trustable, TrustLevel> trusted = Maps.newHashMap();
        plugin.getTrustLevel("build").ifPresent(l -> gpc.builders().forEach(id -> setTrust(id, l, trusted)));
        plugin.getTrustLevel("container").ifPresent(l -> gpc.containers().forEach(id -> setTrust(id, l, trusted)));
        plugin.getTrustLevel("access").ifPresent(l -> gpc.accessors().forEach(id -> setTrust(id, l, trusted)));
        plugin.getTrustLevel("manage").ifPresent(l -> gpc.managers().forEach(id -> setTrust(id, l, trusted)));
        return trusted;
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
                            UUID.fromString(resultSet.getString("name")),
                            resultSet.getTimestamp("lastlogin"),
                            resultSet.getInt("claimblocks"),
                            (BukkitHuskClaims) plugin
                    ));
                }
            } catch (Throwable e) {
                plugin.log(Level.WARNING, "Exception getting user page #%s from GP database".formatted(page), e);
            }
            return users;
        }, pool);
    }

    private record GriefPreventionClaim(int id,
                                        @NotNull String owner,
                                        @NotNull String lesserCorner, @NotNull String greaterCorner,
                                        @NotNull List<String> builders, @NotNull List<String> containers,
                                        @NotNull List<String> accessors, @NotNull List<String> managers,
                                        boolean inheritNothing, int parentId) {

        @NotNull
        private Claim toClaim(@NotNull BukkitGriefPreventionImporter importer) {
            final String[] lesserCorner = this.lesserCorner.split(";");
            final String[] greaterCorner = this.greaterCorner.split(";");
            final Region region = Region.from(
                    Region.Point.at(Integer.parseInt(lesserCorner[1]), Integer.parseInt(lesserCorner[3])),
                    Region.Point.at(Integer.parseInt(greaterCorner[1]), Integer.parseInt(greaterCorner[3]))
            );

            // Determine claim members
            final UUID ownerUuid = !owner.isEmpty() ? UUID.fromString(owner) : null;
            final Claim claim = Claim.create(ownerUuid, region, importer.getPlugin());
            importer.convertTrustees(this, importer.getPlugin()).forEach(claim::setTrustLevel);
            claim.setInheritParent(!inheritNothing);
            return claim;
        }

        private boolean isChildClaim() {
            return parentId != -1;
        }

    }

    @AllArgsConstructor
    private static final class GriefPreventionUser {
        private final static Timestamp NEVER = new Timestamp(100);
        private UUID uuid;
        private String name;
        private final Timestamp lastLogin;
        private int claimBlocks;

        private GriefPreventionUser(@NotNull UUID uuid, @NotNull Timestamp lastLogin, int claimBlocks,
                                    @NotNull BukkitHuskClaims plugin) {
            this.uuid = uuid;
            this.lastLogin = lastLogin;
            this.claimBlocks = claimBlocks;

            final OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
            if (player.hasPlayedBefore()) {
                this.name = player.getName();
            } else {
                this.name = uuid.toString().substring(0, 8);
            }
        }

        @NotNull
        private OffsetDateTime getLastLogin() {
            return lastLogin.before(NEVER)
                    ? OffsetDateTime.now()
                    : lastLogin.toLocalDateTime().atOffset(OffsetDateTime.now().getOffset());
        }

        @NotNull
        private User toUser() {
            return User.of(uuid, name);
        }

        @NotNull
        private SavedUser toSavedUser() {
            return SavedUser.createImported(toUser(), getLastLogin(), claimBlocks);
        }

    }
}
