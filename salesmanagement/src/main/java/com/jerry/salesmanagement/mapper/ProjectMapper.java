package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Project;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectMapper {
    Project selectByUuid(String uuid);
    Project selectByProjectNumber(String projectNumber);
    List<Project> selectAll();
    int insert(Project project);
    int updateByUuid(Project project);
    int deleteByUuid(String uuid);
}
