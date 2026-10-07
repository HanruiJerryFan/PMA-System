package com.jerry.salesmanagement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerry.salesmanagement.mapper.*;
import com.jerry.salesmanagement.pojo.*;
import com.jerry.salesmanagement.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FinanceVoucherSubjectTests {
    private final FinanceVoucherServiceImpl service = new FinanceVoucherServiceImpl();
    private final FinanceVoucherMapper voucherMapper = mock(FinanceVoucherMapper.class);
    private final FinanceSubjectMapper subjectMapper = mock(FinanceSubjectMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mapper", voucherMapper);
        ReflectionTestUtils.setField(service, "financeSubjectService", new FinanceSubjectServiceImpl(subjectMapper));
        CurrentUserService currentUser = mock(CurrentUserService.class);
        when(currentUser.requireCurrentUserId()).thenReturn(1L);
        ReflectionTestUtils.setField(service, "currentUserService", currentUser);
        ReflectionTestUtils.setField(service, "entryAuditService", mock(EntryAuditService.class));
        ReflectionTestUtils.setField(service, "customerService", mock(CustomerService.class));
        ProjectMapper projects = mock(ProjectMapper.class);
        when(projects.selectByUuid("project")).thenReturn(new Project());
        ReflectionTestUtils.setField(service, "projectMapper", projects);
        CustomerMapper customers = mock(CustomerMapper.class);
        Customer customer = new Customer();
        customer.setUuid("customer");
        customer.setCustomerName("往来单位");
        when(customers.selectByUuid("customer")).thenReturn(customer);
        ReflectionTestUtils.setField(service, "customerMapper", customers);
        TaxRateDictMapper rates = mock(TaxRateDictMapper.class);
        TaxRateDict taxRate = new TaxRateDict();
        taxRate.setRate(BigDecimal.ZERO);
        taxRate.setIsActive(true);
        when(rates.selectByRate(new BigDecimal("0.00"))).thenReturn(taxRate);
        ReflectionTestUtils.setField(service, "taxRateDictMapper", rates);
        when(subjectMapper.selectById(501L)).thenReturn(subject(501L, 1, "CUSTOM_ONE", "自定义一级"));
    }

    @Test
    void savesCustomSubjectIdsAndResolvesNamesFromDictionary() {
        when(subjectMapper.selectById(602L)).thenReturn(subject(602L, 2, "CUSTOM_TWO", "自定义二级"));
        FinanceVoucher result = service.create(voucher());
        assertEquals(501L, result.getLevel1SubjectId());
        assertEquals(602L, result.getLevel2SubjectId());
        assertEquals("自定义一级", result.getLevel1SubjectName());
        assertEquals("自定义二级", result.getLevel2SubjectName());
        assertEquals(new BigDecimal("106.10"), result.getBookedAmount());
        verify(voucherMapper).insert(result);
    }

    @Test
    void renamedSalarySubjectUsesItsDictionaryCodeAndIgnoresSubmittedRuleCode() {
        when(subjectMapper.selectById(602L)).thenReturn(subject(602L, 2, "SALARY", "工资支出"));
        FinanceVoucher voucher = voucher();
        voucher.setLevel2SubjectCode("OTHER");
        FinanceVoucher result = service.create(voucher);
        assertEquals("工资支出", result.getLevel2SubjectName());
        assertEquals("SALARY", result.getLevel2SubjectCode());
        assertEquals(new BigDecimal("100.00"), result.getBookedAmount());
    }

    @Test
    void renamedProjectTransferKeepsNegativeBookedAmount() {
        when(subjectMapper.selectById(602L)).thenReturn(subject(602L, 2, "PROJECT_TRANSFER_OUT", "项目调拨支出"));
        FinanceVoucher voucher = voucher();
        voucher.setActualExpenseAmount(new BigDecimal("82.00"));
        assertEquals(new BigDecimal("-100.00"), service.create(voucher).getBookedAmount());
    }

    @Test
    void rejectsSecondLevelIdPointingToFirstLevelSubject() {
        when(subjectMapper.selectById(602L)).thenReturn(subject(602L, 1, "CUSTOM_WRONG", "一级"));
        assertThrows(IllegalArgumentException.class, () -> service.create(voucher()));
        verify(voucherMapper, never()).insert(any());
    }

    @Test
    void ruleCodeCannotBeProvidedThroughJson() throws Exception {
        FinanceVoucher voucher = new ObjectMapper().readValue(
                "{\"level1SubjectId\":501,\"level2SubjectId\":602,\"level2SubjectCode\":\"SALARY\"}", FinanceVoucher.class);
        assertEquals(602L, voucher.getLevel2SubjectId());
        assertNull(voucher.getLevel2SubjectCode());
    }

    private FinanceVoucher voucher() {
        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setVoucherNo("2026100601");
        voucher.setOccurredOn(LocalDate.of(2026, 10, 6));
        voucher.setProjectId("project");
        voucher.setLevel1SubjectId(501L);
        voucher.setLevel2SubjectId(602L);
        voucher.setSummary("支出");
        voucher.setTransactionDirection("PAY");
        voucher.setTaxRate(BigDecimal.ZERO);
        voucher.setCounterpartyCustomerId("customer");
        voucher.setInvoiceStatus("NOT_INVOICED");
        voucher.setIsCompleted(true);
        voucher.setActualExpenseAmount(new BigDecimal("87.00"));
        return voucher;
    }

    private FinanceSubject subject(Long id, int level, String code, String name) {
        FinanceSubject subject = new FinanceSubject();
        subject.setId(id);
        subject.setSubjectLevel(level);
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        return subject;
    }
}
