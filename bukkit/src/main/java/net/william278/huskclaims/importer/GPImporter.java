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
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        final List<CompletableFuture<List<Claim>>> claimPages = IntStream.rangeClosed(1, totalPages)
                .mapToObj(this::getClaimPage)
                .toList();
        CompletableFuture.allOf(claimPages.toArray(CompletableFuture[]::new)).join();
        final List<Claim> claims = new ArrayList<>();
        claimPages.stream()
                .map(CompletableFuture::join)
                .forEach(claims::addAll);

        users.forEach(u -> {
            int totalArea = claims.stream()
                    .filter(c -> c.owner.equals(u.name))
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
        });

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

    private CompletableFuture<List<Claim>> getClaimPage(int page) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM griefprevention_claimdata LIMIT ?, ?")) {
                statement.setInt(1, (page - 1) * CLAIMS_PER_PAGE);
                statement.setInt(2, CLAIMS_PER_PAGE);
                ResultSet resultSet = statement.executeQuery();
                List<Claim> claims = new ArrayList<>();
                while (resultSet.next()) {
                    claims.add(new Claim(
                            resultSet.getString("owner"),
                            resultSet.getString("lessercorner"),
                            resultSet.getString("greatercorner"),
                            resultSet.getString("builders"),
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
                            resultSet.getString("lastlogin"),
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

    private record Claim(String owner, String lesserCorner, String greaterCorner, String builders, String containers,
                         String accessors, String managers, boolean inheritNothing, int parentId) {

    }

    @Getter
    @AllArgsConstructor
    private static final class User {
        private final String name;
        private final String lastLogin;
        private int accruedBlocks;
    }
}
