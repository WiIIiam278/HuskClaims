-- Add the spent claim blocks column
ALTER TABLE `%user_data%` ADD COLUMN `spent_claim_blocks` bigint DEFAULT 0;