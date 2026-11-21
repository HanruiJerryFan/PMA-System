package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import com.jerry.salesmanagement.pojo.dto.ProjectStageSortOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectStatusHistoryMapper {
    List<ProjectStatusHistory> selectAll();

    ProjectStatusHistory selectByUuid(@Param("uuid") String uuid);

    List<ProjectStatusHistory> selectByProjectId(@Param("projectId") String projectId);

    Integer selectCurrentStageSortOrderByProjectId(@Param("projectId") String projectId);

    List<ProjectStageSortOrder> selectCurrentStageSortOrdersByProjectIds(@Param("projectIds") List<String> projectIds);

    int insert(ProjectStatusHistory history);

    int update(ProjectStatusHistory history);

    int deleteByUuid(@Param("uuid") String uuid);
}
