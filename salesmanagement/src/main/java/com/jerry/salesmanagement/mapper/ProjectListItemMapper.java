package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProjectListItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectListItemMapper {
    ProjectListItem selectByUuid(String uuid);
    List<ProjectListItem> selectByProjectListId(String projectListId);
    int insert(ProjectListItem item);
    int updateByUuid(ProjectListItem item);
    int deleteByUuid(String uuid);
    int deleteByProjectListId(String projectListId);
}
