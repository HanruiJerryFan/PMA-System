package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class WarehouseDocumentItem {
    private Long id;
    private String uuid;
    private String docNumber;
    private Integer lineNo;
    private String materialId;
    private String materialCode;
    private String materialName;
    private String model;
    private String displayName;
    private String displayModel;
    private String brand;
    private String unit;
    private Double quantity;
    private Double unitPrice;
    private Double amount;
    private String remark;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
