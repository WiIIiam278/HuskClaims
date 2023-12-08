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

/**
 * Interface for getting the plugin {@link Database} implementation
 *
 * @since 1.0
 */
public interface DatabaseProvider {

    @NotNull
    Database getDatabase();

    void setDatabase(@NotNull Database database);

    void closeDatabase();

    default void loadDatabase() throws IllegalStateException {
        // Create database instance
        final Database database = createDatabase();

        // Initialize database
        database.initialize();
        if (!database.hasLoaded()) {
            throw new IllegalStateException("Failed to initialize database");
        }

        // Set database
        setDatabase(database);
    }

    @NotNull
    private Database createDatabase() {
        final Database.Type type = getPlugin().getSettings().getDatabase().getType();
        switch (type) {
            case MYSQL, MARIADB -> {
                throw new UnsupportedOperationException("MySQL/MariaDB support is not yet implemented");
            }
            case SQLITE -> {
                return new SqLiteDatabase(getPlugin());
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    @NotNull
    HuskClaims getPlugin();

}
