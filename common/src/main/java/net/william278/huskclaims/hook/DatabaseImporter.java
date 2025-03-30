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

import net.kyori.adventure.text.Component;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.database.MySqlDatabase;
import net.william278.huskclaims.database.SqLiteDatabase;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

/**
 * Importer for migrating between SQLite and MySQL databases
 */
@PluginHook(
        name = "DatabaseImporter",
        register = PluginHook.Register.ON_ENABLE
)
public class DatabaseImporter extends Importer {

    // Constants for database operations
    private static final String SQLITE_DB_NAME = "HuskClaimsData.db";
    private static final String BACKUP_FOLDER = "backups";

    // Database instance references
    private Database sourceDatabase;
    private Database targetDatabase;
    
    // Data caches for migration
    private List<SavedUser> allUsers;
    private Map<UUID, Set<UserGroup>> allUserGroups;
    private Map<ServerWorld, ClaimWorld> allClaimWorlds;
    
    // Database type configuration
    private String sourceType;
    private String targetType;
    
    // Counters for migration statistics
    private int userCount = 0;
    private int groupCount = 0;
    private int claimWorldCount = 0;

    /**
     * Create a new database importer
     *
     * @param plugin The HuskClaims plugin instance
     */
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

    @Override
    protected void prepare() {
        validateDatabaseTypes();
        createBackup();
        setupDatabases();
        resetCounters();
    }

    /**
     * Validates that the database types are properly configured for migration
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validateDatabaseTypes() {
        if (sourceType == null || targetType == null) {
            throw new IllegalArgumentException("Source and target database types must be specified");
        }

        if (!isValidDatabaseType(sourceType)) {
            throw new IllegalArgumentException("Source database type must be 'mysql', 'mariadb', or 'sqlite'");
        }

        if (!isValidDatabaseType(targetType)) {
            throw new IllegalArgumentException("Target database type must be 'mysql', 'mariadb', or 'sqlite'");
        }
        
        // Consider mariadb same as mysql for comparison purposes
        String normalizedSource = normalizeDbType(sourceType);
        String normalizedTarget = normalizeDbType(targetType);
        
        if (normalizedSource.equalsIgnoreCase(normalizedTarget)) {
            throw new IllegalArgumentException("Source and target database types must be different");
        }
    }

    /**
     * Normalizes database type names (treats mariadb as mysql)
     */
    private String normalizeDbType(String dbType) {
        return dbType.equalsIgnoreCase("mariadb") ? "mysql" : dbType;
    }

    /**
     * Checks if the database type is valid
     */
    private boolean isValidDatabaseType(String dbType) {
        return dbType.equalsIgnoreCase("mysql") || 
               dbType.equalsIgnoreCase("mariadb") || 
               dbType.equalsIgnoreCase("sqlite");
    }

    /**
     * Creates a backup of the source database if it's SQLite
     */
    private void createBackup() {
        try {
            // Only backup SQLite databases
            if (!sourceType.equalsIgnoreCase("sqlite")) {
                getPlugin().log(Level.INFO, "MySQL database should be backed up manually before migration");
                return;
            }
            
            // Create backups directory if it doesn't exist
            Path backupsDir = getPlugin().getConfigDirectory().resolve(BACKUP_FOLDER);
            Files.createDirectories(backupsDir);

            // Create timestamp for the backup filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            
            // Backup SQLite database file
            Path sourceFile = getPlugin().getConfigDirectory().resolve(SQLITE_DB_NAME);
            Path targetFile = backupsDir.resolve("HuskClaimsData_" + timestamp + ".db");
            Files.copy(sourceFile, targetFile);
            getPlugin().log(Level.INFO, "Created SQLite database backup at " + targetFile);
        } catch (Throwable e) {
            getPlugin().log(Level.WARNING, "Failed to create database backup: " + e.getMessage(), e);
        }
    }

