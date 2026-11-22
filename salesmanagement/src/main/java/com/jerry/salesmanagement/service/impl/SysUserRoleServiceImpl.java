package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SysPermissionMapper;
import com.jerry.salesmanagement.mapper.SysRoleMapper;
import com.jerry.salesmanagement.mapper.SysRolePermissionMapper;
import com.jerry.salesmanagement.mapper.SysUserMapper;
import com.jerry.salesmanagement.mapper.SysUserRoleMapper;
import com.jerry.salesmanagement.pojo.SysPermission;
import com.jerry.salesmanagement.pojo.SysRole;
import com.jerry.salesmanagement.pojo.SysRolePermission;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.SysUserRole;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysUserRoleServiceImpl implements SysUserRoleService {

    private static final String SYSTEM_NAME = "System";

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private SysRoleMapper roleMapper;

    @Autowired(required = false)
    private SysUserMapper userMapper;

    @Autowired(required = false)
    private SysRolePermissionMapper rolePermissionMapper;

    @Autowired(required = false)
    private SysPermissionMapper permissionMapper;

    @Override
    public List<SysUserRole> getByUserId(Long userId) {
        return userRoleMapper.selectByUserId(userId);
    }

    @Override
    public List<SysUserRole> getByRoleId(Long roleId) {
        return userRoleMapper.selectByRoleId(roleId);
    }

    @Override
    public void addUserRole(SysUserRole userRole) {
        if (userRole == null) {
            throw new IllegalArgumentException("User role assignment is required");
        }
        validateUserRoleMutation(userRole.getUserId(), userRole.getRoleId());
        userRoleMapper.insert(userRole);
    }

    @Override
    public void deleteUserRole(Long userId, Long roleId) {
        validateUserRoleMutation(userId, roleId);
        userRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
    }

    private void validateUserRoleMutation(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            throw new IllegalArgumentException("User ID and Role ID are required");
        }
        if (currentUserService == null || currentUserService.getCurrentUser() == null) {
            return;
        }
        boolean isSystemActor = currentUserService.isSystemUser();
        if (userMapper != null) {
            SysUser targetUser = userMapper.selectById(userId);
            if (targetUser != null
                    && StringUtils.hasText(targetUser.getUsername())
                    && SYSTEM_NAME.equalsIgnoreCase(targetUser.getUsername().trim())
                    && !isSystemActor) {
                throw new IllegalArgumentException("仅 System 账号可调整 System 账号的角色");
            }
        }
        if (roleMapper != null) {
            SysRole targetRole = roleMapper.selectById(roleId);
            if (targetRole != null
                    && StringUtils.hasText(targetRole.getRoleName())
                    && SYSTEM_NAME.equalsIgnoreCase(targetRole.getRoleName().trim())
                    && !isSystemActor) {
                throw new IllegalArgumentException("仅 System 账号可分配或移除 System 角色");
            }
        }
        Long actorId = currentUserService.getCurrentUserId();
        if (!isSystemActor && actorId != null && Objects.equals(actorId, userId)) {
            throw new IllegalArgumentException("不能修改当前登录账号的角色分配");
        }
        if (!isSystemActor && rolePermissionMapper != null && permissionMapper != null) {
            Map<Long, String> permissionCodeById = permissionMapper.selectAll().stream()
                    .filter(p -> p != null && p.getId() != null && StringUtils.hasText(p.getPermissionCode()))
                    .collect(Collectors.toMap(SysPermission::getId, p -> p.getPermissionCode().trim(), (a, b) -> a));
            for (SysRolePermission rp : rolePermissionMapper.selectByRoleId(roleId)) {
                String code = rp == null ? null : permissionCodeById.get(rp.getPermissionId());
                if (StringUtils.hasText(code) && !currentUserService.hasAnyAuthority(code)) {
                    throw new IllegalArgumentException("不能分配或移除超出当前账号权限范围的角色");
                }
            }
        }
    }
}
