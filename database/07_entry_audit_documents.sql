-- Add shared entry/auditor support for project lists, warehouse documents, and finance vouchers.
SET @schema_name = DATABASE();

SET @project_list_entry_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND COLUMN_NAME = 'entry_user'
);
SET @ddl = IF(
  @project_list_entry_column_exists = 0,
  'ALTER TABLE `project_list` ADD COLUMN `entry_user` bigint(20) DEFAULT NULL AFTER `entry_date`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @project_list_auditor_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND COLUMN_NAME = 'auditor_user'
);
SET @ddl = IF(
  @project_list_auditor_column_exists = 0,
  'ALTER TABLE `project_list` ADD COLUMN `auditor_user` bigint(20) DEFAULT NULL AFTER `entry_user`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @project_list_entry_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND INDEX_NAME = 'idx_project_list_entry_user'
);
SET @ddl = IF(
  @project_list_entry_index_exists = 0,
  'ALTER TABLE `project_list` ADD KEY `idx_project_list_entry_user` (`entry_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @project_list_auditor_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND INDEX_NAME = 'idx_project_list_auditor_user'
);
SET @ddl = IF(
  @project_list_auditor_index_exists = 0,
  'ALTER TABLE `project_list` ADD KEY `idx_project_list_auditor_user` (`auditor_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @finance_entry_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'finance_voucher'
    AND COLUMN_NAME = 'entry_user'
);
SET @ddl = IF(
  @finance_entry_column_exists = 0,
  'ALTER TABLE `finance_voucher` ADD COLUMN `entry_user` bigint(20) DEFAULT NULL AFTER `is_completed`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @finance_auditor_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'finance_voucher'
    AND COLUMN_NAME = 'auditor_user'
);
SET @ddl = IF(
  @finance_auditor_column_exists = 0,
  'ALTER TABLE `finance_voucher` ADD COLUMN `auditor_user` bigint(20) DEFAULT NULL AFTER `entry_user`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @finance_entry_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'finance_voucher'
    AND INDEX_NAME = 'idx_finance_voucher_entry_user'
);
SET @ddl = IF(
  @finance_entry_index_exists = 0,
  'ALTER TABLE `finance_voucher` ADD KEY `idx_finance_voucher_entry_user` (`entry_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @finance_auditor_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'finance_voucher'
    AND INDEX_NAME = 'idx_finance_voucher_auditor_user'
);
SET @ddl = IF(
  @finance_auditor_index_exists = 0,
  'ALTER TABLE `finance_voucher` ADD KEY `idx_finance_voucher_auditor_user` (`auditor_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @warehouse_entry_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'warehouse_doc'
    AND COLUMN_NAME = 'entry_user'
);
SET @ddl = IF(
  @warehouse_entry_column_exists = 0,
  'ALTER TABLE `warehouse_doc` ADD COLUMN `entry_user` bigint(20) DEFAULT NULL AFTER `source_ref_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @warehouse_auditor_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'warehouse_doc'
    AND COLUMN_NAME = 'auditor_user'
);
SET @ddl = IF(
  @warehouse_auditor_column_exists = 0,
  'ALTER TABLE `warehouse_doc` ADD COLUMN `auditor_user` bigint(20) DEFAULT NULL AFTER `entry_user`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @warehouse_entry_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'warehouse_doc'
    AND INDEX_NAME = 'idx_warehouse_doc_entry_user'
);
SET @ddl = IF(
  @warehouse_entry_index_exists = 0,
  'ALTER TABLE `warehouse_doc` ADD KEY `idx_warehouse_doc_entry_user` (`entry_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @warehouse_auditor_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'warehouse_doc'
    AND INDEX_NAME = 'idx_warehouse_doc_auditor_user'
);
SET @ddl = IF(
  @warehouse_auditor_index_exists = 0,
  'ALTER TABLE `warehouse_doc` ADD KEY `idx_warehouse_doc_auditor_user` (`auditor_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE project_list
SET entry_user = create_user
WHERE entry_user IS NULL
  AND create_user IS NOT NULL;

UPDATE finance_voucher
SET entry_user = created_by
WHERE entry_user IS NULL
  AND created_by IS NOT NULL;

UPDATE warehouse_doc
SET entry_user = create_user
WHERE entry_user IS NULL
  AND create_user IS NOT NULL;

INSERT INTO sys_permission (permission_code, permission_name, create_time, create_user)
SELECT 'project.list.entry', '项目清单录入', NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'project.list.entry');

INSERT INTO sys_permission (permission_code, permission_name, create_time, create_user)
SELECT 'project.list.audit', '项目清单审核', NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'project.list.audit');

INSERT INTO sys_permission (permission_code, permission_name, create_time, create_user)
SELECT 'inventory.warehouse-doc.entry', '出入库单录入', NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'inventory.warehouse-doc.entry');

INSERT INTO sys_permission (permission_code, permission_name, create_time, create_user)
SELECT 'inventory.warehouse-doc.audit', '出入库单审核', NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'inventory.warehouse-doc.audit');

INSERT INTO sys_permission (permission_code, permission_name, create_time, create_user)
SELECT 'finance.voucher.entry', '财务凭证录入', NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance.voucher.entry');

INSERT INTO sys_permission (permission_code, permission_name, create_time, create_user)
SELECT 'finance.voucher.audit', '财务凭证审核', NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance.voucher.audit');

INSERT INTO sys_permission (permission_code, permission_name, create_time, create_user)
SELECT 'document.entry-audit.manage', '录入审核人员维护', NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'document.entry-audit.manage');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
  'project.list.entry',
  'project.list.audit',
  'inventory.warehouse-doc.entry',
  'inventory.warehouse-doc.audit',
  'finance.voucher.entry',
  'finance.voucher.audit',
  'document.entry-audit.manage'
)
WHERE r.role_name IN ('System', '管理员');
