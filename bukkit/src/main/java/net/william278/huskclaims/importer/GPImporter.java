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

package net.william278.huskclaims.importer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class GPImporter extends Importer {

    private static final int USERS_PER_PAGE = 500;
    private static final int CLAIMS_PER_PAGE = 500;

    private final BukkitHuskClaims plugin;
    private HikariDataSource dataSource;
    private ExecutorService pool;
    private List<User> users;

    public GPImporter(@NotNull BukkitHuskClaims plugin) {
        super("GriefPreventionImporter", List.of(ImportData.USERS, ImportData.CLAIMS), plugin);
        this.plugin = plugin;
    }

    @Override
    public void prepare(@NotNull Map<String, String> args) throws IllegalArgumentException {
        final String uri = args.get("uri");
        final String username = args.get("username");
        final String password = args.get("password");

        if (uri == null || username == null || password == null) {
            throw new IllegalArgumentException("Missing required arguments");
        }

        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(uri);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(30000);
        config.setPoolName("HuskClaims-GPImporter");
        config.setReadOnly(true);

        this.dataSource = new HikariDataSource(config);
        this.pool = Executors.newCachedThreadPool();
    }

    @Override
    public void unload() {
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
        final List<CompletableFuture<List<User>>> userPages = IntStream.rangeClosed(1, totalPages)
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
        final List<CompletableFuture<List<GPClaim>>> claimPages = IntStream.rangeClosed(1, totalPages)
                .mapToObj(this::getClaimPage)
                .toList();
        CompletableFuture.allOf(claimPages.toArray(CompletableFuture[]::new)).join();
        final List<GPClaim> claims = new ArrayList<>();
        claimPages.stream()
                .map(CompletableFuture::join)
                .forEach(claims::addAll);

        final List<GPClaim> claimToSave = new ArrayList<>();
        final List<User> correctUsers = new ArrayList<>();

        users.forEach(u -> {
            final UUID uuid = UUID.fromString(u.name);
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (!offlinePlayer.hasPlayedBefore()) {
//                plugin.getLogger().warning("Unable to import claim data for user " + u.name + " as they have never played on this server");
                return;
            }
            correctUsers.add(u);

            System.out.println("Importing claims for " + offlinePlayer.getName());

            final String name = offlinePlayer.getName();
            final int totalArea = claims.stream()
                    .filter(c -> c.owner.equals(u.name))
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
            u.accruedBlocks -= totalArea;
            u.name = name == null ? u.name : name;
            plugin.getDatabase().createOrUpdateUser(u.uuid, u.name, u.accruedBlocks, u.lastLogin);
        });

        final Map<Claim, GPClaim> claimMap = new HashMap<>();

        claimToSave.forEach(c -> {
            final Map<UUID, String> trusted = c.builders.stream()
                    .map(u -> correctUsers.stream()
                            .filter(user -> user.uuid.equals(u))
                            .findFirst()
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(HashMap::new, (m, v) -> m.put(v.uuid, v.name), HashMap::putAll);
            final Claim claim = c.toClaim(trusted, plugin);
            claimMap.put(claim, c);
        });

        final Map<Claim, GPClaim> childs = claimMap.entrySet().stream()
                .filter(e -> e.getValue().parentId != -1)
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);

        final Set<ClaimWorld> claimWorlds = new HashSet<>();
        claimMap.forEach((claim, c) -> {
            final String world = c.lesserCorner.split(";")[0];
            Optional<ClaimWorld> optionalClaimWorld = Optional.ofNullable(plugin.getClaimWorlds().getOrDefault(world, null));
            if (optionalClaimWorld.isEmpty()) {
                plugin.getLogger().warning("Unable to import claim data for claim " + c.lesserCorner + " as the world " + world + " does not exist");
                return;
            }
            final ClaimWorld claimWorld = optionalClaimWorld.get();
            claimWorlds.add(claimWorld);

            if (c.parentId != -1) {
                claimWorld.getClaims().add(claim);
                return;
            }

            final List<Claim> childClaims = childs.entrySet().stream()
                    .filter(e -> e.getValue().parentId == c.id)
                    .map(Map.Entry::getKey)
                    .toList();
            claim.getChildren().addAll(childClaims);
            claimWorld.getClaims().add(claim);
        });

        claimWorlds.forEach(claimWorld -> plugin.getDatabase().updateClaimWorld(claimWorld));

        return claims.size();
    }

    private int getTotalUsers() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM griefprevention_playerdata")) {
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("An error occurred whilst getting the total number of users in the GriefPrevention database: " + e.getMessage());
        }
        return 0;
    }

    private int getTotalClaims() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM griefprevention_claimdata")) {
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("An error occurred whilst getting the total number of claims in the GriefPrevention database: " + e.getMessage());
        }
        return 0;
    }

    private CompletableFuture<List<GPClaim>> getClaimPage(int page) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM griefprevention_claimdata LIMIT ?, ?")) {
                statement.setInt(1, (page - 1) * CLAIMS_PER_PAGE);
                statement.setInt(2, CLAIMS_PER_PAGE);
                ResultSet resultSet = statement.executeQuery();
                List<GPClaim> claims = new ArrayList<>();
                while (resultSet.next()) {
                    claims.add(new GPClaim(
                            resultSet.getInt("id"),
                            resultSet.getString("owner"),
                            resultSet.getString("lessercorner"),
                            resultSet.getString("greatercorner"),
                            Arrays.stream(resultSet.getString("builders").split(";"))
                                    .map(UUID::fromString)
                                    .toList(),
                            resultSet.getString("containers"),
                            resultSet.getString("accessors"),
                            resultSet.getString("managers"),
                            resultSet.getBoolean("inheritnothing"),
                            resultSet.getInt("parentid")
                    ));
                }
                return claims;
            } catch (Throwable e) {
                plugin.getLogger().warning("An error occurred whilst getting the claim data from the GriefPrevention database: " + e.getMessage());
            }
            return List.of();
        }, pool);
    }

    private CompletableFuture<List<User>> getUserPage(int page) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM griefprevention_playerdata LIMIT ?, ?")) {
                statement.setInt(1, (page - 1) * USERS_PER_PAGE);
                statement.setInt(2, USERS_PER_PAGE);
                ResultSet resultSet = statement.executeQuery();
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(new User(
                            resultSet.getString("name"),
                            resultSet.getDate("lastlogin"),
                            resultSet.getInt("accruedblocks") + resultSet.getInt("bonusblocks")
                    ));
                }
                return users;
            } catch (Throwable e) {
                plugin.getLogger().warning("An error occurred whilst getting the user data from the GriefPrevention database: " + e.getMessage());
            }
            return List.of();
        }, pool);
    }

    private record GPClaim(int id, String owner, String lesserCorner, String greaterCorner, List<UUID> builders, String containers,
                           String accessors, String managers, boolean inheritNothing, int parentId) {


        private Claim toClaim(@NotNull Map<UUID, String> trusted, @NotNull BukkitHuskClaims plugin
                              ) {
            String[] lesserCorner = this.lesserCorner.split(";");
            String[] greaterCorner = this.greaterCorner.split(";");
            final Region region = Region.from(Region.Point.at(Integer.parseInt(lesserCorner[1]), Integer.parseInt(lesserCorner[3])),
                    Region.Point.at(Integer.parseInt(greaterCorner[1]), Integer.parseInt(greaterCorner[3])));

            Claim claim =  Claim.create(UUID.fromString(owner), region, plugin);
            claim.getTrustedUsers().putAll(trusted);
            claim.setInheritParent(!inheritNothing);

            return claim;
        }

    }

    @Getter
    @AllArgsConstructor
    private static final class User {
        private UUID uuid;
        private String name;
        private final Date lastLogin;
        private int accruedBlocks;

        public User(String name, Date lastLogin, int accruedBlocks) {
            this.name = name;
            this.lastLogin = lastLogin;
            this.accruedBlocks = accruedBlocks;
            this.uuid = UUID.fromString(name);
        }
    }
}
