package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ProductHistoryPrice {
    private Long id;
    private String productBasicInfoUuid;
    private Double referencePrice;
    private String supplierCustomerId;
    private String supplier;
    private Date createTime;
    private Long createUser;
}
