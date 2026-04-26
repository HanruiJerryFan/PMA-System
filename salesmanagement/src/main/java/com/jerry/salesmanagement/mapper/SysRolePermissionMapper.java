package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper {
    List<SysRolePermission> selectByRoleId(Long roleId);

    int insert(SysRolePermission rp);

    int deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, 
                                      @Param("permissionId") Long permissionId);
}
