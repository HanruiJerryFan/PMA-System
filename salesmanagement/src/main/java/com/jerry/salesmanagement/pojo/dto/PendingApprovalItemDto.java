package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PendingApprovalItemDto {
    private String module;
    private String id;
    private String code;
    private String title;
    private String subType;
    private String subTypeLabel;
    private String projectId;
    private String projectNumber;
    private String projectName;
    private String counterpartyName;
    private BigDecimal amount;
    private String bizDate;
    private Long entryUser;
    private String entryUserName;
    private Date createTime;
    private String detailRoute;
}
