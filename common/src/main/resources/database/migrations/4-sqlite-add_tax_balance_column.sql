-- Add tax_balance column to user_data table
ALTER TABLE `%user_data%` ADD COLUMN `tax_balance` real NOT NULL DEFAULT 0.0;
