package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ContractBasicInfo {
    private Long id;
    private String uuid;
    private String projectBasicInfoId; // references project.uuid
    private String subItemNo;
    private Long contractTypeId;       // references contract_type.id
    private String subItemContent;
    private String contractNumber;
    private String clientId;           // references customer.uuid
    private String contactId;          // references customer_contact.uuid
    private Date signDate;
    private Double contractAmount;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
