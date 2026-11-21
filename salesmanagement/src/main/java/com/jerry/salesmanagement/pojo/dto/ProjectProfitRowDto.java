package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectProfitRowDto {
    private String projectId;
    private String projectNumber;
    private String projectName;
    private String customerId;
    private String customerName;
    private String currentStageName;

    private BigDecimal contractAmount;
    private BigDecimal actualIncomeAmount;
    private BigDecimal actualExpenseAmount;
    private BigDecimal bookedIncomeAmount;
    private BigDecimal bookedExpenseAmount;
    private BigDecimal grossProfit;
    private BigDecimal grossMarginRate;
    private BigDecimal bookedNetProfit;

    private BigDecimal receivableAmount;
    private BigDecimal payableAmount;
    private BigDecimal pendingInvoiceAmount;
    private BigDecimal missingInvoiceAmount;
    private BigDecimal collectionRate;

    private int voucherCount;
    private int uncompletedVoucherCount;
}
