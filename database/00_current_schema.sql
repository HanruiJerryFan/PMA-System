mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 5.7.19, for Win64 (x86_64)
--
-- Host: localhost    Database: salesmanagement
-- ------------------------------------------------------
-- Server version	5.7.19

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `attachment`
--

DROP TABLE IF EXISTS `attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `attachment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `business_type` varchar(64) NOT NULL,
  `business_uuid` varchar(36) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `original_file_name` varchar(255) DEFAULT NULL,
  `file_ext` varchar(32) DEFAULT NULL,
  `mime_type` varchar(128) DEFAULT NULL,
  `file_size` bigint(20) DEFAULT NULL,
  `storage_path` varchar(500) NOT NULL,
  `uploaded_by` bigint(20) DEFAULT NULL,
  `uploaded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attachment_uuid` (`uuid`),
  KEY `idx_attachment_business_type` (`business_type`),
  KEY `idx_attachment_business_uuid` (`business_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_trail`
--

DROP TABLE IF EXISTS `audit_trail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `audit_trail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `module` varchar(64) NOT NULL,
  `action` varchar(64) NOT NULL,
  `target_type` varchar(64) DEFAULT NULL,
  `target_id` varchar(128) DEFAULT NULL,
  `detail` text,
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user_id` (`user_id`),
  KEY `idx_audit_module` (`module`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_occurred_at` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `clause_type`
--

DROP TABLE IF EXISTS `clause_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `clause_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(100) NOT NULL COMMENT '条款类别描述',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `code_sequence`
--

DROP TABLE IF EXISTS `code_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `code_sequence` (
  `sequence_key` varchar(128) NOT NULL,
  `current_value` int(11) NOT NULL DEFAULT '0',
  `description` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sequence_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_basic_info`
--

DROP TABLE IF EXISTS `contract_basic_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_basic_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `project_basic_info_id` varchar(36) NOT NULL COMMENT '关联project_basic_info.uuid',
  `sub_item_no` varchar(2) NOT NULL COMMENT '分项号(2位数字)',
  `contract_type_id` bigint(20) NOT NULL COMMENT '关联contract_type.id',
  `sub_item_content` text COMMENT '分项合同内容',
  `contract_number` varchar(50) NOT NULL COMMENT '合同编号',
  `client_id` varchar(36) NOT NULL COMMENT '关联customer_info.uuid',
  `contact_id` varchar(36) DEFAULT NULL COMMENT '关联customer_contact.uuid',
  `sign_date` date DEFAULT NULL COMMENT '签订日期',
  `contract_amount` decimal(15,2) DEFAULT NULL COMMENT '合同金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_clause`
--

DROP TABLE IF EXISTS `contract_clause`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_clause` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `contract_basic_info_id` varchar(36) NOT NULL COMMENT '关联contract_basic_info.uuid',
  `clause_type_id` bigint(20) NOT NULL COMMENT '关联clause_type.id',
  `clause_description` text COMMENT '条款描述',
  `clause_amount` decimal(15,2) DEFAULT NULL COMMENT '条款金额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_type`
--

DROP TABLE IF EXISTS `contract_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type_name` varchar(50) NOT NULL COMMENT '合同类别名称(如: 销售)',
  `type_code` varchar(2) NOT NULL COMMENT '合同类别代号(如: X)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_activity_rule`
--

DROP TABLE IF EXISTS `customer_activity_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_activity_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `status_code` varchar(20) NOT NULL,
  `min_idle_days` int(11) NOT NULL,
  `max_idle_days` int(11) DEFAULT NULL,
  `sort_order` int(11) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_activity_rule_status` (`status_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_contact`
--

DROP TABLE IF EXISTS `customer_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_contact` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `client_id` varchar(36) NOT NULL COMMENT '关联customer_info.uuid',
  `contact_name` varchar(50) NOT NULL COMMENT '联系人姓名',
  `contact_position` varchar(50) DEFAULT NULL COMMENT '联系人职务',
  `contact_phone` varchar(50) DEFAULT NULL COMMENT '联系人电话',
  `wechat` varchar(64) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `qq` varchar(32) DEFAULT NULL,
  `remark` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_industry_dict`
--

DROP TABLE IF EXISTS `customer_industry_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_industry_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `industry_name` varchar(64) NOT NULL COMMENT 'Industry name',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `industry_name` (`industry_name`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_info`
--

DROP TABLE IF EXISTS `customer_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `customer_code` varchar(9) NOT NULL COMMENT '9-digit customer system ID',
  `customer_name` varchar(100) NOT NULL,
  `region_id` mediumint(7) unsigned NOT NULL,
  `register_address` varchar(255) DEFAULT NULL COMMENT '注册地址',
  `office_address` varchar(255) DEFAULT NULL COMMENT '办公地址',
  `tax_identification_number` varchar(18) DEFAULT NULL COMMENT '纳税人识别号(18位)',
  `customer_type_id` bigint(20) NOT NULL COMMENT '客户类型(关联customer_type.id)',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '开户银行',
  `bank_account` varchar(50) DEFAULT NULL COMMENT '银行账号',
  `industry` varchar(100) DEFAULT NULL,
  `remark` text,
  `activity_status` varchar(20) NOT NULL COMMENT 'ACTIVE / NORMAL / INACTIVE / DORMANT',
  `last_active_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `uk_customer_info_customer_code` (`customer_code`),
  UNIQUE KEY `uk_customer_info_tax_identification_number` (`tax_identification_number`),
  KEY `idx_customer_info_customer_name` (`customer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_customer_info_validate_insert`
BEFORE INSERT ON `customer_info`
FOR EACH ROW
BEGIN
    IF NEW.tax_identification_number IS NOT NULL
       AND TRIM(NEW.tax_identification_number) <> ''
       AND CHAR_LENGTH(TRIM(NEW.tax_identification_number)) <> 18 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'tax_identification_number must be 18 characters';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_customer_info_validate_update`
BEFORE UPDATE ON `customer_info`
FOR EACH ROW
BEGIN
    IF NEW.tax_identification_number IS NOT NULL
       AND TRIM(NEW.tax_identification_number) <> ''
       AND CHAR_LENGTH(TRIM(NEW.tax_identification_number)) <> 18 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'tax_identification_number must be 18 characters';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `customer_type`
--

DROP TABLE IF EXISTS `customer_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type_code` char(1) NOT NULL,
  `type_name` varchar(100) NOT NULL COMMENT '客户类型名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_type_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_voucher`
--

DROP TABLE IF EXISTS `finance_voucher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `finance_voucher` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `voucher_no` varchar(10) NOT NULL,
  `occurred_on` date NOT NULL,
  `project_id` varchar(36) DEFAULT NULL,
  `level_1_subject` varchar(32) NOT NULL,
  `level_2_subject` varchar(64) NOT NULL,
  `summary` varchar(500) NOT NULL,
  `transaction_direction` varchar(16) NOT NULL,
  `tax_rate` decimal(5,4) NOT NULL,
  `counterparty_customer_id` varchar(36) DEFAULT NULL,
  `counterparty_name_snapshot` varchar(255) NOT NULL,
  `invoice_status` varchar(32) NOT NULL DEFAULT 'NOT_INVOICED',
  `invoice_no` varchar(128) DEFAULT NULL,
  `invoice_amount` decimal(18,2) DEFAULT NULL,
  `actual_income_amount` decimal(18,2) DEFAULT NULL,
  `actual_expense_amount` decimal(18,2) DEFAULT NULL,
  `booked_amount` decimal(18,2) NOT NULL,
  `is_completed` tinyint(1) NOT NULL DEFAULT '1',
  `entry_user` bigint(20) DEFAULT NULL,
  `auditor_user` bigint(20) DEFAULT NULL,
  `remark` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint(20) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `voucher_no` (`voucher_no`),
  KEY `idx_finance_voucher_project_id` (`project_id`),
  KEY `idx_finance_voucher_counterparty_customer_id` (`counterparty_customer_id`),
  KEY `idx_finance_voucher_invoice_status` (`invoice_status`),
  KEY `idx_finance_voucher_entry_user` (`entry_user`),
  KEY `idx_finance_voucher_auditor_user` (`auditor_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_finance_voucher_validate_insert`
BEFORE INSERT ON `finance_voucher`
FOR EACH ROW
BEGIN
    IF NEW.voucher_no IS NULL
       OR NEW.voucher_no NOT REGEXP '^[0-9]{10}$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'voucher_no must be 10 digits';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_finance_voucher_validate_update`
BEFORE UPDATE ON `finance_voucher`
FOR EACH ROW
BEGIN
    IF NEW.voucher_no IS NULL
       OR NEW.voucher_no NOT REGEXP '^[0-9]{10}$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'voucher_no must be 10 digits';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `goods_transaction_dynamic_info`
--

DROP TABLE IF EXISTS `goods_transaction_dynamic_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `goods_transaction_dynamic_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `material_id` varchar(36) NOT NULL,
  `material_code` varchar(12) DEFAULT NULL,
  `material_name` varchar(200) NOT NULL DEFAULT '',
  `model` varchar(200) DEFAULT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `category` varchar(64) NOT NULL DEFAULT 'PROJECT_PURCHASE',
  `project_id` varchar(36) DEFAULT NULL,
  `warehouse_id` bigint(20) DEFAULT NULL,
  `txn_type` varchar(20) DEFAULT NULL,
  `txn_time` datetime DEFAULT NULL,
  `quantity` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '数量',
  `unit_price` decimal(15,4) DEFAULT '0.0000',
  `amount` decimal(15,2) DEFAULT NULL,
  `source_ref_type` varchar(64) DEFAULT NULL,
  `source_ref_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idx_inventory_txn_time` (`txn_time`),
  KEY `idx_inventory_material_warehouse_project` (`material_id`,`warehouse_id`,`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `login_record`
--

DROP TABLE IF EXISTS `login_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `login_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '登录用户ID',
  `login_start_time` datetime NOT NULL COMMENT '登录开始时间',
  `login_end_time` datetime DEFAULT NULL COMMENT '登录结束时间',
  `duration_seconds` bigint(20) DEFAULT NULL,
  `login_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `last_seen_at` datetime DEFAULT NULL COMMENT 'Last heartbeat time',
  `logout_reason` varchar(32) DEFAULT NULL COMMENT 'MANUAL / TIMEOUT / BROWSER_CLOSED / FORCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_band`
--

DROP TABLE IF EXISTS `product_band`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_band` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(3) DEFAULT NULL,
  `band_name` varchar(100) NOT NULL,
  `sort_order` int(11) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_basic_info`
--

DROP TABLE IF EXISTS `product_basic_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_basic_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `brand_code` varchar(2) DEFAULT NULL,
  `category_code` varchar(2) DEFAULT NULL,
  `subcategory_code` varchar(2) DEFAULT NULL,
  `sequence_no` varchar(3) DEFAULT NULL,
  `band_code` varchar(3) DEFAULT NULL,
  `material_code` varchar(12) NOT NULL,
  `category_id` bigint(20) NOT NULL COMMENT '关联product_category.id',
  `subcategory_id` bigint(20) DEFAULT NULL,
  `product_name` varchar(200) NOT NULL COMMENT '商品名称',
  `product_model` varchar(200) DEFAULT NULL COMMENT '商品型号',
  `brand_id` bigint(20) DEFAULT NULL COMMENT '关联product_brand.id',
  `manufacturer` varchar(200) DEFAULT NULL COMMENT '生产单位/厂家',
  `unit` varchar(50) DEFAULT NULL COMMENT '计量单位(如: 个/台/套)',
  `frequency` varchar(100) DEFAULT NULL,
  `specification` text,
  `other_note` text,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint(20) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `uk_product_basic_info_material_code` (`material_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_product_basic_info_validate_insert`
BEFORE INSERT ON `product_basic_info`
FOR EACH ROW
BEGIN
    IF NEW.material_code IS NULL
       OR CHAR_LENGTH(TRIM(NEW.material_code)) <> 12 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'material_code must be 12 characters';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_product_basic_info_validate_update`
BEFORE UPDATE ON `product_basic_info`
FOR EACH ROW
BEGIN
    IF NEW.material_code IS NULL
       OR CHAR_LENGTH(TRIM(NEW.material_code)) <> 12 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'material_code must be 12 characters';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `product_brand`
--

DROP TABLE IF EXISTS `product_brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_brand` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(2) DEFAULT NULL,
  `brand_name` varchar(100) NOT NULL,
  `sort_order` int(11) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_category`
--

DROP TABLE IF EXISTS `product_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(2) DEFAULT NULL,
  `category_name` varchar(100) NOT NULL,
  `sort_order` int(11) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_history_price`
--

DROP TABLE IF EXISTS `product_history_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_history_price` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_basic_info_uuid` varchar(36) NOT NULL COMMENT '关联product_basic_info.uuid',
  `reference_price` decimal(15,2) DEFAULT NULL COMMENT '参考价格',
  `supplier_customer_id` varchar(36) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_subcategory`
--

DROP TABLE IF EXISTS `product_subcategory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_subcategory` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(2) DEFAULT NULL,
  `subcategory_name` varchar(100) NOT NULL,
  `sort_order` int(11) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_basic_info`
--

DROP TABLE IF EXISTS `project_basic_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_basic_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `project_number` varchar(10) NOT NULL COMMENT '项目编号(如: AB-1234)',
  `project_name` varchar(200) NOT NULL COMMENT '项目名称',
  `customer_id` varchar(36) DEFAULT NULL,
  `customer_uuid` varchar(36) DEFAULT NULL,
  `manager_id` bigint(20) NOT NULL COMMENT '负责人(关联sys_user.id)',
  `participant_1_user_id` bigint(20) DEFAULT NULL,
  `participant_2_user_id` bigint(20) DEFAULT NULL,
  `participant_3_user_id` bigint(20) DEFAULT NULL,
  `region_id` mediumint(7) unsigned NOT NULL COMMENT '项目所在区域',
  `project_type_id` bigint(20) NOT NULL COMMENT '项目类型(关联project_type.id)',
  `warranty_until` date DEFAULT NULL COMMENT 'Warranty until date',
  `plan_start_time` datetime DEFAULT NULL COMMENT '项目计划开始时间',
  `plan_end_time` datetime DEFAULT NULL COMMENT '项目计划结束时间',
  `sales_contract_attachment_uuid` varchar(36) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `uk_project_basic_info_project_number` (`project_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_project_basic_info_validate_insert`
BEFORE INSERT ON `project_basic_info`
FOR EACH ROW
BEGIN
    IF NEW.customer_id IS NULL AND NEW.customer_uuid IS NOT NULL AND TRIM(NEW.customer_uuid) <> '' THEN
        SET NEW.customer_id = NEW.customer_uuid;
    END IF;
    IF NEW.customer_uuid IS NULL AND NEW.customer_id IS NOT NULL AND TRIM(NEW.customer_id) <> '' THEN
        SET NEW.customer_uuid = NEW.customer_id;
    END IF;
    IF NEW.project_number IS NULL OR NEW.project_number NOT REGEXP '^[A-Z]{2}-[0-9]{4}$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'project_number must match format XX-0000';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_project_basic_info_validate_update`
BEFORE UPDATE ON `project_basic_info`
FOR EACH ROW
BEGIN
    IF NEW.customer_id IS NULL AND NEW.customer_uuid IS NOT NULL AND TRIM(NEW.customer_uuid) <> '' THEN
        SET NEW.customer_id = NEW.customer_uuid;
    END IF;
    IF NEW.customer_uuid IS NULL AND NEW.customer_id IS NOT NULL AND TRIM(NEW.customer_id) <> '' THEN
        SET NEW.customer_uuid = NEW.customer_id;
    END IF;
    IF NEW.project_number IS NULL OR NEW.project_number NOT REGEXP '^[A-Z]{2}-[0-9]{4}$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'project_number must match format XX-0000';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `project_list`
--

DROP TABLE IF EXISTS `project_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_list` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `project_id` varchar(36) NOT NULL,
  `list_name` varchar(128) NOT NULL DEFAULT '',
  `list_type` varchar(32) NOT NULL,
  `entry_date` date DEFAULT NULL,
  `entry_user` bigint(20) DEFAULT NULL,
  `auditor_user` bigint(20) DEFAULT NULL,
  `pdf_attachment_id` varchar(36) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_list_uuid` (`uuid`),
  KEY `idx_project_list_type` (`list_type`),
  KEY `idx_project_list_project_id` (`project_id`),
  KEY `idx_project_list_entry_user` (`entry_user`),
  KEY `idx_project_list_auditor_user` (`auditor_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_list_item`
--

DROP TABLE IF EXISTS `project_list_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_list_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `project_list_id` varchar(36) NOT NULL,
  `material_id` varchar(36) DEFAULT NULL,
  `material_code` varchar(12) DEFAULT NULL,
  `item_name` varchar(255) NOT NULL,
  `model` varchar(255) DEFAULT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `unit` varchar(32) DEFAULT NULL,
  `quantity` decimal(18,4) NOT NULL,
  `unit_price` decimal(18,2) DEFAULT NULL,
  `total_amount` decimal(18,2) DEFAULT NULL,
  `source_type` varchar(64) DEFAULT NULL,
  `remark` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_list_item_uuid` (`uuid`),
  KEY `idx_project_list_item_list_id` (`project_list_id`),
  KEY `idx_project_list_item_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_status_history`
--

DROP TABLE IF EXISTS `project_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_status_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  `stage_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_status_history_uuid` (`uuid`),
  KEY `idx_project_status_history_stage_id` (`stage_id`),
  KEY `idx_project_status_history_project_id` (`project_id`),
  KEY `idx_project_status_history_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_type`
--

DROP TABLE IF EXISTS `project_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `type_name` varchar(100) NOT NULL COMMENT '项目类型',
  `type_desc` varchar(100) NOT NULL COMMENT '项目类型说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_type_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `region` (
  `id` mediumint(7) unsigned NOT NULL AUTO_INCREMENT,
  `level` tinyint(1) unsigned NOT NULL COMMENT '层级',
  `parent_code` bigint(14) unsigned NOT NULL DEFAULT '0' COMMENT '父级行政代码',
  `area_code` bigint(14) unsigned NOT NULL DEFAULT '0' COMMENT '行政代码',
  `zip_code` mediumint(6) unsigned zerofill NOT NULL DEFAULT '000000' COMMENT '邮政编码',
  `city_code` char(6) NOT NULL DEFAULT '' COMMENT '区号',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '名称',
  `short_name` varchar(50) NOT NULL DEFAULT '' COMMENT '简称',
  `merger_name` varchar(50) NOT NULL DEFAULT '' COMMENT '组合名',
  `pinyin` varchar(30) NOT NULL DEFAULT '' COMMENT '拼音',
  `lng` decimal(10,6) NOT NULL DEFAULT '0.000000' COMMENT '经度',
  `lat` decimal(10,6) NOT NULL DEFAULT '0.000000' COMMENT '纬度',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`area_code`) USING BTREE,
  KEY `idx_parent_code` (`parent_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4063 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stage`
--

DROP TABLE IF EXISTS `stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stage_code` varchar(16) NOT NULL,
  `description` varchar(128) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_stage_code` (`stage_code`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code` varchar(128) NOT NULL,
  `permission_name` varchar(100) NOT NULL COMMENT '权限名称或标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_permission_code` (`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint(20) DEFAULT NULL COMMENT '创建人(用户ID)',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint(20) DEFAULT NULL COMMENT '更新人(用户ID)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_permission` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `permission_id` bigint(20) NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（加密后）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名/昵称',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `force_password_change` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint(20) DEFAULT '1' COMMENT '创建人(用户ID)',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint(20) DEFAULT NULL COMMENT '更新人(用户ID)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_user_role_role` (`role_id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `system_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `config_key` varchar(128) NOT NULL,
  `config_value` varchar(512) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tax_rate_dict`
--

DROP TABLE IF EXISTS `tax_rate_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tax_rate_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `rate` decimal(4,2) NOT NULL,
  `label` varchar(32) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tax_rate_dict_rate` (`rate`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse_doc`
--

DROP TABLE IF EXISTS `warehouse_doc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `warehouse_doc` (
  `doc_number` varchar(50) NOT NULL COMMENT '仓库单号(唯一)',
  `doc_type` varchar(50) DEFAULT NULL,
  `business_category` varchar(64) NOT NULL DEFAULT 'PROJECT_PURCHASE',
  `project_id` varchar(36) DEFAULT NULL,
  `warehouse_id` bigint(20) DEFAULT NULL,
  `counterparty_customer_id` varchar(36) DEFAULT NULL,
  `counterparty_name` varchar(200) NOT NULL DEFAULT '',
  `counterparty_address` varchar(255) DEFAULT NULL,
  `counterparty_contact` varchar(255) DEFAULT NULL,
  `contract_number` varchar(64) DEFAULT NULL,
  `doc_date` datetime DEFAULT NULL,
  `total_amount` decimal(18,2) DEFAULT NULL,
  `doc_time` datetime DEFAULT NULL,
  `handler_name` varchar(100) DEFAULT NULL,
  `source_ref_type` varchar(100) DEFAULT NULL,
  `source_ref_id` varchar(100) DEFAULT NULL,
  `entry_user` bigint(20) DEFAULT NULL,
  `auditor_user` bigint(20) DEFAULT NULL,
  `remark` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`doc_number`),
  KEY `idx_warehouse_doc_entry_user` (`entry_user`),
  KEY `idx_warehouse_doc_auditor_user` (`auditor_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse_doc_item`
--

DROP TABLE IF EXISTS `warehouse_doc_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `warehouse_doc_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `doc_number` varchar(50) NOT NULL,
  `line_no` int(11) NOT NULL,
  `material_id` varchar(36) NOT NULL,
  `material_code` varchar(12) DEFAULT NULL,
  `material_name` varchar(200) NOT NULL,
  `model` varchar(200) DEFAULT NULL,
  `display_name` varchar(200) NOT NULL,
  `display_model` varchar(200) NOT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `unit` varchar(32) DEFAULT NULL,
  `quantity` decimal(18,4) NOT NULL,
  `unit_price` decimal(18,4) DEFAULT NULL,
  `amount` decimal(18,2) DEFAULT NULL,
  `remark` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idx_warehouse_doc_item_doc_number` (`doc_number`),
  KEY `idx_warehouse_doc_item_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse_info`
--

DROP TABLE IF EXISTS `warehouse_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `warehouse_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `warehouse_code` varchar(32) NOT NULL,
  `warehouse_name` varchar(100) NOT NULL,
  `warehouse_address` varchar(255) DEFAULT NULL,
  `contact_name` varchar(100) DEFAULT NULL,
  `contact_phone` varchar(50) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-29  2:37:19
