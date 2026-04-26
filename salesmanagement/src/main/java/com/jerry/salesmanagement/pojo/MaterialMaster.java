package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class MaterialMaster {
    private Long id;
    private String uuid;
    private String brandCode;
    private String categoryCode;
    private String subcategoryCode;
    private String sequenceNo;
    private String bandCode;
    private String materialCode;
    private Long categoryId;
    private Long subcategoryId;
    private String productName;
    private String productModel;
    private Long brandId;
    private String brandName;
    private String manufacturer;
    private String unit;
    private String frequency;
    private String specification;
    private String otherNote;
    private Boolean isActive;
    private Date createdAt;
    private Long createdBy;
    private Date updatedAt;
    private Long updatedBy;
}
