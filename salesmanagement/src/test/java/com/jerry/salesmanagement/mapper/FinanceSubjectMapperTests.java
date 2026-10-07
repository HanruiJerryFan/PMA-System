package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.FinanceSubject;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FinanceSubjectMapperTests {
    @Test
    void voucherJoinsFollowSubjectIdsAfterRenameAndForeignKeysProtectReferences() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE finance_subject (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, subject_level TINYINT NOT NULL,
                  subject_code VARCHAR(64) NOT NULL, subject_name VARCHAR(64) NOT NULL, sort_order INT NOT NULL,
                  UNIQUE(subject_level, subject_code), UNIQUE(subject_level, subject_name)
                )
                """);
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, real_name VARCHAR(64), username VARCHAR(64))");
        jdbc.execute("""
                CREATE TABLE finance_voucher (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), voucher_no VARCHAR(10), occurred_on DATE,
                  project_id VARCHAR(36), level_1_subject_id BIGINT NOT NULL REFERENCES finance_subject(id),
                  level_2_subject_id BIGINT NOT NULL REFERENCES finance_subject(id), summary VARCHAR(500),
                  transaction_direction VARCHAR(16), tax_rate DECIMAL(5,4), counterparty_customer_id VARCHAR(36),
                  counterparty_name_snapshot VARCHAR(255), invoice_status VARCHAR(32), invoice_type VARCHAR(32),
                  invoice_no VARCHAR(128), invoice_amount DECIMAL(18,2), actual_income_amount DECIMAL(18,2),
                  actual_expense_amount DECIMAL(18,2), booked_amount DECIMAL(18,2), is_completed BOOLEAN,
                  entry_user BIGINT, auditor_user BIGINT, remark TEXT, created_at TIMESTAMP, created_by BIGINT,
                  updated_at TIMESTAMP, updated_by BIGINT
                )
                """);
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        for (String resource : new String[] {"mapper/FinanceSubjectMapper.xml", "mapper/FinanceVoucherMapper.xml"}) {
            try (InputStream input = new ClassPathResource(resource).getInputStream()) {
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession session = factory.openSession(true)) {
            FinanceSubjectMapper subjects = session.getMapper(FinanceSubjectMapper.class);
            FinanceVoucherMapper vouchers = session.getMapper(FinanceVoucherMapper.class);
            FinanceSubject first = subject(1, "PROJECT", "项目");
            FinanceSubject second = subject(2, "SALARY", "人员工资");
            subjects.insert(first);
            subjects.insert(second);
            assertNotNull(first.getId());
            assertEquals(second.getId(), subjects.selectByCode(2, "SALARY").getId());
            assertEquals(second.getId(), subjects.selectByName(2, "人员工资").getId());

            FinanceVoucher voucher = new FinanceVoucher();
            voucher.setUuid(UUID.randomUUID().toString());
            voucher.setVoucherNo("2026100601");
            voucher.setOccurredOn(LocalDate.of(2026, 10, 6));
            voucher.setLevel1SubjectId(first.getId());
            voucher.setLevel2SubjectId(second.getId());
            voucher.setBookedAmount(new BigDecimal("100.00"));
            vouchers.insert(voucher);
            assertNotNull(voucher.getId());
            assertEquals(second.getId(), vouchers.selectByUuid(voucher.getUuid()).getLevel2SubjectId());

            second.setSubjectName("工资支出");
            subjects.update(second);
            FinanceVoucher loaded = vouchers.selectByUuid(voucher.getUuid());
            assertEquals("项目", loaded.getLevel1SubjectName());
            assertEquals("工资支出", loaded.getLevel2SubjectName());
            assertEquals("SALARY", loaded.getLevel2SubjectCode());
            assertEquals(new BigDecimal("100.00"), loaded.getBookedAmount());
            assertEquals("工资支出", vouchers.selectByVoucherNo(voucher.getVoucherNo()).getLevel2SubjectName());
            assertEquals("工资支出", vouchers.selectAll().get(0).getLevel2SubjectName());
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.update("DELETE FROM finance_subject WHERE id = ?", second.getId()));

            FinanceSubject replacement = subject(2, "CUSTOM_TEST", "自定义");
            subjects.insert(replacement);
            voucher.setLevel2SubjectId(replacement.getId());
            vouchers.updateByUuid(voucher);
            assertEquals("自定义", vouchers.selectByUuid(voucher.getUuid()).getLevel2SubjectName());
            assertEquals(3, subjects.selectAll().size());
        }
    }

    private FinanceSubject subject(int level, String code, String name) {
        FinanceSubject subject = new FinanceSubject();
        subject.setSubjectLevel(level);
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        subject.setSortOrder(level);
        return subject;
    }
}
