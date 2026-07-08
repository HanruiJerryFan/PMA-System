package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProjectListItem implements EntryAuditable {
    private Long id;
    private String uuid;
    private String projectListId;
    private String materialId;
    private String materialCode;
    private String itemName;
    private String model;
    private String brand;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String sourceType;
    private String remark;
    private Long entryUser;
    private String entryUserName;
    private Long auditorUser;
    private String auditorUserName;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
