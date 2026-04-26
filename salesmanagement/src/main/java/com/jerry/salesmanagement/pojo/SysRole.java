package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class SysRole {
    private Long id;
    private String roleName;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}