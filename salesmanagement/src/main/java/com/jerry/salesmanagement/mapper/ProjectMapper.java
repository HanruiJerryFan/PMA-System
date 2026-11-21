package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMapper {
    Project selectByUuid(String uuid);
    Project selectByProjectNumber(String projectNumber);
    List<Project> selectAll();
    List<Project> selectPage(
            @Param("projectNumber") String projectNumber,
            @Param("projectName") String projectName,
            @Param("customerId") String customerId,
            @Param("managerId") Long managerId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    long countPage(
            @Param("projectNumber") String projectNumber,
            @Param("projectName") String projectName,
            @Param("customerId") String customerId,
            @Param("managerId") Long managerId
    );
    int insert(Project project);
    int updateByUuid(Project project);
    int deleteByUuid(String uuid);
}
