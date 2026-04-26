package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.SysRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    SysRole selectById(Long id);
    List<SysRole> selectAll();

    int insert(SysRole role);
    int update(SysRole role);
    int deleteById(Long id);
}
