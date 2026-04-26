package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SysPermissionMapper;
import com.jerry.salesmanagement.pojo.SysPermission;
import com.jerry.salesmanagement.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]*$");

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Override
    public SysPermission getById(Long id) {
        return permissionMapper.selectById(id);
    }

    @Override
    public SysPermission getByPermissionCode(String permissionCode) {
        return permissionMapper.selectByPermissionCode(permissionCode);
    }

    @Override
    public List<SysPermission> getAll() {
        return permissionMapper.selectAll();
    }

    @Override
    public SysPermission create(SysPermission permission) {
        validate(permission, false);
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    public SysPermission update(SysPermission permission) {
        validate(permission, true);
        permissionMapper.update(permission);
        return permission;
    }

    @Override
    public void delete(Long id) {
        permissionMapper.deleteById(id);
    }

    private void validate(SysPermission permission, boolean updating) {
        if (updating && permission.getId() == null) {
            throw new IllegalArgumentException("Permission ID is required");
        }
        if (!StringUtils.hasText(permission.getPermissionCode())) {
            throw new IllegalArgumentException("Permission code is required");
        }
        if (!PERMISSION_CODE_PATTERN.matcher(permission.getPermissionCode().trim()).matches()) {
            throw new IllegalArgumentException("Permission code must use lowercase letters, digits, dot, dash, or underscore");
        }
        if (!StringUtils.hasText(permission.getPermissionName())) {
            throw new IllegalArgumentException("Permission name is required");
        }
        SysPermission existing = permissionMapper.selectByPermissionCode(permission.getPermissionCode().trim());
        if (existing != null && (!updating || !existing.getId().equals(permission.getId()))) {
            throw new IllegalArgumentException("Permission code already exists");
        }
        permission.setPermissionCode(permission.getPermissionCode().trim());
        permission.setPermissionName(permission.getPermissionName().trim());
    }
}
