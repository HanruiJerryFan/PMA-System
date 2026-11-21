package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    SysRole selectById(Long id);
    List<SysRole> selectAll();
    List<String> selectRoleNamesByUserId(@Param("userId") Long userId);

    int insert(SysRole role);
    int update(SysRole role);
    int deleteById(Long id);
}
