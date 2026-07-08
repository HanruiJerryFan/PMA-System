-- Add entry/auditor support to each project list detail row.
SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @project_list_item_entry_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list_item'
    AND COLUMN_NAME = 'entry_user'
);
SET @ddl = IF(
  @project_list_item_entry_column_exists = 0,
  'ALTER TABLE `project_list_item` ADD COLUMN `entry_user` bigint(20) DEFAULT NULL AFTER `remark`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @project_list_item_auditor_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list_item'
    AND COLUMN_NAME = 'auditor_user'
);
SET @ddl = IF(
  @project_list_item_auditor_column_exists = 0,
  'ALTER TABLE `project_list_item` ADD COLUMN `auditor_user` bigint(20) DEFAULT NULL AFTER `entry_user`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @project_list_item_entry_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list_item'
    AND INDEX_NAME = 'idx_project_list_item_entry_user'
);
SET @ddl = IF(
  @project_list_item_entry_index_exists = 0,
  'ALTER TABLE `project_list_item` ADD KEY `idx_project_list_item_entry_user` (`entry_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @project_list_item_auditor_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list_item'
    AND INDEX_NAME = 'idx_project_list_item_auditor_user'
);
SET @ddl = IF(
  @project_list_item_auditor_index_exists = 0,
  'ALTER TABLE `project_list_item` ADD KEY `idx_project_list_item_auditor_user` (`auditor_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE project_list_item
SET entry_user = create_user
WHERE entry_user IS NULL
  AND create_user IS NOT NULL;
