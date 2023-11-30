package net.william278.huskclaims.database;

import net.william278.huskclaims.HuskClaims;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for getting the plugin {@link Database} implementation
 */
public interface DatabaseProvider {

    @NotNull
    Database getDatabase();

    void setDatabase(@NotNull Database database);

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
                throw new NotImplementedException("MySQL/MariaDB support is not yet implemented");
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
