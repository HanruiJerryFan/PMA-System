package com.jerry.salesmanagement.common;

import com.jerry.salesmanagement.pojo.FinanceVoucher;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared financial calculation helpers aligned with FinanceVouchers validation and aging rules.
 */
public final class FinanceVoucherMetrics {

    public enum GapType {
        NONE,
        RECEIVABLE,
        PAYABLE,
        PENDING_INVOICE,
        MISSING_INVOICE
    }

    public record GapInfo(GapType type, BigDecimal amount) {
        public static GapInfo none() {
            return new GapInfo(GapType.NONE, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private FinanceVoucherMetrics() {
    }

    public static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static BigDecimal scale2(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal scale4(BigDecimal value) {
        return defaultZero(value).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Resolves receivable/payable/invoice-gap classification matching frontend rules.
     */
    public static GapInfo resolveGap(FinanceVoucher voucher) {
        if (voucher == null) {
            return GapInfo.none();
        }
        String direction = voucher.getTransactionDirection();
        if (!"RECEIVE".equals(direction) && !"PAY".equals(direction)) {
            return GapInfo.none();
        }

        BigDecimal actualAmount = "RECEIVE".equals(direction)
                ? defaultZero(voucher.getActualIncomeAmount())
                : defaultZero(voucher.getActualExpenseAmount());

        if (!Boolean.TRUE.equals(voucher.getIsCompleted())) {
            if (actualAmount.compareTo(BigDecimal.ZERO) > 0) {
                return new GapInfo(
                        "RECEIVE".equals(direction) ? GapType.RECEIVABLE : GapType.PAYABLE,
                        scale2(actualAmount)
                );
            }
            return GapInfo.none();
        }

        BigDecimal invoiceAmount = voucher.getInvoiceAmount();
        if (invoiceAmount == null) {
            return GapInfo.none();
        }

        int cmp = actualAmount.compareTo(invoiceAmount);
        if ("RECEIVE".equals(direction)) {
            if (cmp < 0) {
                return new GapInfo(GapType.RECEIVABLE, scale2(invoiceAmount.subtract(actualAmount)));
            }
            if (cmp > 0) {
                return new GapInfo(GapType.PENDING_INVOICE, scale2(actualAmount.subtract(invoiceAmount)));
            }
        } else {
            if (cmp < 0) {
                return new GapInfo(GapType.PAYABLE, scale2(invoiceAmount.subtract(actualAmount)));
            }
            if (cmp > 0) {
                return new GapInfo(GapType.MISSING_INVOICE, scale2(actualAmount.subtract(invoiceAmount)));
            }
        }
        return GapInfo.none();
    }
}
