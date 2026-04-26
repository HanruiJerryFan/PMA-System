USE salesmanagement;

DROP VIEW IF EXISTS `contract`;
CREATE VIEW `contract` AS
SELECT
    cbi.id AS id,
    cbi.uuid AS uuid,
    cbi.project_basic_info_id AS project_uuid,
    cbi.client_id AS customer_uuid,
    cbi.contact_id AS customer_contact_uuid,
    cbi.contract_type_id AS contract_type_id,
    cbi.sub_item_no AS sub_item_no,
    cbi.sub_item_content AS sub_item_content,
    cbi.contract_number AS contract_number,
    cbi.sign_date AS signed_at,
    cbi.contract_amount AS contract_amount,
    cbi.create_time AS created_at,
    cbi.create_user AS created_by,
    cbi.update_time AS updated_at,
    cbi.update_user AS updated_by
FROM contract_basic_info cbi;

DROP VIEW IF EXISTS `customer`;
CREATE VIEW `customer` AS
SELECT
    ci.id AS id,
    ci.uuid AS uuid,
    ci.customer_code AS customer_code,
    ci.customer_name AS name,
    ci.region_id AS region_id,
    ci.register_address AS registered_address,
    ci.office_address AS office_address,
    ci.tax_identification_number AS tax_identifier,
    ci.customer_type_id AS customer_type_id,
    ci.bank_name AS bank_name,
    ci.bank_account AS bank_account,
    ci.industry AS industry,
    ci.remark AS remark,
    ci.activity_status AS activity_status,
    ci.last_active_at AS last_active_at,
    ci.create_time AS created_at,
    ci.create_user AS created_by,
    ci.update_time AS updated_at,
    ci.update_user AS updated_by
FROM customer_info ci;

DROP VIEW IF EXISTS `inventory_txn`;
CREATE VIEW `inventory_txn` AS
SELECT
    gtdi.id AS id,
    gtdi.uuid AS uuid,
    gtdi.material_id AS material_id,
    gtdi.material_code AS material_code,
    gtdi.material_name AS material_name,
    gtdi.model AS model,
    gtdi.brand AS brand,
    gtdi.category AS category,
    gtdi.project_id AS project_id,
    gtdi.warehouse_id AS warehouse_id,
    gtdi.txn_type AS txn_type,
    gtdi.txn_time AS txn_time,
    gtdi.quantity AS quantity,
    gtdi.unit_price AS unit_price,
    gtdi.amount AS amount,
    gtdi.source_ref_type AS source_ref_type,
    gtdi.source_ref_id AS source_ref_id,
    gtdi.create_time AS created_at,
    gtdi.create_user AS created_by,
    gtdi.update_time AS updated_at,
    gtdi.update_user AS updated_by
FROM goods_transaction_dynamic_info gtdi;

DROP VIEW IF EXISTS `inventory_balance`;
CREATE VIEW `inventory_balance` AS
SELECT
    CONCAT(
        COALESCE(gtdi.material_id, ''),
        ':',
        COALESCE(CAST(gtdi.warehouse_id AS CHAR), ''),
        ':',
        COALESCE(gtdi.project_id, '')
    ) AS uuid,
    gtdi.material_id AS material_id,
    gtdi.material_code AS material_code,
    gtdi.material_name AS material_name,
    gtdi.model AS model,
    gtdi.brand AS brand,
    gtdi.category AS category,
    gtdi.warehouse_id AS warehouse_id,
    gtdi.project_id AS project_id,
    ROUND(SUM(CASE WHEN gtdi.txn_type = 'IN' THEN gtdi.quantity ELSE 0 END), 2) AS inbound_quantity,
    ROUND(SUM(CASE WHEN gtdi.txn_type = 'OUT' THEN gtdi.quantity ELSE 0 END), 2) AS outbound_quantity,
    ROUND(SUM(CASE WHEN gtdi.txn_type = 'IN' THEN gtdi.quantity ELSE -gtdi.quantity END), 2) AS balance_quantity,
    MAX(gtdi.txn_time) AS last_txn_time
FROM goods_transaction_dynamic_info gtdi
GROUP BY
    gtdi.material_id,
    gtdi.material_code,
    gtdi.material_name,
    gtdi.model,
    gtdi.brand,
    gtdi.category,
    gtdi.warehouse_id,
    gtdi.project_id;

DROP VIEW IF EXISTS `material_master`;
CREATE VIEW `material_master` AS
SELECT
    pbi.id AS id,
    pbi.uuid AS uuid,
    pbi.material_code AS material_code,
    pbi.category_id AS category_id,
    pbi.subcategory_id AS subcategory_id,
    pbi.product_name AS name,
    pbi.product_model AS model,
    pbi.brand_id AS brand_id,
    pbi.manufacturer AS manufacturer,
    pbi.unit AS unit,
    pbi.frequency AS frequency,
    pbi.specification AS specification,
    pbi.other_note AS other_note,
    pbi.is_active AS is_active,
    pbi.created_at AS created_at,
    pbi.created_by AS created_by,
    pbi.updated_at AS updated_at,
    pbi.updated_by AS updated_by
FROM product_basic_info pbi;

DROP VIEW IF EXISTS `project`;
CREATE VIEW `project` AS
SELECT
    pbi.id AS id,
    pbi.uuid AS uuid,
    pbi.project_number AS project_code,
    pbi.project_name AS name,
    pbi.customer_id AS customer_id,
    pbi.manager_id AS owner_user_id,
    pbi.participant_1_user_id AS participant_1_user_id,
    pbi.participant_2_user_id AS participant_2_user_id,
    pbi.participant_3_user_id AS participant_3_user_id,
    pbi.region_id AS region_id,
    pbi.project_type_id AS project_type_id,
    pbi.warranty_until AS warranty_until,
    pbi.plan_start_time AS planned_start_at,
    pbi.plan_end_time AS planned_end_at,
    pbi.sales_contract_attachment_uuid AS sales_contract_attachment_uuid,
    pbi.create_time AS created_at,
    pbi.create_user AS created_by,
    pbi.update_time AS updated_at,
    pbi.update_user AS updated_by
FROM project_basic_info pbi;

DROP VIEW IF EXISTS `warehouse`;
CREATE VIEW `warehouse` AS
SELECT
    wi.id AS id,
    wi.warehouse_code AS code,
    wi.warehouse_name AS name,
    wi.warehouse_address AS address,
    wi.contact_name AS contact_name,
    wi.contact_phone AS contact_phone,
    wi.remark AS remark
FROM warehouse_info wi;

DROP VIEW IF EXISTS `warehouse_document`;
CREATE VIEW `warehouse_document` AS
SELECT
    wd.doc_number AS doc_number,
    wd.doc_type AS doc_type,
    wd.business_category AS business_category,
    wd.project_id AS project_id,
    wd.warehouse_id AS warehouse_id,
    wd.counterparty_customer_id AS counterparty_customer_id,
    wd.counterparty_name AS counterparty_name,
    wd.counterparty_address AS counterparty_address,
    wd.counterparty_contact AS counterparty_contact,
    wd.contract_number AS contract_number,
    wd.doc_date AS doc_date,
    wd.total_amount AS total_amount,
    wd.doc_time AS doc_time,
    wd.handler_name AS handler_name,
    wd.source_ref_type AS source_ref_type,
    wd.source_ref_id AS source_ref_id,
    wd.remark AS remark,
    wd.create_time AS created_at,
    wd.create_user AS created_by,
    wd.update_time AS updated_at,
    wd.update_user AS updated_by
FROM warehouse_doc wd;
