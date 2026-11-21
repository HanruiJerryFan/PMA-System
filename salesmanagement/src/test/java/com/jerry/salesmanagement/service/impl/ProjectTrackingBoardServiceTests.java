package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.ProjectTypeMapper;
import com.jerry.salesmanagement.mapper.RegionMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.mapper.SysUserMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingBoardDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingCardDto;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.SystemConfigService;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectTrackingBoardServiceTests {

    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);

        jdbc.execute("CREATE TABLE sys_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), password VARCHAR(255), real_name VARCHAR(50), status VARCHAR(16), force_password_change BOOLEAN, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE customer_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), customer_code VARCHAR(9), customer_name VARCHAR(100), region_id BIGINT, register_address VARCHAR(255), office_address VARCHAR(255), tax_identification_number VARCHAR(18), customer_type_id BIGINT, bank_name VARCHAR(100), bank_account VARCHAR(50), industry VARCHAR(100), remark TEXT, activity_status VARCHAR(20), last_active_at TIMESTAMP, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE region (id BIGINT PRIMARY KEY, `level` TINYINT, parent_code BIGINT, area_code BIGINT, zip_code VARCHAR(6), city_code VARCHAR(6), name VARCHAR(50), short_name VARCHAR(50), merger_name VARCHAR(50), pinyin VARCHAR(30), lng DECIMAL(10,6), lat DECIMAL(10,6))");
        jdbc.execute("CREATE TABLE project_type (id BIGINT PRIMARY KEY, code VARCHAR(32), name VARCHAR(64), is_active BOOLEAN, type_name VARCHAR(100), type_desc VARCHAR(100))");
        jdbc.execute("CREATE TABLE stage (id BIGINT PRIMARY KEY, stage_code VARCHAR(16), description VARCHAR(128), sort_order INT, is_active BOOLEAN)");
        jdbc.execute("CREATE TABLE project_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_number VARCHAR(10), project_name VARCHAR(200), customer_id VARCHAR(36), customer_uuid VARCHAR(36), manager_id BIGINT, participant_1_user_id BIGINT, participant_2_user_id BIGINT, participant_3_user_id BIGINT, region_id BIGINT, project_type_id BIGINT, warranty_until DATE, plan_start_time TIMESTAMP, plan_end_time TIMESTAMP, sales_contract_attachment_uuid VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE project_status_history (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(64), project_id VARCHAR(64), stage_id BIGINT, created_at TIMESTAMP, created_by BIGINT)");
        jdbc.execute("CREATE TABLE project_list (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_id VARCHAR(36), list_name VARCHAR(128), list_type VARCHAR(32), entry_date DATE, entry_user BIGINT, auditor_user BIGINT, pdf_attachment_id VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE contract_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_basic_info_id VARCHAR(36), sub_item_no VARCHAR(2), contract_type_id BIGINT, sub_item_content TEXT, contract_number VARCHAR(50), client_id VARCHAR(36), contact_id VARCHAR(36), sign_date DATE, contract_amount DECIMAL(15,2), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE finance_subject (id BIGINT PRIMARY KEY, subject_level TINYINT, subject_code VARCHAR(64), subject_name VARCHAR(64), sort_order INT)");
        jdbc.execute("CREATE TABLE finance_voucher (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), voucher_no VARCHAR(10), occurred_on DATE, project_id VARCHAR(36), level_1_subject_id BIGINT, level_2_subject_id BIGINT, summary VARCHAR(500), transaction_direction VARCHAR(16), tax_rate DECIMAL(5,4), counterparty_customer_id VARCHAR(36), counterparty_name_snapshot VARCHAR(255), invoice_status VARCHAR(32), invoice_type VARCHAR(32), invoice_no VARCHAR(128), invoice_amount DECIMAL(18,2), actual_income_amount DECIMAL(18,2), actual_expense_amount DECIMAL(18,2), booked_amount DECIMAL(18,2), is_completed BOOLEAN, entry_user BIGINT, auditor_user BIGINT, remark TEXT, created_at TIMESTAMP, created_by BIGINT, updated_at TIMESTAMP, updated_by BIGINT)");
        jdbc.execute("CREATE TABLE warehouse_doc (doc_number VARCHAR(50) PRIMARY KEY, doc_type VARCHAR(50), business_category VARCHAR(64), project_id VARCHAR(36), warehouse_id BIGINT, counterparty_customer_id VARCHAR(36), counterparty_name VARCHAR(200), counterparty_address VARCHAR(255), counterparty_contact VARCHAR(255), contract_number VARCHAR(64), doc_date TIMESTAMP, total_amount DECIMAL(18,2), source_ref_type VARCHAR(100), source_ref_id VARCHAR(100), entry_user BIGINT, auditor_user BIGINT, remark TEXT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/SysUserMapper.xml"),
                new ClassPathResource("mapper/CustomerMapper.xml"),
                new ClassPathResource("mapper/RegionMapper.xml"),
                new ClassPathResource("mapper/ProjectTypeMapper.xml"),
                new ClassPathResource("mapper/StageMapper.xml"),
                new ClassPathResource("mapper/ProjectMapper.xml"),
                new ClassPathResource("mapper/ProjectStatusHistoryMapper.xml"),
                new ClassPathResource("mapper/ProjectListMapper.xml"),
                new ClassPathResource("mapper/ContractBasicInfoMapper.xml"),
                new ClassPathResource("mapper/FinanceVoucherMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());

        seedBaseData();
    }

    private void seedBaseData() {
        jdbc.update("INSERT INTO sys_user(id, username, real_name, status) VALUES (1, 'mgr', '张经理', 'ACTIVE')");
        jdbc.update("INSERT INTO customer_info(id, uuid, customer_code, customer_name, region_id, customer_type_id, activity_status) VALUES (1, 'c1', '110150001', '华东客户A', 1, 5, 'ACTIVE')");
        jdbc.update("INSERT INTO region(id, `level`, parent_code, area_code, name, merger_name) VALUES (1, 2, 110000000000, 110100000000, '北京市', '北京')");
        jdbc.update("INSERT INTO project_type(id, code, name, is_active, type_name, type_desc) VALUES (1, 'XS', '产品销售', true, 'XS', '产品销售')");
        jdbc.update("INSERT INTO stage(id, stage_code, description, sort_order, is_active) VALUES (1, '1.1', '发现', 1, true), (2, '3.3', '执行中-供货', 9, true), (3, '4.2', '执行完毕', 14, true)");

        jdbc.update("INSERT INTO finance_subject(id, subject_level, subject_code, subject_name, sort_order) VALUES (1, 1, 'PROJECT', '项目', 1), (2, 2, 'PROJECT_RECEIPT', '项目回款', 1), (3, 2, 'EQUIPMENT_PURCHASE', '设备采购', 2)");

        // p1: overdue + stalled (entered stage 2 45 days ago before 2026-06-15) + warranty expiring in 10 days + low collection + pending audit
        jdbc.update("INSERT INTO project_basic_info(uuid, project_number, project_name, customer_id, customer_uuid, manager_id, region_id, project_type_id, warranty_until, plan_start_time, plan_end_time) " +
                "VALUES ('p1', 'AB-1001', '一号工程', 'c1', 'c1', 1, 1, 1, '2026-06-25', '2026-01-01 00:00:00', '2026-06-01 00:00:00'), " +
                "('p2', 'AB-1002', '二号工程', 'c1', 'c1', 1, 1, 1, '2026-05-01', '2025-10-01 00:00:00', '2026-04-01 00:00:00')");

        jdbc.update("INSERT INTO project_status_history(uuid, project_id, stage_id, created_at, created_by) VALUES " +
                "('h1', 'p1', 1, '2026-04-01 10:00:00', 1), ('h2', 'p1', 2, '2026-05-01 10:00:00', 1), ('h3', 'p2', 3, '2026-05-20 10:00:00', 1)");

        jdbc.update("INSERT INTO project_list(uuid, project_id, list_name, list_type, entry_user, auditor_user) VALUES " +
                "('pl1', 'p1', '初始清单', 'INITIAL_SALES', 1, NULL)");

        jdbc.update("INSERT INTO contract_basic_info(uuid, project_basic_info_id, sub_item_no, contract_type_id, contract_number, client_id, contract_amount) VALUES " +
                "('ct1', 'p1', '01', 1, 'HT-001', 'c1', 1000.00)");

        jdbc.update("INSERT INTO finance_voucher(uuid, voucher_no, occurred_on, project_id, level_1_subject_id, level_2_subject_id, summary, transaction_direction, tax_rate, counterparty_customer_id, counterparty_name_snapshot, invoice_status, invoice_amount, actual_income_amount, actual_expense_amount, booked_amount, is_completed, entry_user, auditor_user) VALUES " +
                "('fv1', '2026060101', '2026-06-01', 'p1', 1, 2, '首付款', 'RECEIVE', 0.1300, 'c1', '华东客户A', 'INVOICED', 500.00, 200.00, NULL, 200.00, true, 1, NULL), " +
                "('fv2', '2026060201', '2026-06-02', 'p1', 1, 3, '采购支出', 'PAY', 0.1300, 'c1', '华东客户A', 'NOT_INVOICED', NULL, NULL, 250.00, 250.00, true, 1, 1)");

        // Include both a normal outbound doc and a WAREHOUSE_TRANSFER auto inbound doc (which must be excluded from pending audit count)
        jdbc.update("INSERT INTO warehouse_doc(doc_number, doc_type, business_category, project_id, counterparty_name, total_amount, source_ref_type, source_ref_id, entry_user, auditor_user) VALUES " +
                "('FHD2026001', 'OUTBOUND_NOTE', 'PROJECT_SALES', 'p1', '华东客户A', 400.00, NULL, NULL, 1, NULL), " +
                "('RKD2026002', 'INBOUND_NOTE', 'WAREHOUSE_TRANSFER_TO_PROJECT', 'p1', 'CK-JZCG', 100.00, 'WAREHOUSE_TRANSFER', 'HBJ2026001', 1, NULL)");
    }

    private ProjectTrackingBoardServiceImpl createService(Set<String> authorities) {
        CurrentUserService currentUserService = new CurrentUserService() {
            @Override public SysUser getCurrentUser() { return null; }
            @Override public Long getCurrentUserId() { return 1L; }
            @Override public Long requireCurrentUserId() { return 1L; }
            @Override public boolean hasAnyAuthority(String... required) {
                return java.util.Arrays.stream(required).anyMatch(authorities::contains);
            }
            @Override public boolean isSystemOrAdmin() { return false; }
        };

        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.getInt("project.tracking.stalled_days", 30)).thenReturn(30);
        when(configService.getInt("project.delete.lock_stage_sort_order", 5)).thenReturn(5);

        ProjectTrackingBoardServiceImpl service = new ProjectTrackingBoardServiceImpl();
        ReflectionTestUtils.setField(service, "projectMapper", sql.getMapper(ProjectMapper.class));
        ReflectionTestUtils.setField(service, "stageMapper", sql.getMapper(StageMapper.class));
        ReflectionTestUtils.setField(service, "projectStatusHistoryMapper", sql.getMapper(ProjectStatusHistoryMapper.class));
        ReflectionTestUtils.setField(service, "customerMapper", sql.getMapper(CustomerMapper.class));
        ReflectionTestUtils.setField(service, "sysUserMapper", sql.getMapper(SysUserMapper.class));
        ReflectionTestUtils.setField(service, "regionMapper", sql.getMapper(RegionMapper.class));
        ReflectionTestUtils.setField(service, "projectTypeMapper", sql.getMapper(ProjectTypeMapper.class));
        ReflectionTestUtils.setField(service, "projectListMapper", sql.getMapper(ProjectListMapper.class));
        ReflectionTestUtils.setField(service, "contractBasicInfoMapper", sql.getMapper(ContractBasicInfoMapper.class));
        ReflectionTestUtils.setField(service, "financeVoucherMapper", sql.getMapper(FinanceVoucherMapper.class));
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", sql.getMapper(WarehouseDocumentMapper.class));
        ReflectionTestUtils.setField(service, "currentUserService", currentUserService);
        ReflectionTestUtils.setField(service, "systemConfigService", configService);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("UTC")));
        return service;
    }

    @Test
    void computesStageHealthFinanceAndRisksForAuthorizedUser() {
        ProjectTrackingBoardServiceImpl service = createService(Set.of(
                "project.access", "project.manage", "contract.access", "finance.access", "inventory.access"
        ));

        ProjectTrackingBoardDto board = service.getTrackingBoard(null, null, null, null);
        assertEquals(2, board.getSummary().getTotalProjects());
        assertEquals(1, board.getSummary().getActiveProjects());
        assertEquals(1, board.getSummary().getCompletedProjects());
        assertEquals(1, board.getSummary().getOverdueCount());
        assertEquals(1, board.getSummary().getStalledCount());
        assertEquals(1, board.getSummary().getWarrantyExpiringCount());
        assertEquals(1, board.getSummary().getWarrantyExpiredCount());

        ProjectTrackingCardDto p1 = board.getItems().stream()
                .filter(c -> "p1".equals(c.getUuid())).findFirst().orElseThrow();
        assertEquals("3.3", p1.getCurrentStageCode());
        assertEquals(45L, p1.getStageStayDays());
        assertEquals(3L, p1.getNextStageId());
        assertTrue(p1.isOverdue());
        assertEquals("EXPIRING_SOON", p1.getWarrantyStatus());
        assertEquals(new BigDecimal("1000.00"), p1.getContractAmount());
        assertEquals(new BigDecimal("200.00"), p1.getReceivedAmount());
        assertEquals(new BigDecimal("300.00"), p1.getReceivableAmount());
        assertEquals(new BigDecimal("20.00"), p1.getCollectionRate());
        assertEquals(new BigDecimal("-50.00"), p1.getEstimatedGrossProfit());
        assertEquals(new BigDecimal("400.00"), p1.getOutboundAmount());
        // pendingListCount(1) + pendingWarehouseDocCount(1, auto inbound excluded) + pendingFinanceVoucherCount(1) = 3
        assertEquals(1, p1.getPendingListCount());
        assertEquals(1, p1.getPendingWarehouseDocCount());
        assertEquals(1, p1.getPendingFinanceVoucherCount());
        assertEquals(3, p1.getTotalPendingAuditCount());
        assertTrue(p1.getRiskTags().contains("OVERDUE"));
        assertTrue(p1.getRiskTags().contains("STALLED"));
        assertTrue(p1.getRiskTags().contains("WARRANTY_EXPIRING"));
        assertTrue(p1.getRiskTags().contains("LOW_COLLECTION"));
        assertTrue(p1.getRiskTags().contains("NEGATIVE_MARGIN"));
        assertTrue(p1.getRiskTags().contains("PENDING_AUDIT"));

        ProjectTrackingCardDto p2 = board.getItems().stream()
                .filter(c -> "p2".equals(c.getUuid())).findFirst().orElseThrow();
        assertTrue(p2.isCompleted());
        assertFalse(p2.isOverdue());
        assertEquals("EXPIRED", p2.getWarrantyStatus());
    }

    @Test
    void hidesContractFinanceAndInventoryDataWhenUserOnlyHasProjectAccess() {
        ProjectTrackingBoardServiceImpl service = createService(Set.of("project.access"));
        ContractBasicInfoMapper contractSpy = mock(ContractBasicInfoMapper.class);
        FinanceVoucherMapper voucherSpy = mock(FinanceVoucherMapper.class);
        WarehouseDocumentMapper warehouseSpy = mock(WarehouseDocumentMapper.class);
        ReflectionTestUtils.setField(service, "contractBasicInfoMapper", contractSpy);
        ReflectionTestUtils.setField(service, "financeVoucherMapper", voucherSpy);
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", warehouseSpy);

        ProjectTrackingBoardDto board = service.getTrackingBoard(null, null, null, null);
        verifyNoInteractions(contractSpy, voucherSpy, warehouseSpy);
        assertNull(board.getSummary().getTotalContractAmount());
        assertNull(board.getSummary().getTotalReceivedAmount());

        ProjectTrackingCardDto p1 = board.getItems().stream()
                .filter(c -> "p1".equals(c.getUuid())).findFirst().orElseThrow();
        assertNull(p1.getContractAmount());
        assertNull(p1.getReceivedAmount());
        assertNull(p1.getOutboundAmount());
        assertNull(p1.getPendingFinanceVoucherCount());
        assertEquals(1, p1.getTotalPendingAuditCount());
    }
}
