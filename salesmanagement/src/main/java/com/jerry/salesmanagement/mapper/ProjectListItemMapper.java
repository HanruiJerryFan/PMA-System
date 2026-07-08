package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProjectListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectListItemMapper {
    ProjectListItem selectByUuid(String uuid);
    List<ProjectListItem> selectByProjectListId(String projectListId);
    int insert(ProjectListItem item);
    int updateByUuid(ProjectListItem item);
    int updateAuditorByUuid(
            @Param("uuid") String uuid,
            @Param("auditorUser") Long auditorUser,
            @Param("updateUser") Long updateUser
    );
    int deleteByUuid(String uuid);
    int deleteByProjectListId(String projectListId);
}
