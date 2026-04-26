package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.SysRole;

import java.util.List;

public interface SysRoleService {
    SysRole getById(Long id);
    List<SysRole> getAll();
    SysRole create(SysRole role);
    SysRole update(SysRole role);
    void delete(Long id);
}
