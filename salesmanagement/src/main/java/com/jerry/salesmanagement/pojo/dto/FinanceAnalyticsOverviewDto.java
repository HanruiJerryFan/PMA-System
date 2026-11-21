package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceAnalyticsOverviewDto {
    private boolean canViewContracts;
    private BigDecimal totalContractAmount;
    private BigDecimal totalActualIncome;
    private BigDecimal totalActualExpense;
    private BigDecimal totalBookedIncome;
    private BigDecimal totalBookedExpense;
    private BigDecimal totalGrossProfit;
    private BigDecimal overallGrossMarginRate;

    private BigDecimal nonProjectIncome;
    private BigDecimal nonProjectExpense;
    private BigDecimal nonProjectBookedExpense;

    private BigDecimal totalReceivable;
    private BigDecimal totalPayable;
    private BigDecimal totalPendingInvoice;
    private BigDecimal totalMissingInvoice;

    private AgingBucketDto receivableAging = new AgingBucketDto();
    private AgingBucketDto payableAging = new AgingBucketDto();

    private List<ProjectProfitRowDto> projectProfits = new ArrayList<>();
    private List<CounterpartyAgingRowDto> receivableByCounterparty = new ArrayList<>();
    private List<CounterpartyAgingRowDto> payableByCounterparty = new ArrayList<>();
}
