USE salesmanagement;

-- Cleared business tables
ALTER TABLE attachment AUTO_INCREMENT = 1;
ALTER TABLE audit_trail AUTO_INCREMENT = 1;
ALTER TABLE contract_basic_info AUTO_INCREMENT = 1;
ALTER TABLE contract_clause AUTO_INCREMENT = 1;
ALTER TABLE customer_contact AUTO_INCREMENT = 1;
ALTER TABLE customer_info AUTO_INCREMENT = 1;
ALTER TABLE finance_voucher AUTO_INCREMENT = 1;
ALTER TABLE goods_transaction_dynamic_info AUTO_INCREMENT = 1;
ALTER TABLE login_record AUTO_INCREMENT = 1;
ALTER TABLE product_basic_info AUTO_INCREMENT = 1;
ALTER TABLE product_history_price AUTO_INCREMENT = 1;
ALTER TABLE project_basic_info AUTO_INCREMENT = 1;
ALTER TABLE project_list AUTO_INCREMENT = 1;
ALTER TABLE project_list_item AUTO_INCREMENT = 1;
ALTER TABLE project_status_history AUTO_INCREMENT = 1;
ALTER TABLE warehouse_doc_item AUTO_INCREMENT = 1;

-- Seed/config/auth tables
ALTER TABLE clause_type AUTO_INCREMENT = 7;
ALTER TABLE contract_type AUTO_INCREMENT = 6;
ALTER TABLE customer_activity_rule AUTO_INCREMENT = 5;
ALTER TABLE customer_industry_dict AUTO_INCREMENT = 15;
ALTER TABLE customer_type AUTO_INCREMENT = 6;
ALTER TABLE product_band AUTO_INCREMENT = 7;
ALTER TABLE product_brand AUTO_INCREMENT = 100;
ALTER TABLE product_category AUTO_INCREMENT = 7;
ALTER TABLE product_subcategory AUTO_INCREMENT = 100;
ALTER TABLE project_type AUTO_INCREMENT = 5;
ALTER TABLE region AUTO_INCREMENT = 4063;
ALTER TABLE stage AUTO_INCREMENT = 15;
ALTER TABLE system_config AUTO_INCREMENT = 6;
ALTER TABLE sys_permission AUTO_INCREMENT = 23;
ALTER TABLE sys_role AUTO_INCREMENT = 4;
ALTER TABLE sys_user AUTO_INCREMENT = 2;
ALTER TABLE tax_rate_dict AUTO_INCREMENT = 7;
ALTER TABLE warehouse_info AUTO_INCREMENT = 2;
