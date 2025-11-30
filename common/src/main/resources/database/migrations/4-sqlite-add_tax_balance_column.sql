-- Add tax_balance column to user_data table (if it doesn't exist)
-- SQLite doesn't support IF NOT EXISTS for ALTER TABLE ADD COLUMN, but the migration system handles errors gracefully
ALTER TABLE `%user_data%` ADD COLUMN `tax_balance` real NOT NULL DEFAULT 0.0;
