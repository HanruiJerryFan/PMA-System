package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProjectTypeMapper;
import com.jerry.salesmanagement.pojo.ProjectType;
import com.jerry.salesmanagement.service.ProjectTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProjectTypeServiceImpl implements ProjectTypeService {

    @Autowired
    private ProjectTypeMapper mapper;

    @Override
    public ProjectType getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ProjectType> getAll() {
        return mapper.selectAll();
    }

    @Override
    public ProjectType create(ProjectType projectType) {
        validate(projectType);
        mapper.insert(projectType);
        return projectType;
    }

    @Override
    public ProjectType update(ProjectType projectType) {
        validate(projectType);
        mapper.update(projectType);
        return mapper.selectById(projectType.getId());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void validate(ProjectType projectType) {
        if (!StringUtils.hasText(projectType.getCode())) {
            throw new IllegalArgumentException("Project type code is required");
        }
        projectType.setCode(projectType.getCode().trim().toUpperCase());
        if (!StringUtils.hasText(projectType.getName())) {
            throw new IllegalArgumentException("Project type name is required");
        }
        if (projectType.getIsActive() == null) {
            throw new IllegalArgumentException("Project type active flag is required");
        }
        ProjectType existing = mapper.selectByCode(projectType.getCode());
        if (existing != null && !existing.getId().equals(projectType.getId())) {
            throw new IllegalArgumentException("Project type code already exists");
        }
    }
}
