package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SysPermissionMapper;
import com.jerry.salesmanagement.mapper.SysRoleMapper;
import com.jerry.salesmanagement.mapper.SysRolePermissionMapper;
import com.jerry.salesmanagement.pojo.SysPermission;
import com.jerry.salesmanagement.pojo.SysRole;
import com.jerry.salesmanagement.pojo.SysRolePermission;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.SysRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysRolePermissionServiceImpl implements SysRolePermissionService {

    private static final String SYSTEM_NAME = "System";

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private SysRoleMapper roleMapper;

    @Autowired(required = false)
    private SysPermissionMapper permissionMapper;

    @Override
    public List<SysRolePermission> getByRoleId(Long roleId) {
        return rolePermissionMapper.selectByRoleId(roleId);
    }

    @Override
    public void addRolePermission(SysRolePermission rp) {
        if (rp == null) {
            throw new IllegalArgumentException("Role permission assignment is required");
        }
        validateRolePermissionMutation(rp.getRoleId(), rp.getPermissionId());
        rolePermissionMapper.insert(rp);
    }

    @Override
    public void deleteRolePermission(Long roleId, Long permissionId) {
        validateRolePermissionMutation(roleId, permissionId);
        rolePermissionMapper.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    private void validateRolePermissionMutation(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            throw new IllegalArgumentException("Role ID and Permission ID are required");
        }
        if (currentUserService == null || currentUserService.getCurrentUser() == null) {
            return;
        }
        boolean isSystemActor = currentUserService.isSystemUser();
        if (roleMapper != null) {
            SysRole targetRole = roleMapper.selectById(roleId);
            if (targetRole != null
                    && StringUtils.hasText(targetRole.getRoleName())
                    && SYSTEM_NAME.equalsIgnoreCase(targetRole.getRoleName().trim())
                    && !isSystemActor) {
                throw new IllegalArgumentException("仅 System 账号可修改 System 角色的权限");
            }
        }
        if (!isSystemActor && permissionMapper != null) {
            SysPermission permission = permissionMapper.selectById(permissionId);
            if (permission != null
                    && StringUtils.hasText(permission.getPermissionCode())
                    && !currentUserService.hasAnyAuthority(permission.getPermissionCode().trim())) {
                throw new IllegalArgumentException("不能授予或回收超出当前账号权限范围的权限项");
            }
        }
    }
}
