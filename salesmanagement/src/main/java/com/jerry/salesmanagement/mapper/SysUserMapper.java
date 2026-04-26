package com.jerry.salesmanagement.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.jerry.salesmanagement.pojo.SysUser;

@Mapper
public interface SysUserMapper {
    SysUser selectById(Long id);
    SysUser selectByUsername(String username);
    List<SysUser> selectAll();

    int insert(SysUser sysUser);
    int update(SysUser sysUser);
    int deleteById(Long id);
}
