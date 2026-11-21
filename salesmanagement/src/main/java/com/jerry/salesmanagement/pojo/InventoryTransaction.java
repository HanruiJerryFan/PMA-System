package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InventoryTransaction {
    private Long id;
    private String uuid;
    private String materialId;
    private String materialCode;
    private String materialName;
    private String model;
    private String brand;
    private String category;
    private String projectId;
    private Long warehouseId;
    private String txnType;
    private Date txnTime;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String sourceRefType;
    private String sourceRefId;
    private Date createdAt;
    private Long createdBy;
    private Date updatedAt;
    private Long updatedBy;
}
