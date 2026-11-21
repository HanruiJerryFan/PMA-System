package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.FinanceSubjectMapper;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.FinanceAnalyticsOverviewDto;
import com.jerry.salesmanagement.pojo.dto.ProjectProfitRowDto;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FinanceAnalyticsServiceTests {

    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);

        jdbc.execute("CREATE TABLE sys_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), password VARCHAR(255), real_name VARCHAR(50), status VARCHAR(16), force_password_change BOOLEAN, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE customer_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), customer_code VARCHAR(9), customer_name VARCHAR(100), region_id BIGINT, register_address VARCHAR(255), office_address VARCHAR(255), tax_identification_number VARCHAR(18), customer_type_id BIGINT, bank_name VARCHAR(100), bank_account VARCHAR(50), industry VARCHAR(100), remark TEXT, activity_status VARCHAR(20), last_active_at TIMESTAMP, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE stage (id BIGINT PRIMARY KEY, stage_code VARCHAR(16), description VARCHAR(128), sort_order INT, is_active BOOLEAN)");
        jdbc.execute("CREATE TABLE project_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_number VARCHAR(10), project_name VARCHAR(200), customer_id VARCHAR(36), customer_uuid VARCHAR(36), manager_id BIGINT, participant_1_user_id BIGINT, participant_2_user_id BIGINT, participant_3_user_id BIGINT, region_id BIGINT, project_type_id BIGINT, warranty_until DATE, plan_start_time TIMESTAMP, plan_end_time TIMESTAMP, sales_contract_attachment_uuid VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE project_status_history (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(64), project_id VARCHAR(64), stage_id BIGINT, created_at TIMESTAMP, created_by BIGINT)");
        jdbc.execute("CREATE TABLE contract_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_basic_info_id VARCHAR(36), sub_item_no VARCHAR(2), contract_type_id BIGINT, sub_item_content TEXT, contract_number VARCHAR(50), client_id VARCHAR(36), contact_id VARCHAR(36), sign_date DATE, contract_amount DECIMAL(15,2), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE finance_subject (id BIGINT PRIMARY KEY, subject_level TINYINT, subject_code VARCHAR(64), subject_name VARCHAR(64), sort_order INT)");
        jdbc.execute("CREATE TABLE finance_voucher (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), voucher_no VARCHAR(10), occurred_on DATE, project_id VARCHAR(36), level_1_subject_id BIGINT, level_2_subject_id BIGINT, summary VARCHAR(500), transaction_direction VARCHAR(16), tax_rate DECIMAL(5,4), counterparty_customer_id VARCHAR(36), counterparty_name_snapshot VARCHAR(255), invoice_status VARCHAR(32), invoice_type VARCHAR(32), invoice_no VARCHAR(128), invoice_amount DECIMAL(18,2), actual_income_amount DECIMAL(18,2), actual_expense_amount DECIMAL(18,2), booked_amount DECIMAL(18,2), is_completed BOOLEAN, entry_user BIGINT, auditor_user BIGINT, remark TEXT, created_at TIMESTAMP, created_by BIGINT, updated_at TIMESTAMP, updated_by BIGINT)");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/CustomerMapper.xml"),
                new ClassPathResource("mapper/StageMapper.xml"),
                new ClassPathResource("mapper/ProjectMapper.xml"),
                new ClassPathResource("mapper/ProjectStatusHistoryMapper.xml"),
                new ClassPathResource("mapper/ContractBasicInfoMapper.xml"),
                new ClassPathResource("mapper/FinanceSubjectMapper.xml"),
                new ClassPathResource("mapper/FinanceVoucherMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());

        jdbc.update("INSERT INTO sys_user(id, username, real_name, status) VALUES (1, 'fin', '财务员', 'ACTIVE')");
        jdbc.update("INSERT INTO customer_info(id, uuid, customer_code, customer_name, region_id, customer_type_id, activity_status) VALUES (1, 'c1', '110150001', '客户A', 1, 5, 'ACTIVE'), (2, 'c2', '110140001', '供应商B', 1, 4, 'ACTIVE')");
        jdbc.update("INSERT INTO stage(id, stage_code, description, sort_order, is_active) VALUES (1, '3.6', '执行中-验收', 12, true)");
        jdbc.update("INSERT INTO project_basic_info(uuid, project_number, project_name, customer_id, customer_uuid, manager_id, region_id, project_type_id) VALUES ('p1', 'AB-3001', '利润分析项目', 'c1', 'c1', 1, 1, 1)");
        jdbc.update("INSERT INTO project_status_history(uuid, project_id, stage_id, created_at, created_by) VALUES ('h1', 'p1', 1, '2026-06-01 10:00:00', 1)");
        jdbc.update("INSERT INTO contract_basic_info(uuid, project_basic_info_id, sub_item_no, contract_type_id, contract_number, client_id, contract_amount) VALUES ('ct1', 'p1', '01', 1, 'HT-3001', 'c1', 2000.00)");

        jdbc.update("INSERT INTO finance_subject(id, subject_level, subject_code, subject_name, sort_order) VALUES " +
                "(1, 1, 'PROJECT', '项目', 1), (2, 1, 'NON_PROJECT', '非项目', 2), " +
                "(3, 2, 'PROJECT_RECEIPT', '项目回款', 1), (4, 2, 'EQUIPMENT_PURCHASE', '设备采购', 2), " +
                "(5, 2, 'PROJECT_TRANSFER_OUT', '项目调出', 3), (6, 2, 'TRAVEL', '差旅', 4)");

        // Reference date: 2026-06-15
        // fv1: completed RECEIVE 800, invoiced 1200, occurred 2026-06-01 (14 days -> 0-30 bucket RECEIVABLE = 400)
        // fv2: uncompleted RECEIVE 300, occurred 2026-03-01 (106 days -> >90 bucket RECEIVABLE = 300)
        // fv3: completed PAY 500, booked 500, invoiced 650, occurred 2026-05-01 (45 days -> 31-60 bucket PAYABLE = 150)
        // fv4: completed PAY PROJECT_TRANSFER_OUT 100, booked -100.00 (cost offset)
        // fv5: NON_PROJECT completed PAY TRAVEL 200, booked 229.89
        jdbc.update("INSERT INTO finance_voucher(uuid, voucher_no, occurred_on, project_id, level_1_subject_id, level_2_subject_id, summary, transaction_direction, tax_rate, counterparty_customer_id, counterparty_name_snapshot, invoice_status, invoice_amount, actual_income_amount, actual_expense_amount, booked_amount, is_completed) VALUES " +
                "('fv1', '2026060101', '2026-06-01', 'p1', 1, 3, '一期回款', 'RECEIVE', 0.1300, 'c1', '客户A', 'INVOICED', 1200.00, 800.00, NULL, 800.00, true), " +
                "('fv2', '2026030101', '2026-03-01', 'p1', 1, 3, '待收尾款', 'RECEIVE', 0.1300, 'c1', '客户A', 'NOT_INVOICED', NULL, 300.00, NULL, 300.00, false), " +
                "('fv3', '2026050101', '2026-05-01', 'p1', 1, 4, '设备采购', 'PAY', 0.1300, 'c2', '供应商B', 'INVOICED', 650.00, NULL, 500.00, 500.00, true), " +
                "('fv4', '2026060501', '2026-06-05', 'p1', 1, 5, '项目调出', 'PAY', 0.1300, 'c2', '供应商B', 'NOT_INVOICED', NULL, NULL, 100.00, -100.00, true), " +
                "('fv5', '2026061001', '2026-06-10', 'p1', 2, 6, '差旅报销', 'PAY', 0.0000, 'c2', '供应商B', 'NOT_INVOICED', NULL, NULL, 200.00, 229.89, true)");
    }

    @Test
    void computesProjectProfitNonProjectTotalsAndAgingBuckets() {
        CurrentUserService currentUserService = new CurrentUserService() {
            @Override public SysUser getCurrentUser() { return null; }
            @Override public Long getCurrentUserId() { return 1L; }
            @Override public Long requireCurrentUserId() { return 1L; }
            @Override public boolean hasAnyAuthority(String... required) {
                Set<String> auth = Set.of("finance.access", "contract.access");
                return java.util.Arrays.stream(required).anyMatch(auth::contains);
            }
            @Override public boolean isSystemOrAdmin() { return false; }
        };

        FinanceAnalyticsServiceImpl service = new FinanceAnalyticsServiceImpl();
        ReflectionTestUtils.setField(service, "financeVoucherMapper", sql.getMapper(FinanceVoucherMapper.class));
        ReflectionTestUtils.setField(service, "financeSubjectMapper", sql.getMapper(FinanceSubjectMapper.class));
        ReflectionTestUtils.setField(service, "projectMapper", sql.getMapper(ProjectMapper.class));
        ReflectionTestUtils.setField(service, "customerMapper", sql.getMapper(CustomerMapper.class));
        ReflectionTestUtils.setField(service, "stageMapper", sql.getMapper(StageMapper.class));
        ReflectionTestUtils.setField(service, "projectStatusHistoryMapper", sql.getMapper(ProjectStatusHistoryMapper.class));
        ReflectionTestUtils.setField(service, "contractBasicInfoMapper", sql.getMapper(ContractBasicInfoMapper.class));
        ReflectionTestUtils.setField(service, "currentUserService", currentUserService);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("UTC")));

        FinanceAnalyticsOverviewDto overview = service.getOverview();
        assertTrue(overview.isCanViewContracts());
        assertEquals(new BigDecimal("2000.00"), overview.getTotalContractAmount());
        assertEquals(new BigDecimal("800.00"), overview.getTotalActualIncome());
        assertEquals(new BigDecimal("800.00"), overview.getTotalActualExpense());
        assertEquals(new BigDecimal("629.89"), overview.getTotalBookedExpense());
        assertEquals(new BigDecimal("200.00"), overview.getNonProjectExpense());
        assertEquals(new BigDecimal("700.00"), overview.getTotalReceivable());
        assertEquals(new BigDecimal("150.00"), overview.getTotalPayable());

        assertEquals(new BigDecimal("400.00"), overview.getReceivableAging().getBucket0To30());
        assertEquals(new BigDecimal("300.00"), overview.getReceivableAging().getBucketOver90());
        assertEquals(new BigDecimal("150.00"), overview.getPayableAging().getBucket31To60());

        ProjectProfitRowDto p1 = overview.getProjectProfits().get(0);
        assertEquals(new BigDecimal("800.00"), p1.getActualIncomeAmount());
        assertEquals(new BigDecimal("600.00"), p1.getActualExpenseAmount());
        assertEquals(new BigDecimal("400.00"), p1.getBookedExpenseAmount());
        assertEquals(new BigDecimal("200.00"), p1.getGrossProfit());
        assertEquals(new BigDecimal("400.00"), p1.getBookedNetProfit());
        assertEquals(new BigDecimal("700.00"), p1.getReceivableAmount());
        assertEquals(new BigDecimal("150.00"), p1.getPayableAmount());
        assertEquals(4, p1.getVoucherCount());
        assertEquals(1, p1.getUncompletedVoucherCount());
    }
}
