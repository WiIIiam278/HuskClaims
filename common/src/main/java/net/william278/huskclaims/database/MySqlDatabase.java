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

import com.zaxxer.hikari.HikariDataSource;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public class MySqlDatabase extends SqlDatabase {

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

    /**
     * Fetch the auto-closeable connection from the hikariDataSource.
     *
     * @return The {@link Connection} to the MySQL database
     * @throws SQLException if the connection fails for some reason
     */
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void initialize() throws RuntimeException {
        /// Initialize the Hikari pooled connection
        
        final Settings.DatabaseSettings databaseSettings = plugin.getSettings().getDatabase();
        final Settings.DatabaseSettings.DatabaseCredentials credentials = databaseSettings.getCredentials();
        final Settings.DatabaseSettings.PoolOptions poolOptions = databaseSettings.getPoolOptions();

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

        // Create tables
        if (!isCreated()) {
            plugin.log(Level.INFO, "Creating MySql database tables");
            try {
                executeScript(getConnection(), "mysql_schema.sql");
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
            performMigrations(getConnection(), databaseSettings.getType());
            setLoaded(true);
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to perform MySql database migrations", e);
            setLoaded(false);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

}
