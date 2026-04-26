package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectListAggregateItem {
    private String materialId;
    private String materialCode;
    private String itemName;
    private String model;
    private String brand;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private int sourceListCount;
}
