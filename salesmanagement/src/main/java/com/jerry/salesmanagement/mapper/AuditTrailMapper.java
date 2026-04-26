package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.AuditTrail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditTrailMapper {
    List<AuditTrail> selectAll(
            @Param("userId") Long userId,
            @Param("module") String module,
            @Param("action") String action
    );

    AuditTrail selectById(@Param("id") Long id);

    int insert(AuditTrail auditTrail);
}
