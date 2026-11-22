package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProjectTypeMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.ProjectType;
import com.jerry.salesmanagement.service.ProjectTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class ProjectTypeServiceImpl implements ProjectTypeService {

    @Autowired
    private ProjectTypeMapper mapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public ProjectType getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ProjectType> getAll() {
        return mapper.selectAll();
    }

    @Override
    @Transactional
    public ProjectType create(ProjectType projectType) {
        validate(projectType);
        mapper.insert(projectType);
        return projectType;
    }

    @Override
    @Transactional
    public ProjectType update(ProjectType projectType) {
        if (projectType.getId() == null) {
            throw new IllegalArgumentException("Project type ID is required");
        }
        ProjectType existing = mapper.selectById(projectType.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Project type does not exist");
        }
        validate(projectType);
        mapper.update(projectType);
        return mapper.selectById(projectType.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "项目类型",
                    new ReferenceCheckMapper.ReferenceUsage("项目", referenceCheckMapper.countProjectsByProjectTypeId(id))
            );
        }
        mapper.deleteById(id);
    }

    private void validate(ProjectType projectType) {
        if (!StringUtils.hasText(projectType.getCode())) {
            throw new IllegalArgumentException("Project type code is required");
        }
        projectType.setCode(projectType.getCode().trim().toUpperCase(Locale.ROOT));
        if (!StringUtils.hasText(projectType.getName())) {
            throw new IllegalArgumentException("Project type name is required");
        }
        projectType.setName(projectType.getName().trim());
        if (projectType.getIsActive() == null) {
            throw new IllegalArgumentException("Project type active flag is required");
        }
        ProjectType existing = mapper.selectByCode(projectType.getCode());
        if (existing != null && !existing.getId().equals(projectType.getId())) {
            throw new IllegalArgumentException("Project type code already exists");
        }
    }
}
