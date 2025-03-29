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

import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.database.MySqlDatabase;
import net.william278.huskclaims.database.SqLiteDatabase;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.UserGroup;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

@PluginHook(
        name = "DatabaseImporter",
        register = PluginHook.Register.ON_ENABLE
)
public class DatabaseImporter extends Importer {

    private static final String SQLITE_DB_NAME = "HuskClaimsData.db";
    private static final String BACKUP_FOLDER = "backups";

    private Database sourceDatabase;
    private Database targetDatabase;
    private List<SavedUser> allUsers;
    private Map<UUID, Set<UserGroup>> allUserGroups;
    private Map<String, ClaimWorld> allClaimWorlds;
    private String sourceType;
    private String targetType;

    public DatabaseImporter(@NotNull HuskClaims plugin) {
        super(
                List.of(ImportData.USERS, ImportData.CLAIMS),
                plugin,
                Map.of(),
                Map.of()
        );
    }

    @Override
    @NotNull
    public String getName() {
        return "database";
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Import claim data between different database types (MySQL and SQLite)";
    }

    @Override
    protected void prepare() {
        if (sourceType == null || targetType == null) {
            throw new IllegalArgumentException("Source and target database types must be specified");
        }

        if (!sourceType.equalsIgnoreCase("mysql") && !sourceType.equalsIgnoreCase("sqlite")) {
            throw new IllegalArgumentException("Source database type must be 'mysql' or 'sqlite'");
        }

        if (!targetType.equalsIgnoreCase("mysql") && !targetType.equalsIgnoreCase("sqlite")) {
            throw new IllegalArgumentException("Target database type must be 'mysql' or 'sqlite'");
        }

        if (sourceType.equalsIgnoreCase(targetType)) {
            throw new IllegalArgumentException("Source and target database types must be different");
        }

        // Create backup of the current database
        createBackup();

        // Get source database (current database)
        sourceDatabase = getPlugin().getDatabase();

        // Create target database based on target type
        if (targetType.equalsIgnoreCase("mysql")) {
            targetDatabase = new MySqlDatabase(getPlugin());
        } else {
            targetDatabase = new SqLiteDatabase(getPlugin());
        }
        
        // Initialize target database
        targetDatabase.initialize();
        if (!targetDatabase.hasLoaded()) {
            throw new IllegalStateException("Failed to initialize target database");
        }

        // Cache all data from source database for migration
        allUsers = sourceDatabase.getInactiveUsers(-1); // Get all users
        allUserGroups = sourceDatabase.getAllUserGroups();
        allClaimWorlds = new HashMap<>();
        
        sourceDatabase.getAllClaimWorlds().forEach((world, claimWorld) -> {
            allClaimWorlds.put(world.getKey(), claimWorld);
        });
    }

    private void createBackup() {
        try {
            // Create backups directory if it doesn't exist
            Path backupsDir = getPlugin().getConfigDirectory().resolve(BACKUP_FOLDER);
            Files.createDirectories(backupsDir);

            // Create timestamp for the backup filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            
            if (sourceType.equalsIgnoreCase("sqlite")) {
                // Backup SQLite database file
                Path sourceFile = getPlugin().getConfigDirectory().resolve(SQLITE_DB_NAME);
                Path targetFile = backupsDir.resolve("HuskClaimsData_" + timestamp + ".db");
                Files.copy(sourceFile, targetFile);
            } else {
                // For MySQL, we rely on the server admin to have backups
                // We could implement a MySQL dump here, but it's outside the scope of this implementation
            }
        } catch (Exception e) {
            getPlugin().log(Level.WARNING, "Failed to create database backup: " + e.getMessage(), e);
        }
    }

    @Override
    protected int importData(@NotNull ImportData importData, @NotNull CommandUser executor) {
        int count = 0;
        
        switch (importData) {
            case USERS -> {
                // Import users
                for (SavedUser user : allUsers) {
                    targetDatabase.createOrUpdateUser(user);
                    count++;
                }
            }
            case CLAIMS -> {
                // Import user groups
                for (Map.Entry<UUID, Set<UserGroup>> entry : allUserGroups.entrySet()) {
                    for (UserGroup group : entry.getValue()) {
                        targetDatabase.addUserGroup(group);
                        count++;
                    }
                }
                
                // Import claim worlds
                for (ClaimWorld claimWorld : allClaimWorlds.values()) {
                    targetDatabase.updateClaimWorld(claimWorld);
                    count++;
                }
            }
        }
        
        return count;
    }

    @Override
    protected void finish() {
        // Close the target database
        targetDatabase.close();
        
        // Clear cached data
        allUsers = null;
        allUserGroups = null;
        allClaimWorlds = null;
        
        // Reset the source and target types
        sourceType = null;
        targetType = null;
        
        // Let the user know they need to update their config.yml to use the new database
        getPlugin().log(Level.INFO, "Database migration completed. Please update your config.yml to use the new database type.");
    }
    
    /**
     * Set source and target database types for migration
     * 
     * @param source The source database type (mysql/sqlite)
     * @param target The target database type (mysql/sqlite)
     */
    public void setDatabaseTypes(@NotNull String source, @NotNull String target) {
        this.sourceType = source.toLowerCase(Locale.ENGLISH);
        this.targetType = target.toLowerCase(Locale.ENGLISH);
    }
} 