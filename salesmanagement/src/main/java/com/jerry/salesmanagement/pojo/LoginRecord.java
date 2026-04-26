package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class LoginRecord {
    private Long id;
    private Long userId;
    private Date loginStartTime;
    private Date loginEndTime;
    private Long durationSeconds;
    private String loginIp;
    private String userAgent;
    private Date lastSeenAt;
    private String logoutReason;
    private Date createdAt;
}