    /**
     * Sets up the source and target databases for migration
     *
     * @throws IllegalStateException if database initialization fails
     */
    private void setupDatabases() {
        // Get source database (current database)
        sourceDatabase = getPlugin().getDatabase();

        // Create target database based on target type
        String normalizedTargetType = normalizeDbType(targetType);
        if (normalizedTargetType.equalsIgnoreCase("mysql")) {
            targetDatabase = new MySqlDatabase(getPlugin());
        } else {
            targetDatabase = new SqLiteDatabase(getPlugin());
        }
        
        // Initialize target database
        targetDatabase.initialize();
        if (!targetDatabase.hasLoaded()) {
            throw new IllegalStateException("Failed to initialize target database");
        }
    }

    /**
     * Reset statistical counters for a new migration
     */
    private void resetCounters() {
        userCount = 0;
        groupCount = 0;
        claimWorldCount = 0;
    }

    @Override
    protected int importData(@NotNull ImportData importData, @NotNull CommandUser executor) {
        return switch (importData) {
            case USERS -> importUserData(executor);
            case CLAIMS -> importClaimData(executor);
            default -> 0;
        };
    }

    /**
     * Import user data including saved users and user groups
     *
     * @param executor The command user executing the import
     * @return The number of users imported
     */
    private int importUserData(CommandUser executor) {
        try {
            // Get and import all users (including inactive)
            allUsers = sourceDatabase.getInactiveUsers(-1);
            
            // Import users
            for (SavedUser user : allUsers) {
                targetDatabase.createOrUpdateUser(user);
                userCount++;
            }
            
            // Import user groups - doing it here ensures we have all users first
            allUserGroups = sourceDatabase.getAllUserGroups();
            for (Map.Entry<UUID, Set<UserGroup>> entry : allUserGroups.entrySet()) {
                for (UserGroup group : entry.getValue()) {
                    targetDatabase.addUserGroup(group);
                    groupCount++;
                }
            }
            
            return userCount;
        } catch (Throwable e) {
            getPlugin().log(Level.SEVERE, "Error migrating user data: " + e.getMessage(), e);
            executor.sendMessage(Component.text("Error migrating user data: " + e.getMessage()));
            throw e;
        }
    }

    /**
     * Import claim data including claim worlds and all associated claim information
     *
     * @param executor The command user executing the import
     * @return The number of non-user entries imported (groups + claim worlds)
     */
    private int importClaimData(CommandUser executor) {
        try {
            // Get and import claim worlds with all their data
            allClaimWorlds = sourceDatabase.getAllClaimWorlds();
            
            // Import claim worlds and their data
            for (Map.Entry<ServerWorld, ClaimWorld> entry : allClaimWorlds.entrySet()) {
                ServerWorld serverWorld = entry.getKey();
                ClaimWorld claimWorld = entry.getValue();
                
                // Log more details
                int claimCount = claimWorld.getClaims().size();
                getPlugin().log(Level.INFO, String.format(
                    "Migrating world %s with %d claims", 
                    serverWorld.toString(), 
                    claimCount
                ));
                
                // Ensure the target database has the claim world record
                targetDatabase.updateClaimWorld(claimWorld);
                claimWorldCount++;
            }
            
            return groupCount + claimWorldCount;
        } catch (Throwable e) {
            getPlugin().log(Level.SEVERE, "Error migrating claim data: " + e.getMessage(), e);
            executor.sendMessage(Component.text("Error migrating claim data: " + e.getMessage()));
            throw e;
        }
    }

    @Override
    protected void finish() {
        try {
            // Close the target database
            if (targetDatabase != null) {
                targetDatabase.close();
            }
            
            // Log summary of migration
            getPlugin().log(Level.INFO, String.format(
                "Migration summary: %d users, %d user groups, %d claim worlds with all associated data (regions, trusts, flags)",
                userCount,
                groupCount,
                claimWorldCount
            ));
            
            // Clear cached data
            clearCache();
            
            // Let the user know they need to update their config.yml to use the new database
            getPlugin().log(Level.INFO, "Database migration completed. Please update your config.yml to use the new database type.");
        } catch (Throwable e) {
            getPlugin().log(Level.SEVERE, "Error during migration cleanup: " + e.getMessage(), e);
        }
    }

    /**
     * Clear all cached data after migration completes
     */
    private void clearCache() {
        allUsers = null;
        allUserGroups = null;
        allClaimWorlds = null;
        sourceType = null;
        targetType = null;
    }
} 