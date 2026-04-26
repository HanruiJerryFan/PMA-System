package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class SysPermission {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
