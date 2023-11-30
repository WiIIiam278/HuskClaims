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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Database {

    protected HuskClaims plugin;

    /**
     * Loads SQL table creation schema statements from a resource file as a string array.
     *
     * @param schemaFileName database script resource file to load from
     * @return Array of string-formatted table creation schema statements
     * @throws IOException if the resource could not be read
     */
    protected final String[] getSchemaStatements(@NotNull String schemaFileName) throws IOException {
        return formatStatementTables(new String(Objects.requireNonNull(
                plugin.getResource(schemaFileName)).readAllBytes(), StandardCharsets.UTF_8)
        ).split(";");
    }

    /**
     * Format all table name placeholder strings in an SQL statement.
     *
     * @param sql the SQL statement with unformatted table name placeholders
     * @return the formatted statement, with table placeholders replaced with the correct names
     */
    protected final String formatStatementTables(@NotNull String sql) {
        final Map<String, String> tableNames = plugin.getSettings().getDatabase().getTableNames();
        return sql.replaceAll("%meta_data%", tableNames.get(Table.META_DATA))
                .replaceAll("%user_data%", tableNames.get(Table.META_DATA))
                .replaceAll("%claim_data%", tableNames.get(Table.META_DATA));
    }

    /**
     * Initialize the database and ensure tables are present; create tables if they do not exist.
     */
    public abstract void initialize() throws IllegalStateException;

    /**
     * Supported database types
     */
    public enum Type {
        MYSQL("MySQL"),
        MARIADB("MariaDB"),
        SQLITE("SQLite");

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
     * Represents the names of tables in the database.
     */
    public enum Table {
        META_DATA("huskclaims_schema"),
        USER_DATA("huskclaims_users"),
        CLAIM_DATA("huskclaims_claim_worlds");

        private final String defaultName;

        Table(@NotNull String defaultName) {
            this.defaultName = defaultName;
        }

        @NotNull
        public String getDefaultName() {
            return defaultName;
        }

        @NotNull
        public static Map<String, String> getConfigMap() {
            return Arrays.stream(values()).collect(Collectors.toMap(
                    table -> table.name().toLowerCase(Locale.ENGLISH),
                    Table::getDefaultName
            ));
        }
    }

}
