-- Add business entry/auditor users for project lists.
SET @schema_name = DATABASE();

SET @entry_user_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND COLUMN_NAME = 'entry_user'
);
SET @ddl = IF(
  @entry_user_column_exists = 0,
  'ALTER TABLE `project_list` ADD COLUMN `entry_user` bigint(20) DEFAULT NULL AFTER `entry_date`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @auditor_user_column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND COLUMN_NAME = 'auditor_user'
);
SET @ddl = IF(
  @auditor_user_column_exists = 0,
  'ALTER TABLE `project_list` ADD COLUMN `auditor_user` bigint(20) DEFAULT NULL AFTER `entry_user`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @entry_user_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND INDEX_NAME = 'idx_project_list_entry_user'
);
SET @ddl = IF(
  @entry_user_index_exists = 0,
  'ALTER TABLE `project_list` ADD KEY `idx_project_list_entry_user` (`entry_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @auditor_user_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'project_list'
    AND INDEX_NAME = 'idx_project_list_auditor_user'
);
SET @ddl = IF(
  @auditor_user_index_exists = 0,
  'ALTER TABLE `project_list` ADD KEY `idx_project_list_auditor_user` (`auditor_user`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
