package net.william278.huskclaims.database;

import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class SqLiteDatabase extends Database {

    /**
     * Path to the SQLite HuskHomesData.db file.
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

    @NotNull
    private Connection getConnection() throws SQLException {
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

            // Specify use of the JDBC SQLite driver for legacy compatibility
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
            plugin.log(Level.SEVERE, "An exception occurred creating the SQLite database file", e);
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "An SQL exception occurred initializing the SQLite database", e);
        } catch (ClassNotFoundException e) {
            plugin.log(Level.SEVERE, "Failed to load the necessary SQLite driver", e);
        }
    }

    @Override
    public void initialize() throws IllegalStateException {
        // Set up the connection
        setConnection();

        // Prepare database schema; make tables if they don't exist
        try {
            // Load the database schema CREATE statements from schema file
            final String[] databaseSchema = getSchemaStatements("database/sqlite_schema.sql");
            try (Statement statement = getConnection().createStatement()) {
                for (String tableCreationStatement : databaseSchema) {
                    statement.execute(tableCreationStatement);
                }
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize the SQLite database", e);
        }
    }

}
