-- Incremental patch script for V1.2.2 defect fixes
-- Fixes:
-- 1. Support 10-12 character material codes (e.g. 2-char bands 'GW'/'DM' with 3-digit sequence)
-- 2. Upgrade goods_transaction_dynamic_info quantity/unit_price precision to DECIMAL(18,4) and amount to DECIMAL(18,2)
-- 3. Disambiguate contract_type 'CL' display name from 'LW' ('劳务实施' -> '劳务分包')
-- 4. Add UNIQUE KEY uk_warehouse_code on warehouse_info when no duplicate codes exist
-- 5. Seed inventory.allow_negative_stock & project.tracking.low_collection_stage_sort_order configs

DROP TRIGGER IF EXISTS `trg_product_basic_info_validate_insert`;
DROP TRIGGER IF EXISTS `trg_product_basic_info_validate_update`;

DELIMITER ;;
CREATE TRIGGER `trg_product_basic_info_validate_insert`
BEFORE INSERT ON `product_basic_info`
FOR EACH ROW
BEGIN
    IF NEW.material_code IS NULL
       OR CHAR_LENGTH(TRIM(NEW.material_code)) < 10
       OR CHAR_LENGTH(TRIM(NEW.material_code)) > 12 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'material_code must be 10 to 12 characters';
    END IF;
END;;

CREATE TRIGGER `trg_product_basic_info_validate_update`
BEFORE UPDATE ON `product_basic_info`
FOR EACH ROW
BEGIN
    IF NEW.material_code IS NULL
       OR CHAR_LENGTH(TRIM(NEW.material_code)) < 10
       OR CHAR_LENGTH(TRIM(NEW.material_code)) > 12 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'material_code must be 10 to 12 characters';
    END IF;
END;;
DELIMITER ;

ALTER TABLE `goods_transaction_dynamic_info`
    MODIFY COLUMN `quantity` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '数量',
    MODIFY COLUMN `unit_price` decimal(18,4) DEFAULT '0.0000',
    MODIFY COLUMN `amount` decimal(18,2) DEFAULT NULL;

UPDATE `contract_type`
SET `type_name` = '劳务分包'
WHERE `type_code` = 'CL'
  AND `type_name` = '劳务实施';

SET @uk_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'warehouse_info'
      AND index_name = 'uk_warehouse_code'
);
SET @dup_exists := (
    SELECT COUNT(*)
    FROM (
        SELECT warehouse_code
        FROM warehouse_info
        GROUP BY warehouse_code
        HAVING COUNT(*) > 1
    ) t
);
SET @ddl := IF(
    @uk_exists = 0 AND @dup_exists = 0,
    'ALTER TABLE `warehouse_info` ADD UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`)
SELECT 'inventory.allow_negative_stock', 'false', 'Allow outbound/transfer/delete operations resulting in negative warehouse stock', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'inventory.allow_negative_stock');

INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`)
SELECT 'project.tracking.low_collection_stage_sort_order', '9', 'Minimum stage sort_order threshold to evaluate LOW_COLLECTION risk tag', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'project.tracking.low_collection_stage_sort_order');
