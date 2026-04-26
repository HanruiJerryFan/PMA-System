package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.SysUserRole;

import java.util.List;

public interface SysUserRoleService {

    List<SysUserRole> getByUserId(Long userId);

    List<SysUserRole> getByRoleId(Long roleId);

    void addUserRole(SysUserRole userRole);
    
    void deleteUserRole(Long userId, Long roleId);
}
