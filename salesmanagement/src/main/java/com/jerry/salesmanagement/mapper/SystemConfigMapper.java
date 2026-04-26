package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemConfigMapper {
    SystemConfig selectByKey(@Param("configKey") String configKey);
    List<SystemConfig> selectAll();
    int insert(SystemConfig systemConfig);
    int updateByKey(SystemConfig systemConfig);
    int deleteByKey(@Param("configKey") String configKey);
}
