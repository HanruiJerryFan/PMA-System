package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.ProjectListItemMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentItemMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.MaterialReconciliationItemDto;
import com.jerry.salesmanagement.pojo.dto.ProjectMaterialReconciliationDto;
import com.jerry.salesmanagement.pojo.dto.ProjectReconciliationOverviewItemDto;
import com.jerry.salesmanagement.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ProjectMaterialReconciliationServiceTests {

    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);

        jdbc.execute("CREATE TABLE sys_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), password VARCHAR(255), real_name VARCHAR(50), status VARCHAR(16), force_password_change BOOLEAN, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE customer_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), customer_code VARCHAR(9), customer_name VARCHAR(100), region_id BIGINT, register_address VARCHAR(255), office_address VARCHAR(255), tax_identification_number VARCHAR(18), customer_type_id BIGINT, bank_name VARCHAR(100), bank_account VARCHAR(50), industry VARCHAR(100), remark TEXT, activity_status VARCHAR(20), last_active_at TIMESTAMP, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE project_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_number VARCHAR(10), project_name VARCHAR(200), customer_id VARCHAR(36), customer_uuid VARCHAR(36), manager_id BIGINT, participant_1_user_id BIGINT, participant_2_user_id BIGINT, participant_3_user_id BIGINT, region_id BIGINT, project_type_id BIGINT, warranty_until DATE, plan_start_time TIMESTAMP, plan_end_time TIMESTAMP, sales_contract_attachment_uuid VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE project_list (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_id VARCHAR(36), list_name VARCHAR(128), list_type VARCHAR(32), entry_date DATE, entry_user BIGINT, auditor_user BIGINT, pdf_attachment_id VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE project_list_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_list_id VARCHAR(36), material_id VARCHAR(36), material_code VARCHAR(12), item_name VARCHAR(255), model VARCHAR(255), brand VARCHAR(128), unit VARCHAR(32), quantity DECIMAL(18,4), unit_price DECIMAL(18,2), total_amount DECIMAL(18,2), source_type VARCHAR(64), remark TEXT, entry_user BIGINT, auditor_user BIGINT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE warehouse_doc (doc_number VARCHAR(50) PRIMARY KEY, doc_type VARCHAR(50), business_category VARCHAR(64), project_id VARCHAR(36), warehouse_id BIGINT, counterparty_customer_id VARCHAR(36), counterparty_name VARCHAR(200), counterparty_address VARCHAR(255), counterparty_contact VARCHAR(255), contract_number VARCHAR(64), doc_date TIMESTAMP, total_amount DECIMAL(18,2), source_ref_type VARCHAR(100), source_ref_id VARCHAR(100), entry_user BIGINT, auditor_user BIGINT, remark TEXT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE warehouse_doc_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), doc_number VARCHAR(50), line_no INT, material_id VARCHAR(36), material_code VARCHAR(12), material_name VARCHAR(200), model VARCHAR(200), display_name VARCHAR(200), display_model VARCHAR(200), brand VARCHAR(128), unit VARCHAR(32), quantity DECIMAL(18,4), unit_price DECIMAL(18,4), amount DECIMAL(18,2), remark TEXT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE goods_transaction_dynamic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), material_id VARCHAR(36), material_code VARCHAR(12), material_name VARCHAR(200), model VARCHAR(200), brand VARCHAR(128), category VARCHAR(64), project_id VARCHAR(36), warehouse_id BIGINT, txn_type VARCHAR(20), txn_time TIMESTAMP, quantity DECIMAL(15,2), unit_price DECIMAL(15,4), amount DECIMAL(15,2), source_ref_type VARCHAR(64), source_ref_id VARCHAR(64), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/CustomerMapper.xml"),
                new ClassPathResource("mapper/ProjectMapper.xml"),
                new ClassPathResource("mapper/ProjectListMapper.xml"),
                new ClassPathResource("mapper/ProjectListItemMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentItemMapper.xml"),
                new ClassPathResource("mapper/InventoryTransactionMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());

        jdbc.update("INSERT INTO sys_user(id, username, real_name, status) VALUES (1, 'mgr', '李工', 'ACTIVE')");
        jdbc.update("INSERT INTO customer_info(id, uuid, customer_code, customer_name, region_id, customer_type_id, activity_status) VALUES (1, 'c1', '110150001', '客户甲', 1, 5, 'ACTIVE')");
        jdbc.update("INSERT INTO project_basic_info(uuid, project_number, project_name, customer_id, customer_uuid, manager_id, region_id, project_type_id) VALUES ('p1', 'AB-2001', '对账项目', 'c1', 'c1', 1, 1, 1)");

        // INITIAL_SALES: m1=10, m2=4; CHANGE: m1=+2 (net sales m1=12); PROCUREMENT: m1=10, m2=4
        jdbc.update("INSERT INTO project_list(uuid, project_id, list_name, list_type) VALUES " +
                "('l1', 'p1', '初始销售', 'INITIAL_SALES'), " +
                "('l2', 'p1', '销售变更', 'CHANGE'), " +
                "('l3', 'p1', '采购清单', 'PROCUREMENT')");
        jdbc.update("INSERT INTO project_list_item(uuid, project_list_id, material_id, material_code, item_name, model, brand, unit, quantity, unit_price, total_amount) VALUES " +
                "('li1', 'l1', 'm1', 'SB0101001400', '信道机', 'M100', '海能达', '台', 10, 100.00, 1000.00), " +
                "('li2', 'l1', 'm2', 'SB0202001150', '对讲机', 'PD660', '海能达', '部', 4, 50.00, 200.00), " +
                "('li3', 'l2', 'm1', 'SB0101001400', '信道机', 'M100', '海能达', '台', 2, 100.00, 200.00), " +
                "('li4', 'l3', 'm1', 'SB0101001400', '信道机', 'M100', '海能达', '台', 12, 80.00, 960.00), " +
                "('li5', 'l3', 'm2', 'SB0202001150', '对讲机', 'PD660', '海能达', '部', 4, 40.00, 160.00)");

        // Docs: INBOUND m1=8, TRANSFER_JTX m1=2 (plus linked WAREHOUSE_TRANSFER auto inbound m1=2 which MUST NOT double count!), OUTBOUND m1=9
        // m2: INBOUND m2=4, OUTBOUND m2=5 (OVER_SHIPPED!)
        jdbc.update("INSERT INTO warehouse_doc(doc_number, doc_type, business_category, project_id, counterparty_name, total_amount, source_ref_type, source_ref_id) VALUES " +
                "('RKD1', 'INBOUND_NOTE', 'PROJECT_PURCHASE', 'p1', '供应商', 800.00, NULL, NULL), " +
                "('HBJ1', 'TRANSFER_JTX', 'WAREHOUSE_TRANSFER_TO_PROJECT', 'p1', 'CK-XMCG', 160.00, NULL, NULL), " +
                "('RKD2', 'INBOUND_NOTE', 'WAREHOUSE_TRANSFER_TO_PROJECT', 'p1', 'CK-JZCG', 160.00, 'WAREHOUSE_TRANSFER', 'HBJ1'), " +
                "('FHD1', 'OUTBOUND_NOTE', 'PROJECT_SALES', 'p1', '客户甲', 1150.00, NULL, NULL)");
        jdbc.update("INSERT INTO warehouse_doc_item(uuid, doc_number, line_no, material_id, material_code, material_name, display_name, display_model, unit, quantity, unit_price, amount) VALUES " +
                "('wdi1', 'RKD1', 1, 'm1', 'SB0101001400', '信道机', '信道机', 'M100', '台', 8, 80.00, 640.00), " +
                "('wdi2', 'RKD1', 2, 'm2', 'SB0202001150', '对讲机', '对讲机', 'PD660', '部', 4, 40.00, 160.00), " +
                "('wdi3', 'HBJ1', 1, 'm1', 'SB0101001400', '信道机', '信道机', 'M100', '台', 2, 80.00, 160.00), " +
                "('wdi4', 'RKD2', 1, 'm1', 'SB0101001400', '信道机', '信道机', 'M100', '台', 2, 80.00, 160.00), " +
                "('wdi5', 'FHD1', 1, 'm1', 'SB0101001400', '信道机', '信道机', 'M100', '台', 9, 100.00, 900.00), " +
                "('wdi6', 'FHD1', 2, 'm2', 'SB0202001150', '对讲机', '对讲机', 'PD660', '部', 5, 50.00, 250.00)");
    }

    private ProjectMaterialReconciliationServiceImpl createService(Set<String> authorities) {
        CurrentUserService currentUserService = new CurrentUserService() {
            @Override public SysUser getCurrentUser() { return null; }
            @Override public Long getCurrentUserId() { return 1L; }
            @Override public Long requireCurrentUserId() { return 1L; }
            @Override public boolean hasAnyAuthority(String... required) {
                return java.util.Arrays.stream(required).anyMatch(authorities::contains);
            }
            @Override public boolean isSystemOrAdmin() { return false; }
        };

        ProjectMaterialReconciliationServiceImpl service = new ProjectMaterialReconciliationServiceImpl();
        ReflectionTestUtils.setField(service, "projectMapper", sql.getMapper(ProjectMapper.class));
        ReflectionTestUtils.setField(service, "customerMapper", sql.getMapper(CustomerMapper.class));
        ReflectionTestUtils.setField(service, "projectListMapper", sql.getMapper(ProjectListMapper.class));
        ReflectionTestUtils.setField(service, "projectListItemMapper", sql.getMapper(ProjectListItemMapper.class));
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", sql.getMapper(WarehouseDocumentMapper.class));
        ReflectionTestUtils.setField(service, "warehouseDocumentItemMapper", sql.getMapper(WarehouseDocumentItemMapper.class));
        ReflectionTestUtils.setField(service, "inventoryTransactionMapper", sql.getMapper(InventoryTransactionMapper.class));
        ReflectionTestUtils.setField(service, "currentUserService", currentUserService);
        return service;
    }

    @Test
    void reconcilesPlannedVsActualWithoutTransferDoubleCounting() {
        ProjectMaterialReconciliationServiceImpl service = createService(Set.of("project.access", "inventory.access"));

        ProjectMaterialReconciliationDto detail = service.getProjectReconciliation("p1");
        assertTrue(detail.isCanViewInventory());
        assertEquals(2, detail.getTotalMaterialLines());
        assertEquals(1, detail.getShortageLineCount());
        assertEquals(1, detail.getOverShippedLineCount());
        assertEquals(new BigDecimal("1400.00"), detail.getPlannedSalesAmount());
        assertEquals(new BigDecimal("1120.00"), detail.getPlannedProcurementAmount());
        assertEquals(new BigDecimal("960.00"), detail.getActualSuppliedAmount());
        assertEquals(new BigDecimal("1150.00"), detail.getActualShippedAmount());

        MaterialReconciliationItemDto m1 = detail.getItems().stream()
                .filter(i -> "m1".equals(i.getMaterialId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("12.0000"), m1.getPlannedSalesQty());
        assertEquals(new BigDecimal("12.0000"), m1.getPlannedProcurementQty());
        assertEquals(new BigDecimal("8.0000"), m1.getPurchasedQty());
        assertEquals(new BigDecimal("2.0000"), m1.getTransferInQty());
        assertEquals(new BigDecimal("10.0000"), m1.getNetSuppliedQty());
        assertEquals(new BigDecimal("2.0000"), m1.getProcurementGapQty());
        assertEquals(new BigDecimal("9.0000"), m1.getShippedQty());
        assertEquals(new BigDecimal("3.0000"), m1.getShipmentGapQty());
        assertEquals("PROCUREMENT_SHORTAGE", m1.getStatus());

        MaterialReconciliationItemDto m2 = detail.getItems().stream()
                .filter(i -> "m2".equals(i.getMaterialId())).findFirst().orElseThrow();
        assertEquals("OVER_SHIPPED", m2.getStatus());

        List<ProjectReconciliationOverviewItemDto> overview = service.getOverview();
        assertEquals(1, overview.size());
        assertEquals(1, overview.get(0).getShortageLineCount());
        assertEquals(1, overview.get(0).getOverShippedLineCount());
    }

    @Test
    void omitsInventoryQueriesWhenUserLacksInventoryPermission() {
        ProjectMaterialReconciliationServiceImpl service = createService(Set.of("project.access"));
        WarehouseDocumentMapper docSpy = mock(WarehouseDocumentMapper.class);
        InventoryTransactionMapper txnSpy = mock(InventoryTransactionMapper.class);
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", docSpy);
        ReflectionTestUtils.setField(service, "inventoryTransactionMapper", txnSpy);

        ProjectMaterialReconciliationDto detail = service.getProjectReconciliation("p1");
        verifyNoInteractions(docSpy, txnSpy);
        assertFalse(detail.isCanViewInventory());
        assertNull(detail.getActualSuppliedAmount());
        assertEquals("NO_INVENTORY_PERMISSION", detail.getItems().get(0).getStatus());
    }
}
