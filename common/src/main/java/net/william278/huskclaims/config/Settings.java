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

package net.william278.huskclaims.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.huskclaims.database.Database;

import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Configuration
public final class Settings {

    protected static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃       HuskClaims Config      ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/huskclaims/
            ┣╸ Config Help: https://william278.net/docs/huskclaims/config-files/
            ┗╸ Documentation: https://william278.net/docs/huskclaims/
            """;

    @Comment("Locale of the default language file to use. Docs: https://william278.net/docs/huskclaims/translations")
    private String language = "en";

    @Comment("Whether to automatically check for plugin updates on startup")
    private boolean checkForUpdates = true;

    @Comment("Database settings")
    private DatabaseSettings database = new DatabaseSettings();

    @Getter
    @NoArgsConstructor
    public static class DatabaseSettings {

        @Comment("Type of database to use (SQLITE, MYSQL or MARIADB)")
        private Database.Type type = Database.Type.SQLITE;

        @Comment("Specify credentials here if you are using MYSQL or MARIADB")
        private Credentials credentials = new Credentials();

        @Comment({"MYSQL / MARIADB database Hikari connection pool properties",
                "Don't modify this unless you know what you're doing!"})
        PoolOptions poolOptions;

        @Getter
        @NoArgsConstructor
        public static class Credentials {
            private String host = "localhost";
            int port = 3306;
            String database = "huskclaims";
            String username = "root";
            String password = "pa55w0rd";
            String parameters = "?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8";
        }

        @Getter
        @NoArgsConstructor
        public static class PoolOptions {
            private int size = 12;
            private long idle = 12;
            private long lifetime = 1800000;
            private long keepAlive = 30000;
            private long timeout = 20000;
        }

        @Comment("Names of tables to use on your database. Don't modify this unless you know what you're doing!")
        Map<String, String> tableNames = Database.Table.getConfigMap();

    }

}
