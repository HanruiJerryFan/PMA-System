package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class AuditTrail {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private Date occurredAt;
}
