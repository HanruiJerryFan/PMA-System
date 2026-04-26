package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SysUser {
        private Long id;
        private String username;
        private String password;
        private String realName;
        private String status;
        private Boolean forcePasswordChange;
        private Timestamp createTime;
        private Long createUser;
        private Timestamp updateTime;
        private Long updateUser;
}
