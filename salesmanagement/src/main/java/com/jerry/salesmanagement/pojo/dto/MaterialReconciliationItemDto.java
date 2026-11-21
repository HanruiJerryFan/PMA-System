package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialReconciliationItemDto {
    private String materialKey;
    private String materialId;
    private String materialCode;
    private String itemName;
    private String model;
    private String brand;
    private String unit;

    private BigDecimal plannedSalesQty;
    private BigDecimal plannedSalesAmount;
    private BigDecimal plannedProcurementQty;
    private BigDecimal plannedProcurementAmount;

    private BigDecimal purchasedQty;
    private BigDecimal purchasedAmount;
    private BigDecimal transferInQty;
    private BigDecimal transferInAmount;
    private BigDecimal transferOutQty;
    private BigDecimal transferOutAmount;
    private BigDecimal netSuppliedQty;
    private BigDecimal shippedQty;
    private BigDecimal shippedAmount;

    private BigDecimal procurementGapQty;
    private BigDecimal shipmentGapQty;
    private BigDecimal unshippedOnHandQty;

    private String status;
}
