package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.pojo.FinanceVoucher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceVoucherServiceImplAccountingTests {

    private final FinanceVoucherServiceImpl service = new FinanceVoucherServiceImpl();

    @Test
    void travelExpenseUsesThirteenPercentDeduction() {
        FinanceVoucher voucher = payVoucher("TRAVEL", "NOT_INVOICED", "0.00", "87.00");

        assertEquals(new BigDecimal("100.00"), calculateBookedAmount(voucher));
    }

    @Test
    void salaryExpenseUsesThirteenPercentDeduction() {
        FinanceVoucher voucher = payVoucher("SALARY", "INVOICED", "0.00", "87.00");

        assertEquals(new BigDecimal("100.00"), calculateBookedAmount(voucher));
    }

    @Test
    void zeroTaxUninvoicedExpenseUsesEighteenPercentDeduction() {
        FinanceVoucher voucher = payVoucher("OTHER", "NOT_INVOICED", "0.00", "82.00");

        assertEquals(new BigDecimal("100.00"), calculateBookedAmount(voucher));
    }

    @Test
    void zeroTaxInvoicedExpenseUsesElevenPointFivePercentDeduction() {
        FinanceVoucher voucher = payVoucher("OTHER", "INVOICED", "0.00", "88.50");

        assertEquals(new BigDecimal("100.00"), calculateBookedAmount(voucher));
    }

    @Test
    void zeroTaxIncomeUsesInvoiceStatusDeduction() {
        FinanceVoucher uninvoiced = receiveVoucher("NOT_INVOICED", "82.00");
        FinanceVoucher invoiced = receiveVoucher("INVOICED", "88.50");

        assertEquals(new BigDecimal("100.00"), calculateBookedAmount(uninvoiced));
        assertEquals(new BigDecimal("100.00"), calculateBookedAmount(invoiced));
    }

    private FinanceVoucher payVoucher(String subject, String invoiceStatus, String taxRate, String expense) {
        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setTransactionDirection("PAY");
        voucher.setLevel2SubjectCode(subject);
        voucher.setInvoiceStatus(invoiceStatus);
        voucher.setTaxRate(new BigDecimal(taxRate));
        voucher.setActualExpenseAmount(new BigDecimal(expense));
        return voucher;
    }

    private FinanceVoucher receiveVoucher(String invoiceStatus, String income) {
        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setTransactionDirection("RECEIVE");
        voucher.setInvoiceStatus(invoiceStatus);
        voucher.setTaxRate(BigDecimal.ZERO);
        voucher.setActualIncomeAmount(new BigDecimal(income));
        return voucher;
    }

    private BigDecimal calculateBookedAmount(FinanceVoucher voucher) {
        return ReflectionTestUtils.invokeMethod(service, "calculateBookedAmount", voucher);
    }
}
