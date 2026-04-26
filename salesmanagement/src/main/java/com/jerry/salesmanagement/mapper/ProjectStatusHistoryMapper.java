package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectStatusHistoryMapper {
    List<ProjectStatusHistory> selectAll();

    ProjectStatusHistory selectByUuid(@Param("uuid") String uuid);

    List<ProjectStatusHistory> selectByProjectId(@Param("projectId") String projectId);

    Integer selectCurrentStageSortOrderByProjectId(@Param("projectId") String projectId);

    int insert(ProjectStatusHistory history);

    int update(ProjectStatusHistory history);

    int deleteByUuid(@Param("uuid") String uuid);
}
