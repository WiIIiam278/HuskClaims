HuskClaims supports migrating data between different database types through the `/huskclaims import database` command.

## Migrating Between Database Types

You can seamlessly migrate all your claim data between MySQL, MariaDB, and SQLite databases with a simple command.

### Prerequisites

1. Ensure both database configurations are properly set in your `config.yml` file:
   - For MySQL/MariaDB migration, make sure your database credentials are correctly configured
   - For SQLite migration, the plugin will use the default file location

### Migration Process

To migrate data between database types, use the following command:

```
/huskclaims import database <source> <destination>
```

Where:
- `<source>`: Current database type (mysql/mariadb/sqlite)
- `<destination>`: Target database type (mysql/mariadb/sqlite)

### Examples

#### Migrating from MySQL to SQLite

```
/huskclaims import database mysql sqlite
```

#### Migrating from SQLite to MySQL

```
/huskclaims import database sqlite mysql
```

#### Migrating from SQLite to MariaDB

```
/huskclaims import database sqlite mariadb
```

### Troubleshooting

If you encounter issues during migration:

1. Check that both database configurations are correct in your `config.yml`
2. Ensure you have sufficient disk space for backups and new database files
3. Verify that your MySQL/MariaDB server is properly configured and accessible
4. Review the server logs for detailed error information 