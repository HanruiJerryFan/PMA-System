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
    public SysRole create(SysRole role) {
        roleMapper.insert(role);
        return role;
    }

    @Override
    public SysRole update(SysRole role) {
        roleMapper.update(role);
        return role;
    }

    @Override
    public void delete(Long id) {
        roleMapper.deleteById(id);
    }
}
