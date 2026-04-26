USE salesmanagement;

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Clear business attachments and logs
TRUNCATE TABLE attachment;
TRUNCATE TABLE audit_trail;
TRUNCATE TABLE login_record;
TRUNCATE TABLE code_sequence;

-- 2. Clear contract and customer business data
TRUNCATE TABLE contract_clause;
TRUNCATE TABLE contract_basic_info;
TRUNCATE TABLE customer_contact;
TRUNCATE TABLE customer_info;

-- 3. Clear product and inventory business/master data
TRUNCATE TABLE product_history_price;
TRUNCATE TABLE product_basic_info;
TRUNCATE TABLE goods_transaction_dynamic_info;

-- 4. Clear project business data
TRUNCATE TABLE project_status_history;
TRUNCATE TABLE project_list_item;
TRUNCATE TABLE project_list;
TRUNCATE TABLE project_basic_info;

-- 5. Clear finance business data
TRUNCATE TABLE finance_voucher;

-- 6. Clear warehouse documents while preserving warehouse dictionary data
TRUNCATE TABLE warehouse_doc_item;
TRUNCATE TABLE warehouse_doc;

-- 7. Keep only System user and its role assignments
SET @system_user_id := (
    SELECT id
    FROM sys_user
    WHERE username = 'System'
    ORDER BY id
    LIMIT 1
);

DELETE FROM sys_user_role
WHERE user_id <> @system_user_id;

DELETE FROM sys_user
WHERE username <> 'System';

-- 8. Guarantee default warehouse still exists
INSERT INTO warehouse_info (
    warehouse_code,
    warehouse_name,
    warehouse_address,
    contact_name,
    contact_phone,
    remark
)
SELECT
    'WH-DEFAULT',
    '默认仓库',
    '默认仓库地址',
    '默认联系人',
    '00000000000',
    '系统初始化默认仓库'
WHERE NOT EXISTS (
    SELECT 1
    FROM warehouse_info
);

SET FOREIGN_KEY_CHECKS = 1;
