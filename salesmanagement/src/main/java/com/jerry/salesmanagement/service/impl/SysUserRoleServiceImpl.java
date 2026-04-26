package com.jerry.salesmanagement.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jerry.salesmanagement.mapper.SysUserRoleMapper;
import com.jerry.salesmanagement.pojo.SysUserRole;
import com.jerry.salesmanagement.service.SysUserRoleService;

@Service
public class SysUserRoleServiceImpl implements SysUserRoleService {

    @Autowired
    private SysUserRoleMapper userRoleMapper;

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
        userRoleMapper.insert(userRole);
    }

    @Override
    public void deleteUserRole(Long userId, Long roleId) {
        userRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
    }
}
