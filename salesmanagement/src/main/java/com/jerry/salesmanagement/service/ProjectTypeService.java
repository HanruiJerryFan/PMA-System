package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProjectType;

import java.util.List;

public interface ProjectTypeService {
    ProjectType getById(Long id);
    List<ProjectType> getAll();
    ProjectType create(ProjectType projectType);
    ProjectType update(ProjectType projectType);
    void delete(Long id);
}
