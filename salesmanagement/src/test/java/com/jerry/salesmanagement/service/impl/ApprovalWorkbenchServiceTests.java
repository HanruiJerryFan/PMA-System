package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalSummaryDto;
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

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApprovalWorkbenchServiceTests {

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
        jdbc.execute("CREATE TABLE warehouse_doc (doc_number VARCHAR(50) PRIMARY KEY, doc_type VARCHAR(50), business_category VARCHAR(64), project_id VARCHAR(36), warehouse_id BIGINT, counterparty_customer_id VARCHAR(36), counterparty_name VARCHAR(200), counterparty_address VARCHAR(255), counterparty_contact VARCHAR(255), contract_number VARCHAR(64), doc_date TIMESTAMP, total_amount DECIMAL(18,2), source_ref_type VARCHAR(100), source_ref_id VARCHAR(100), entry_user BIGINT, auditor_user BIGINT, remark TEXT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE finance_subject (id BIGINT PRIMARY KEY, subject_level TINYINT, subject_code VARCHAR(64), subject_name VARCHAR(64), sort_order INT)");
        jdbc.execute("CREATE TABLE finance_voucher (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), voucher_no VARCHAR(10), occurred_on DATE, project_id VARCHAR(36), level_1_subject_id BIGINT, level_2_subject_id BIGINT, summary VARCHAR(500), transaction_direction VARCHAR(16), tax_rate DECIMAL(5,4), counterparty_customer_id VARCHAR(36), counterparty_name_snapshot VARCHAR(255), invoice_status VARCHAR(32), invoice_type VARCHAR(32), invoice_no VARCHAR(128), invoice_amount DECIMAL(18,2), actual_income_amount DECIMAL(18,2), actual_expense_amount DECIMAL(18,2), booked_amount DECIMAL(18,2), is_completed BOOLEAN, entry_user BIGINT, auditor_user BIGINT, remark TEXT, created_at TIMESTAMP, created_by BIGINT, updated_at TIMESTAMP, updated_by BIGINT)");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/ProjectMapper.xml"),
                new ClassPathResource("mapper/ProjectListMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentMapper.xml"),
                new ClassPathResource("mapper/FinanceVoucherMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());

        jdbc.update("INSERT INTO sys_user(id, username, real_name, status) VALUES (1, 'op', '录入员', 'ACTIVE'), (2, 'aud', '审核员', 'ACTIVE')");
        jdbc.update("INSERT INTO customer_info(id, uuid, customer_code, customer_name, region_id, customer_type_id, activity_status) VALUES (1, 'c1', '110150001', '客户甲', 1, 5, 'ACTIVE')");
        jdbc.update("INSERT INTO project_basic_info(uuid, project_number, project_name, customer_id, customer_uuid, manager_id, region_id, project_type_id) VALUES ('p1', 'AB-1001', '一号项目', 'c1', 'c1', 1, 1, 1)");
        jdbc.update("INSERT INTO finance_subject(id, subject_level, subject_code, subject_name, sort_order) VALUES (1, 1, 'PROJECT', '项目', 1), (2, 2, 'PROJECT_RECEIPT', '项目回款', 1)");

        jdbc.update("INSERT INTO project_list(uuid, project_id, list_name, list_type, entry_date, entry_user, auditor_user) VALUES " +
                "('pl-pending', 'p1', '待审清单', 'INITIAL_SALES', '2026-06-01', 1, NULL), " +
                "('pl-done', 'p1', '已审清单', 'PROCUREMENT', '2026-06-01', 1, 2)");

        jdbc.update("INSERT INTO warehouse_doc(doc_number, doc_type, business_category, project_id, counterparty_name, doc_date, total_amount, source_ref_type, source_ref_id, entry_user, auditor_user) VALUES " +
                "('HBJ2026001', 'TRANSFER_JTX', 'WAREHOUSE_TRANSFER_TO_PROJECT', 'p1', 'CK-XMCG', '2026-06-02 00:00:00', 500.00, NULL, NULL, 1, NULL), " +
                "('RKD2026001', 'INBOUND_NOTE', 'WAREHOUSE_TRANSFER_TO_PROJECT', 'p1', 'CK-JZCG', '2026-06-02 00:00:00', 500.00, 'WAREHOUSE_TRANSFER', 'HBJ2026001', 1, NULL), " +
                "('RKD2026002', 'INBOUND_NOTE', 'PROJECT_PURCHASE', 'p1', '供应商', '2026-06-02 00:00:00', 300.00, NULL, NULL, 1, 2)");

        jdbc.update("INSERT INTO finance_voucher(uuid, voucher_no, occurred_on, project_id, level_1_subject_id, level_2_subject_id, summary, transaction_direction, tax_rate, counterparty_customer_id, counterparty_name_snapshot, invoice_status, actual_income_amount, booked_amount, is_completed, entry_user, auditor_user) VALUES " +
                "('fv-pending', '2026060301', '2026-06-03', 'p1', 1, 2, '待审回款', 'RECEIVE', 0.1300, 'c1', '客户甲', 'INVOICED', 800.00, 800.00, true, 1, NULL), " +
                "('fv-done', '2026060302', '2026-06-03', 'p1', 1, 2, '已审回款', 'RECEIVE', 0.1300, 'c1', '客户甲', 'INVOICED', 100.00, 100.00, true, 1, 2)");
    }

    private ApprovalWorkbenchServiceImpl createService(Set<String> authorities) {
        CurrentUserService currentUserService = new CurrentUserService() {
            @Override public SysUser getCurrentUser() { return null; }
            @Override public Long getCurrentUserId() { return 1L; }
            @Override public Long requireCurrentUserId() { return 1L; }
            @Override public boolean hasAnyAuthority(String... required) {
                return java.util.Arrays.stream(required).anyMatch(authorities::contains);
            }
            @Override public boolean isSystemOrAdmin() { return false; }
        };

        ApprovalWorkbenchServiceImpl service = new ApprovalWorkbenchServiceImpl();
        ReflectionTestUtils.setField(service, "projectMapper", sql.getMapper(ProjectMapper.class));
        ReflectionTestUtils.setField(service, "projectListMapper", sql.getMapper(ProjectListMapper.class));
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", sql.getMapper(WarehouseDocumentMapper.class));
        ReflectionTestUtils.setField(service, "financeVoucherMapper", sql.getMapper(FinanceVoucherMapper.class));
        ReflectionTestUtils.setField(service, "currentUserService", currentUserService);
        return service;
    }

    @Test
    void aggregatesPendingApprovalsAndExcludesAutoGeneratedTransferInboundDocs() {
        ApprovalWorkbenchServiceImpl service = createService(Set.of(
                "project.list.audit", "inventory.warehouse-doc.audit", "finance.voucher.audit"
        ));

        PendingApprovalSummaryDto summary = service.getPendingApprovals();
        assertEquals(3, summary.getTotalPendingCount());
        assertEquals(1, summary.getProjectListCount());
        assertEquals(1, summary.getWarehouseDocCount());
        assertEquals(1, summary.getFinanceVoucherCount());
        assertEquals("pl-pending", summary.getProjectLists().get(0).getId());
        assertEquals("HBJ2026001", summary.getWarehouseDocs().get(0).getId());
        assertEquals("fv-pending", summary.getFinanceVouchers().get(0).getId());
    }

    @Test
    void respectsPerModuleAuditPermissions() {
        ApprovalWorkbenchServiceImpl service = createService(Set.of("finance.voucher.audit"));
        ProjectListMapper listSpy = mock(ProjectListMapper.class);
        WarehouseDocumentMapper docSpy = mock(WarehouseDocumentMapper.class);
        ReflectionTestUtils.setField(service, "projectListMapper", listSpy);
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", docSpy);

        PendingApprovalSummaryDto summary = service.getPendingApprovals();
        verifyNoInteractions(listSpy, docSpy);
        assertFalse(summary.isCanAuditProjectLists());
        assertFalse(summary.isCanAuditWarehouseDocs());
        assertTrue(summary.isCanAuditFinanceVouchers());
        assertEquals(1, summary.getTotalPendingCount());
        assertEquals(0, summary.getProjectListCount());
        assertEquals(0, summary.getWarehouseDocCount());
        assertEquals(1, summary.getFinanceVoucherCount());
    }
}
