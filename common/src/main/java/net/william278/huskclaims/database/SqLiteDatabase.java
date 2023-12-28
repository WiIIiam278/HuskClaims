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

import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SqLiteDatabase extends SqlDatabase {

    /**
     * Path to the SQLite HuskClaimsData.db file.
     */
    private final Path databaseFile;

    /**
     * The name of the database file.
     */
    private static final String DATABASE_FILE_NAME = "HuskClaimsData.db";

    /**
     * The persistent SQLite database connection.
     */
    private Connection connection;

    public SqLiteDatabase(@NotNull HuskClaims plugin) {
        super(plugin);
        this.databaseFile = plugin.getConfigDirectory().resolve(DATABASE_FILE_NAME);
    }

    public Connection getConnection() throws SQLException {
        if (connection == null) {
            setConnection();
        } else if (connection.isClosed()) {
            setConnection();
        }
        return connection;
    }

    private void setConnection() {
        try {
            // Ensure that the database file exists
            if (databaseFile.toFile().createNewFile()) {
                plugin.log(Level.INFO, "Created the SQLite database file");
            }

            // Specify use of the JDBC SQLite driver
            Class.forName("org.sqlite.JDBC");

            // Set SQLite database properties
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setEncoding(SQLiteConfig.Encoding.UTF8);
            config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);

            // Establish the connection
            connection = DriverManager.getConnection(
                    String.format("jdbc:sqlite:%s", databaseFile.toAbsolutePath()),
                    config.toProperties()
            );
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "An exception occurred creating the database file", e);
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "An SQL exception occurred initializing the SQLite database", e);
        } catch (ClassNotFoundException e) {
            plugin.log(Level.SEVERE, "Failed to load the necessary SQLite driver", e);
        }
    }

    @Override
    public void initialize() throws RuntimeException {
        // Establish connection
        this.setConnection();

        // Backup database file
        this.backupFlatFile(databaseFile);

        // Create tables
        if (!isCreated()) {
            plugin.log(Level.INFO, "Creating SQLite database tables");
            try {
                executeScript(getConnection(), "sqlite_schema.sql");
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "Failed to create SQLite database tables");
                setLoaded(false);
                return;
            }
            setSchemaVersion(Migration.getLatestVersion());
            plugin.log(Level.INFO, "SQLite database tables created!");
            setLoaded(true);
            return;
        }

        // Perform migrations
        try {
            performMigrations(getConnection(), Type.SQLITE);
            setLoaded(true);
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to perform SQLite database migrations");
            setLoaded(false);
        }
    }


    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to close connection", e);
        }
    }

}
