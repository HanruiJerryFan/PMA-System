package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProjectType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectTypeMapper {
    ProjectType selectById(Long id);
    ProjectType selectByCode(String code);
    List<ProjectType> selectAll();
    List<ProjectType> selectActive();
    int insert(ProjectType projectType);
    int update(ProjectType projectType);
    int deleteById(Long id);
}
