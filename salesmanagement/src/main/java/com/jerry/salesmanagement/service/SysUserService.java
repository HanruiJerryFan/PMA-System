package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.SysUser;

import java.util.List;

public interface SysUserService {
    SysUser getById(Long id);
    SysUser getByUsername(String username);
    List<SysUser> getAll();

    SysUser create(SysUser user);
    SysUser update(SysUser user);
    void changePassword(Long userId, String oldPassword, String newPassword, Long operatorId);
    String adminResetPassword(Long userId, Long operatorId);
    void delete(Long id);
}
