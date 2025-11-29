# Add tax_balance column to user_data table
ALTER TABLE `%user_data%` ADD COLUMN `tax_balance` double NOT NULL DEFAULT 0.0;
