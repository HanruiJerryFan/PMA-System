package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysPermissionMapper {
    SysPermission selectById(Long id);
    SysPermission selectByPermissionCode(String permissionCode);
    List<SysPermission> selectAll();
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    int insert(SysPermission permission);
    int update(SysPermission permission);
    int deleteById(Long id);
}
