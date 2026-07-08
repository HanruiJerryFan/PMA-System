package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProjectList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectListMapper {
    ProjectList selectByUuid(String uuid);
    List<ProjectList> selectAll();
    List<ProjectList> selectByProjectId(String projectId);
    int insert(ProjectList projectList);
    int updateByUuid(ProjectList projectList);
    int updateAuditorByUuid(
            @Param("uuid") String uuid,
            @Param("auditorUser") Long auditorUser,
            @Param("updateUser") Long updateUser
    );
    int deleteByUuid(String uuid);
}
