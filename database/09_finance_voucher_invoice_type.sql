-- Add VAT invoice classification to finance vouchers.
SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @finance_voucher_invoice_type_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'finance_voucher'
    AND COLUMN_NAME = 'invoice_type'
);
SET @ddl = IF(
  @finance_voucher_invoice_type_column_exists = 0,
  'ALTER TABLE `finance_voucher` ADD COLUMN `invoice_type` varchar(32) DEFAULT NULL AFTER `invoice_status`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
