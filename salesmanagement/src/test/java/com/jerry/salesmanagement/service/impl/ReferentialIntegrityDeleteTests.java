package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CodeSequenceMapper;
import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.MaterialMasterService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferentialIntegrityDeleteTests {

    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;
    private DataSourceTransactionManager transactionManager;
    private CodeSequenceService codeSequenceService;
    private ReferenceCheckMapper referenceCheckMapper;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);

        jdbc.execute("CREATE TABLE code_sequence (sequence_key VARCHAR(128) PRIMARY KEY, current_value INT, description VARCHAR(255), create_time TIMESTAMP, update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE customer_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), customer_code VARCHAR(9), customer_name VARCHAR(100), region_id BIGINT, register_address VARCHAR(255), office_address VARCHAR(255), tax_identification_number VARCHAR(18), customer_type_id BIGINT, bank_name VARCHAR(100), bank_account VARCHAR(50), industry VARCHAR(100), remark TEXT, activity_status VARCHAR(20), last_active_at TIMESTAMP, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE customer_contact (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), client_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE project_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_number VARCHAR(10), project_name VARCHAR(200), customer_id VARCHAR(36), customer_uuid VARCHAR(36), manager_id BIGINT, participant_1_user_id BIGINT, participant_2_user_id BIGINT, participant_3_user_id BIGINT, region_id BIGINT, project_type_id BIGINT, warranty_until DATE, plan_start_time TIMESTAMP, plan_end_time TIMESTAMP, sales_contract_attachment_uuid VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE stage (id BIGINT PRIMARY KEY, sort_order INT)");
        jdbc.execute("CREATE TABLE project_status_history (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(64), project_id VARCHAR(64), stage_id BIGINT, created_at TIMESTAMP, created_by BIGINT)");
        jdbc.execute("CREATE TABLE project_list (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE project_list_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_list_id VARCHAR(36), material_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE contract_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_basic_info_id VARCHAR(36), sub_item_no VARCHAR(2), contract_type_id BIGINT, sub_item_content TEXT, contract_number VARCHAR(50), client_id VARCHAR(36), contact_id VARCHAR(36), sign_date DATE, contract_amount DECIMAL(15,2), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE contract_clause (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), contract_basic_info_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE finance_voucher (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_id VARCHAR(36), counterparty_customer_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE warehouse_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, warehouse_code VARCHAR(32), warehouse_name VARCHAR(100), warehouse_address VARCHAR(255), contact_name VARCHAR(100), contact_phone VARCHAR(50), remark VARCHAR(255))");
        jdbc.execute("CREATE TABLE warehouse_doc (doc_number VARCHAR(50) PRIMARY KEY, project_id VARCHAR(36), warehouse_id BIGINT, counterparty_customer_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE warehouse_doc_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), doc_number VARCHAR(50), material_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE goods_transaction_dynamic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), material_id VARCHAR(36), project_id VARCHAR(36), warehouse_id BIGINT)");
        jdbc.execute("CREATE TABLE product_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), brand_code VARCHAR(2), category_code VARCHAR(2), subcategory_code VARCHAR(2), sequence_no VARCHAR(3), band_code VARCHAR(3), material_code VARCHAR(12), category_id BIGINT, subcategory_id BIGINT, product_name VARCHAR(200), product_model VARCHAR(200), brand_id BIGINT, manufacturer VARCHAR(200), unit VARCHAR(50), frequency VARCHAR(100), specification TEXT, other_note TEXT, is_active BOOLEAN, created_at TIMESTAMP, created_by BIGINT, updated_at TIMESTAMP, updated_by BIGINT)");
        jdbc.execute("CREATE TABLE product_history_price (id BIGINT AUTO_INCREMENT PRIMARY KEY, product_basic_info_uuid VARCHAR(36), supplier_customer_id VARCHAR(36))");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/CodeSequenceMapper.xml"),
                new ClassPathResource("mapper/CustomerMapper.xml"),
                new ClassPathResource("mapper/MaterialMasterMapper.xml"),
                new ClassPathResource("mapper/WarehouseMapper.xml"),
                new ClassPathResource("mapper/ProjectMapper.xml"),
                new ClassPathResource("mapper/ProjectStatusHistoryMapper.xml"),
                new ClassPathResource("mapper/ContractBasicInfoMapper.xml"),
                new ClassPathResource("mapper/ReferenceCheckMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());
        referenceCheckMapper = sql.getMapper(ReferenceCheckMapper.class);

        CodeSequenceServiceImpl seqImpl = new CodeSequenceServiceImpl();
        ReflectionTestUtils.setField(seqImpl, "mapper", sql.getMapper(CodeSequenceMapper.class));
        codeSequenceService = transactional(seqImpl, CodeSequenceService.class);
    }

    @Test
    void customerDeleteBlockedWhenReferencedAndPreservesSequenceThenDeletesCleanlyWhenUnreferenced() {
        jdbc.update("INSERT INTO code_sequence(sequence_key, current_value) VALUES ('customer:11011', 2)");
        jdbc.update("INSERT INTO customer_info(uuid, customer_code, customer_name, region_id, customer_type_id, activity_status) VALUES ('c-1', '110110001', 'Cust1', 1, 1, 'ACTIVE'), ('c-2', '110110002', 'Cust2', 1, 1, 'ACTIVE')");
        jdbc.update("INSERT INTO customer_contact(uuid, client_id) VALUES ('cc-1', 'c-2')");
        jdbc.update("INSERT INTO contract_basic_info(uuid, project_basic_info_id, sub_item_no, contract_type_id, contract_number, client_id) VALUES ('ht-1', 'p-1', '01', 1, 'HT-1', 'c-2')");

        CustomerServiceImpl target = new CustomerServiceImpl();
        ReflectionTestUtils.setField(target, "customerMapper", sql.getMapper(CustomerMapper.class));
        ReflectionTestUtils.setField(target, "codeSequenceService", codeSequenceService);
        ReflectionTestUtils.setField(target, "referenceCheckMapper", referenceCheckMapper);
        CustomerService service = transactional(target, CustomerService.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete("c-2"));
        assertTrue(ex.getMessage().contains("客户联系人(1)"));
        assertTrue(ex.getMessage().contains("合同(1)"));
        assertEquals(2, count("customer_info"));
        assertEquals(2, jdbc.queryForObject("SELECT current_value FROM code_sequence WHERE sequence_key = 'customer:11011'", Integer.class));

        jdbc.update("DELETE FROM customer_contact WHERE uuid = 'cc-1'");
        jdbc.update("DELETE FROM contract_basic_info WHERE uuid = 'ht-1'");
        service.delete("c-2");
        assertEquals(1, count("customer_info"));
        assertEquals(1, jdbc.queryForObject("SELECT current_value FROM code_sequence WHERE sequence_key = 'customer:11011'", Integer.class));
    }

    @Test
    void materialDeleteBlockedWhenReferencedAndAllowedWhenUnreferenced() {
        jdbc.update("INSERT INTO code_sequence(sequence_key, current_value) VALUES ('material:CATEGORY_SUBCATEGORY_BRAND:3:010203', 1)");
        jdbc.update("INSERT INTO product_basic_info(uuid, brand_code, category_code, subcategory_code, sequence_no, band_code, material_code, category_id, product_name, is_active) VALUES ('m-1', '03', '01', '02', '001', 'AAA', '010203001AAA', 1, 'Mat1', true)");
        jdbc.update("INSERT INTO project_list_item(uuid, project_list_id, material_id) VALUES ('pli-1', 'pl-1', 'm-1')");
        jdbc.update("INSERT INTO goods_transaction_dynamic_info(uuid, material_id, warehouse_id) VALUES ('tx-1', 'm-1', 1)");

        MaterialMasterServiceImpl target = new MaterialMasterServiceImpl();
        ReflectionTestUtils.setField(target, "materialMasterMapper", sql.getMapper(MaterialMasterMapper.class));
        ReflectionTestUtils.setField(target, "codeSequenceService", codeSequenceService);
        ReflectionTestUtils.setField(target, "systemConfigService", mock(SystemConfigService.class));
        ReflectionTestUtils.setField(target, "referenceCheckMapper", referenceCheckMapper);
        MaterialMasterService service = transactional(target, MaterialMasterService.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete("m-1"));
        assertTrue(ex.getMessage().contains("项目清单明细(1)"));
        assertTrue(ex.getMessage().contains("库存流水(1)"));
        assertEquals(1, count("product_basic_info"));

        jdbc.update("DELETE FROM project_list_item WHERE uuid = 'pli-1'");
        jdbc.update("DELETE FROM goods_transaction_dynamic_info WHERE uuid = 'tx-1'");
        service.delete("m-1");
        assertEquals(0, count("product_basic_info"));
    }

    @Test
    void warehouseDeleteBlocksReservedWarehousesAndReferencedWarehousesButDeletesUnusedCustomOnes() {
        jdbc.update("INSERT INTO warehouse_info(id, warehouse_code, warehouse_name) VALUES (1, 'CK-XMCG', '项目采购仓'), (2, 'CK-JZCG', '集中采购仓'), (3, 'CK-CUSTOM', '临时仓库')");
        jdbc.update("INSERT INTO warehouse_doc(doc_number, warehouse_id) VALUES ('RKD202610001', 3)");

        WarehouseServiceImpl service = new WarehouseServiceImpl();
        ReflectionTestUtils.setField(service, "warehouseMapper", sql.getMapper(WarehouseMapper.class));
        ReflectionTestUtils.setField(service, "referenceCheckMapper", referenceCheckMapper);

        IllegalArgumentException reserved1 = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));
        assertTrue(reserved1.getMessage().contains("CK-XMCG"));
        IllegalArgumentException reserved2 = assertThrows(IllegalArgumentException.class, () -> service.delete(2L));
        assertTrue(reserved2.getMessage().contains("CK-JZCG"));

        IllegalArgumentException inUse = assertThrows(IllegalArgumentException.class, () -> service.delete(3L));
        assertTrue(inUse.getMessage().contains("出入库单(1)"));
        assertEquals(3, count("warehouse_info"));

        jdbc.update("DELETE FROM warehouse_doc WHERE doc_number = 'RKD202610001'");
        service.delete(3L);
        assertEquals(2, count("warehouse_info"));
    }

    @Test
    void projectDeleteAllowsBelowLockHistoryOnlyWhenNoChildBusinessRecordsExist() {
        jdbc.update("INSERT INTO stage(id, sort_order) VALUES (1, 2)");
        jdbc.update("INSERT INTO project_basic_info(uuid, project_number, project_name, customer_id, customer_uuid, manager_id, region_id, project_type_id) VALUES ('p-1', 'AB-1001', 'Proj1', 'c-1', 'c-1', 1, 1, 1)");
        jdbc.update("INSERT INTO project_status_history(uuid, project_id, stage_id) VALUES ('h-1', 'p-1', 1)");
        jdbc.update("INSERT INTO project_list(uuid, project_id) VALUES ('pl-1', 'p-1')");
        jdbc.update("INSERT INTO finance_voucher(uuid, project_id) VALUES ('fv-1', 'p-1')");

        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.getInt("project.delete.lock_stage_sort_order", 5)).thenReturn(5);

        ProjectServiceImpl service = new ProjectServiceImpl();
        ReflectionTestUtils.setField(service, "projectMapper", sql.getMapper(ProjectMapper.class));
        ReflectionTestUtils.setField(service, "projectStatusHistoryMapper", sql.getMapper(ProjectStatusHistoryMapper.class));
        ReflectionTestUtils.setField(service, "systemConfigService", configService);
        ReflectionTestUtils.setField(service, "referenceCheckMapper", referenceCheckMapper);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete("p-1"));
        assertTrue(ex.getMessage().contains("项目清单(1)"));
        assertTrue(ex.getMessage().contains("财务凭证(1)"));
        assertEquals(1, count("project_basic_info"));

        jdbc.update("DELETE FROM project_list WHERE uuid = 'pl-1'");
        jdbc.update("DELETE FROM finance_voucher WHERE uuid = 'fv-1'");
        service.delete("p-1");
        assertEquals(0, count("project_basic_info"));
    }

    @Test
    void contractDeleteBlockedWhenClausesExistAndAllowedAfterClausesRemoved() {
        jdbc.update("INSERT INTO contract_basic_info(uuid, project_basic_info_id, sub_item_no, contract_type_id, contract_number, client_id) VALUES ('ht-1', 'p-1', '01', 1, 'HT-1', 'c-1')");
        jdbc.update("INSERT INTO contract_clause(uuid, contract_basic_info_id) VALUES ('cl-1', 'ht-1')");

        ContractBasicInfoServiceImpl service = new ContractBasicInfoServiceImpl();
        ReflectionTestUtils.setField(service, "contractbasicInfoMapper", sql.getMapper(ContractBasicInfoMapper.class));
        ReflectionTestUtils.setField(service, "referenceCheckMapper", referenceCheckMapper);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete("ht-1"));
        assertTrue(ex.getMessage().contains("合同条款(1)"));
        assertEquals(1, count("contract_basic_info"));

        jdbc.update("DELETE FROM contract_clause WHERE uuid = 'cl-1'");
        service.delete("ht-1");
        assertEquals(0, count("contract_basic_info"));
    }

    private int count(String table) {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    @SuppressWarnings("unchecked")
    private <T> T transactional(Object target, Class<T> contract) {
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setInterfaces(contract);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        proxy.addAdvice(interceptor);
        return (T) proxy.getProxy();
    }
}
