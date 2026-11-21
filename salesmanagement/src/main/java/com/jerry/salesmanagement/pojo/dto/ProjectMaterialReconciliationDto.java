package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectMaterialReconciliationDto {
    private String projectId;
    private String projectNumber;
    private String projectName;
    private String customerName;
    private boolean canViewInventory;

    private int totalMaterialLines;
    private int shortageLineCount;
    private int pendingShipmentLineCount;
    private int overShippedLineCount;
    private int overProcuredLineCount;
    private int matchedLineCount;

    private BigDecimal plannedSalesAmount;
    private BigDecimal plannedProcurementAmount;
    private BigDecimal actualSuppliedAmount;
    private BigDecimal actualShippedAmount;
    private BigDecimal fulfillmentRate;

    private List<MaterialReconciliationItemDto> items = new ArrayList<>();
}
