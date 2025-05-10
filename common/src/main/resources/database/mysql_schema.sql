# Set the storage engine
SET DEFAULT_STORAGE_ENGINE = INNODB;

# Enable foreign key constraints
SET FOREIGN_KEY_CHECKS = 1;

# Create the metadata table if it does not exist
CREATE TABLE IF NOT EXISTS `%meta_data%`
(
    `schema_version` integer NOT NULL PRIMARY KEY
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;

# Create the users table if it does not exist
CREATE TABLE IF NOT EXISTS `%user_data%`
(
    `uuid`               char(36)      NOT NULL UNIQUE,
    `username`           varchar(16)   NOT NULL,
    `last_login`         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `claim_blocks`       bigint        NOT NULL DEFAULT 0,
    `preferences`        longblob      NOT NULL,
    `spent_claim_blocks` bigint        NOT NULL DEFAULT 0,

    PRIMARY KEY (`uuid`)
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;

# Create the user groups table if it does not exist
CREATE TABLE IF NOT EXISTS `%user_group_data%`
(
    `id`      integer     NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `uuid`    char(36)    NOT NULL,
    `name`    varchar(32) NOT NULL,
    `members` longblob    NOT NULL,

    FOREIGN KEY (`uuid`) REFERENCES `%user_data%` (`uuid`)
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;

# Create the claim worlds table if it does not exist
CREATE TABLE IF NOT EXISTS `%claim_data%`
(
    `id`                integer      NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `server_name`       varchar(255) NOT NULL,
    `world_uuid`        char(36)     NOT NULL,
    `world_name`        varchar(128) NOT NULL,
    `world_environment` varchar(32)  NOT NULL,
    `data`              longblob     NOT NULL
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;