package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SysRolePermissionMapper;
import com.jerry.salesmanagement.pojo.SysRolePermission;
import com.jerry.salesmanagement.service.SysRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRolePermissionServiceImpl implements SysRolePermissionService {

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Override
    public List<SysRolePermission> getByRoleId(Long roleId) {
        return rolePermissionMapper.selectByRoleId(roleId);
    }

    @Override
    public void addRolePermission(SysRolePermission rp) {
        rolePermissionMapper.insert(rp);
    }

    @Override
    public void deleteRolePermission(Long roleId, Long permissionId) {
        rolePermissionMapper.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }
}
