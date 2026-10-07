-- Apply to the selected database with voucher writes stopped, before deploying both frontend and backend.
-- Existing vouchers switch to subject IDs. Legacy code columns remain for migration comparison.
-- Re-running preserves IDs and renamed subject names.
SET NAMES utf8mb4;
SET @finance_subject_original_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = IF(
  FIND_IN_SET('STRICT_TRANS_TABLES', @@SESSION.sql_mode) > 0 OR FIND_IN_SET('STRICT_ALL_TABLES', @@SESSION.sql_mode) > 0,
  @@SESSION.sql_mode, CONCAT_WS(',', NULLIF(@@SESSION.sql_mode, ''), 'STRICT_TRANS_TABLES')
);

CREATE TABLE IF NOT EXISTS `finance_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_level` tinyint NOT NULL,
  `subject_code` varchar(64) NOT NULL,
  `subject_name` varchar(64) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_finance_subject_level_code` (`subject_level`, `subject_code`),
  UNIQUE KEY `uk_finance_subject_level_name` (`subject_level`, `subject_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `finance_subject` (`subject_level`, `subject_code`, `subject_name`, `sort_order`) VALUES
  (1, 'NON_PROJECT', '非项目', 1),
  (1, 'PROJECT', '项目', 2),
  (2, 'EQUIPMENT_PURCHASE', '设备采购', 1),
  (2, 'AUXILIARY_MATERIAL_PURCHASE', '辅材采购', 2),
  (2, 'CONSTRUCTION_FEE', '施工费', 3),
  (2, 'SALES_EXPENSE', '销售费用', 4),
  (2, 'MISCELLANEOUS', '杂项支出', 5),
  (2, 'AMORTIZATION', '摊销', 6),
  (2, 'PROJECT_RECEIPT', '项目回款', 7),
  (2, 'WAREHOUSE_TRANSFER_IN', '仓库调入', 8),
  (2, 'PROJECT_TRANSFER_OUT', '项目调出', 9),
  (2, 'SALARY', '人员工资', 10),
  (2, 'TRAVEL', '差旅', 11),
  (2, 'ENTERTAINMENT', '招待', 12),
  (2, 'CONFERENCE', '会议', 13),
  (2, 'VEHICLE', '车辆', 14),
  (2, 'OTHER', '其他', 15)
ON DUPLICATE KEY UPDATE `subject_code` = `finance_subject`.`subject_code`;

SET @finance_subject_ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_voucher' AND COLUMN_NAME = 'level_1_subject_id') = 0,
  'ALTER TABLE finance_voucher ADD COLUMN level_1_subject_id bigint DEFAULT NULL AFTER level_1_subject', 'SELECT 1');
PREPARE finance_subject_stmt FROM @finance_subject_ddl;
EXECUTE finance_subject_stmt;
DEALLOCATE PREPARE finance_subject_stmt;

SET @finance_subject_ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_voucher' AND COLUMN_NAME = 'level_2_subject_id') = 0,
  'ALTER TABLE finance_voucher ADD COLUMN level_2_subject_id bigint DEFAULT NULL AFTER level_2_subject', 'SELECT 1');
PREPARE finance_subject_stmt FROM @finance_subject_ddl;
EXECUTE finance_subject_stmt;
DEALLOCATE PREPARE finance_subject_stmt;

-- Preserve any historical codes outside the original fixed options as renameable subjects.
INSERT IGNORE INTO finance_subject (subject_level, subject_code, subject_name, sort_order)
SELECT DISTINCT 1, v.level_1_subject, v.level_1_subject, 1000
FROM finance_voucher v
WHERE v.level_1_subject IS NOT NULL AND v.level_1_subject <> ''
  AND NOT EXISTS (
    SELECT 1 FROM finance_subject s WHERE s.subject_level = 1
      AND s.subject_code = CONVERT(v.level_1_subject USING utf8mb4) COLLATE utf8mb4_general_ci
  );
INSERT IGNORE INTO finance_subject (subject_level, subject_code, subject_name, sort_order)
SELECT DISTINCT 2, v.level_2_subject, v.level_2_subject, 1000
FROM finance_voucher v
WHERE v.level_2_subject IS NOT NULL AND v.level_2_subject <> ''
  AND NOT EXISTS (
    SELECT 1 FROM finance_subject s WHERE s.subject_level = 2
      AND s.subject_code = CONVERT(v.level_2_subject USING utf8mb4) COLLATE utf8mb4_general_ci
  );

UPDATE finance_voucher v
JOIN finance_subject s ON s.subject_level = 1
  AND s.subject_code = CONVERT(v.level_1_subject USING utf8mb4) COLLATE utf8mb4_general_ci
SET v.level_1_subject_id = s.id
WHERE v.level_1_subject_id IS NULL;
UPDATE finance_voucher v
JOIN finance_subject s ON s.subject_level = 2
  AND s.subject_code = CONVERT(v.level_2_subject USING utf8mb4) COLLATE utf8mb4_general_ci
SET v.level_2_subject_id = s.id
WHERE v.level_2_subject_id IS NULL;

-- Inspect this result. STRICT mode causes the NOT NULL alteration to fail if any IDs are unmapped.
SELECT uuid, voucher_no, level_1_subject, level_2_subject
FROM finance_voucher
WHERE level_1_subject_id IS NULL OR level_2_subject_id IS NULL;
ALTER TABLE finance_voucher
  MODIFY COLUMN level_1_subject_id bigint NOT NULL,
  MODIFY COLUMN level_2_subject_id bigint NOT NULL,
  MODIFY COLUMN level_1_subject varchar(32) DEFAULT NULL,
  MODIFY COLUMN level_2_subject varchar(64) DEFAULT NULL;

SET @finance_subject_ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_voucher'
    AND CONSTRAINT_NAME = 'fk_finance_voucher_level_1_subject') = 0,
  'ALTER TABLE finance_voucher ADD CONSTRAINT fk_finance_voucher_level_1_subject
 FOREIGN KEY (level_1_subject_id) REFERENCES finance_subject(id) ON DELETE RESTRICT ON UPDATE RESTRICT', 'SELECT 1');
PREPARE finance_subject_stmt FROM @finance_subject_ddl;
EXECUTE finance_subject_stmt;
DEALLOCATE PREPARE finance_subject_stmt;

SET @finance_subject_ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_voucher'
    AND CONSTRAINT_NAME = 'fk_finance_voucher_level_2_subject') = 0,
  'ALTER TABLE finance_voucher ADD CONSTRAINT fk_finance_voucher_level_2_subject
 FOREIGN KEY (level_2_subject_id) REFERENCES finance_subject(id) ON DELETE RESTRICT ON UPDATE RESTRICT', 'SELECT 1');
PREPARE finance_subject_stmt FROM @finance_subject_ddl;
EXECUTE finance_subject_stmt;
DEALLOCATE PREPARE finance_subject_stmt;

SET SESSION sql_mode = @finance_subject_original_sql_mode;
SELECT COUNT(*) AS voucher_count, COUNT(level_1_subject_id) AS level_1_mapped, COUNT(level_2_subject_id) AS level_2_mapped
FROM finance_voucher;
