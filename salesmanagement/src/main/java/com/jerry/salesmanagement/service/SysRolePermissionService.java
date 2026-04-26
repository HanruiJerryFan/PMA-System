package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.SysRolePermission;

import java.util.List;

public interface SysRolePermissionService {
    List<SysRolePermission> getByRoleId(Long roleId);
    void addRolePermission(SysRolePermission rp);
    void deleteRolePermission(Long roleId, Long permissionId);
}
