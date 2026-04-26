package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class CustomerContact {
    private Long id;
    private String uuid;
    private String clientId;  // references customer.uuid
    private String contactName;
    private String contactPosition;
    private String contactPhone;
    private String wechat;
    private String email;
    private String qq;
    private String remark;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
