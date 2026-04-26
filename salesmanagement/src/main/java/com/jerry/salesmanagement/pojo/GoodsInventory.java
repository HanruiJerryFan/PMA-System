package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class GoodsInventory {
    private String uuid;
    private String materialId;
    private String materialCode;
    private String materialName;
    private String model;
    private String brand;
    private String businessCategory;
    private Long warehouseId;
    private String warehouseName;
    private String projectId;
    private String projectName;
    private Double inboundQuantity;
    private Double outboundQuantity;
    private Double balanceQuantity;
    private Date lastTxnTime;
}
