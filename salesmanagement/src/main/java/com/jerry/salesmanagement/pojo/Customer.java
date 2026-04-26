package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Customer {
    private Long id;
    private String uuid;
    private String customerCode;
    private String customerName;
    private Long regionId;
    private String registerAddress;
    private String officeAddress;
    private String taxIdentificationNumber;
    private Long customerTypeId;
    private String bankName;
    private String bankAccount;
    private String industry;
    private String remark;
    private String activityStatus;
    private Date lastActiveAt;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
