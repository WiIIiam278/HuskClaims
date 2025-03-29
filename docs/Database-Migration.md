# Database Migration

HuskClaims supports migrating data between different database types through the `/huskclaims import database` command.

## Migrating Between MySQL and SQLite

You can seamlessly migrate all your claim data between MySQL and SQLite databases with a simple command.

### Prerequisites

1. Ensure both database configurations are properly set in your `config.yml` file:
   - For MySQL migration, make sure your MySQL credentials are correctly configured
   - For SQLite migration, the plugin will use the default file location

### Migration Process

To migrate data between database types, use the following command:

```
/huskclaims import database <source> <destination>
```

Where:
- `<source>`: Current database type (mysql/sqlite)
- `<destination>`: Target database type (mysql/sqlite)

### Examples

#### Migrating from MySQL to SQLite

```
/huskclaims import database mysql sqlite
```

#### Migrating from SQLite to MySQL

```
/huskclaims import database sqlite mysql
```

### Troubleshooting

If you encounter issues during migration:

1. Check that both database configurations are correct in your `config.yml`
2. Ensure you have sufficient disk space for backups and new database files
3. Verify that your MySQL server is properly configured and accessible
4. Review the server logs for detailed error information 