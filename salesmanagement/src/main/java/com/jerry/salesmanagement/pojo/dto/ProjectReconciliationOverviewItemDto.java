package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectReconciliationOverviewItemDto {
    private String projectId;
    private String projectNumber;
    private String projectName;
    private String customerName;
    private int totalMaterialLines;
    private int shortageLineCount;
    private int pendingShipmentLineCount;
    private int overShippedLineCount;
    private BigDecimal plannedSalesAmount;
    private BigDecimal plannedProcurementAmount;
    private BigDecimal actualSuppliedAmount;
    private BigDecimal actualShippedAmount;
    private BigDecimal fulfillmentRate;
}
