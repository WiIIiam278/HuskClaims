# Add tax_balance column to user_data table (if it doesn't exist)
ALTER TABLE `%user_data%` ADD COLUMN IF NOT EXISTS `tax_balance` double NOT NULL DEFAULT 0.0;
