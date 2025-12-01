# Add tax_balance column to user_data table
# Note: If column already exists, this will fail but migration system handles it gracefully
ALTER TABLE `%user_data%` ADD COLUMN `tax_balance` double NOT NULL DEFAULT 0.0;
