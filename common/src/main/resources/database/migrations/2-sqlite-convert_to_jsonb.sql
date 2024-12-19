-- Disable FK checks and start transaction
PRAGMA foreign_keys = off;
BEGIN TRANSACTION;

-- Convert longblob columns to JSONB (less file size, better perf)
UPDATE `%user_data%` SET `preferences` = jsonb(`preferences`);
UPDATE `%user_group_data%` SET `members` = jsonb(`members`);
UPDATE `%claim_data%` SET `data` = jsonb(`data`);

-- Re-enable FK checks and commit
COMMIT TRANSACTION;
PRAGMA foreign_keys = on;