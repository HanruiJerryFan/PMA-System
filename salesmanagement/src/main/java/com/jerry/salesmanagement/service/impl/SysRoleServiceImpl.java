package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SysRoleMapper;
import com.jerry.salesmanagement.pojo.SysRole;
import com.jerry.salesmanagement.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;

    @Override
    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public List<SysRole> getAll() {
        return roleMapper.selectAll();
    }

    @Override
    public List<String> getRoleNamesByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return roleMapper.selectRoleNamesByUserId(userId);
    }

    @Override
    public SysRole create(SysRole role) {
        roleMapper.insert(role);
        return role;
    }

    @Override
    public SysRole update(SysRole role) {
        if (role == null || role.getId() == null) {
            throw new IllegalArgumentException("Role ID is required");
        }
        SysRole existing = roleMapper.selectById(role.getId());
        if (isSystemRole(existing)) {
            throw new IllegalArgumentException("内置 System 角色不可修改或删除");
        }
        roleMapper.update(role);
        return role;
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Role ID is required");
        }
        SysRole existing = roleMapper.selectById(id);
        if (isSystemRole(existing)) {
            throw new IllegalArgumentException("内置 System 角色不可修改或删除");
        }
        roleMapper.deleteById(id);
    }

    private boolean isSystemRole(SysRole role) {
        return role != null
                && role.getRoleName() != null
                && "System".equalsIgnoreCase(role.getRoleName().trim());
    }
}
