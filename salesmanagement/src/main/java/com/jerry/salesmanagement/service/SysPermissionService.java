package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.SysPermission;

import java.util.List;

public interface SysPermissionService {
    SysPermission getById(Long id);
    SysPermission getByPermissionCode(String permissionCode);
    List<SysPermission> getAll();
    SysPermission create(SysPermission permission);
    SysPermission update(SysPermission permission);
    void delete(Long id);
}
