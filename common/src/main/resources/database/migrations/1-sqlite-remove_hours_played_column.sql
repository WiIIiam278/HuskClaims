-- Disable FK checks and start transaction
PRAGMA foreign_keys = off;
BEGIN TRANSACTION;

-- Drop the hours played column
ALTER TABLE `%user_data%` DROP COLUMN `hours_played`;

-- Re-enable FK checks and commit
COMMIT TRANSACTION;
PRAGMA foreign_keys = on;