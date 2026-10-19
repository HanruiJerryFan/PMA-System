-- Optional index for transfer document lookups; reuse the existing warehouses and their IDs.

SET @transfer_index_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'warehouse_doc' AND INDEX_NAME = 'idx_warehouse_doc_source'),
    'SELECT 1',
    'ALTER TABLE warehouse_doc ADD INDEX idx_warehouse_doc_source (source_ref_type, source_ref_id)'
);
PREPARE transfer_index_statement FROM @transfer_index_sql;
EXECUTE transfer_index_statement;
DEALLOCATE PREPARE transfer_index_statement;

SELECT id, warehouse_code, warehouse_name FROM warehouse_info
WHERE UPPER(TRIM(warehouse_code)) IN ('CK-XMCG', 'CK-JZCG')
   OR UPPER(TRIM(warehouse_name)) IN ('CK-XMCG', 'CK-JZCG');
