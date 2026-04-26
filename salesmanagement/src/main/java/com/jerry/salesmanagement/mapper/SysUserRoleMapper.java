package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {
    List<SysUserRole> selectByUserId(Long userId);
    List<SysUserRole> selectByRoleId(Long roleId);

    int insert(SysUserRole userRole);
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
