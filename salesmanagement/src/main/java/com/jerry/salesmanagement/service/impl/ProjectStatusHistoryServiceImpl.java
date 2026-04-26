package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.ProjectStatusHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectStatusHistoryServiceImpl implements ProjectStatusHistoryService {

    @Autowired
    private ProjectStatusHistoryMapper mapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private StageMapper stageMapper;

    @Autowired
    private CustomerService customerService;

    @Override
    public List<ProjectStatusHistory> getAll() {
        return mapper.selectAll();
    }

    @Override
    public ProjectStatusHistory getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<ProjectStatusHistory> getByProjectId(String projectId) {
        return mapper.selectByProjectId(projectId);
    }

    @Override
    @Transactional
    public ProjectStatusHistory create(ProjectStatusHistory history) {
        Project project = validate(history, false);
        history.setUuid(UUID.randomUUID().toString());
        if (history.getCreatedAt() == null) {
            history.setCreatedAt(new Date());
        }
        mapper.insert(history);
        touchProjectCustomer(project);
        return mapper.selectByUuid(history.getUuid());
    }

    @Override
    @Transactional
    public ProjectStatusHistory update(ProjectStatusHistory history) {
        Project project = validate(history, true);
        if (history.getCreatedAt() == null) {
            history.setCreatedAt(new Date());
        }
        mapper.update(history);
        touchProjectCustomer(project);
        return mapper.selectByUuid(history.getUuid());
    }

    @Override
    public void delete(String uuid) {
        mapper.deleteByUuid(uuid);
    }

    private Project validate(ProjectStatusHistory history, boolean requireUuid) {
        if (requireUuid && !StringUtils.hasText(history.getUuid())) {
            throw new IllegalArgumentException("History UUID is required");
        }
        if (!StringUtils.hasText(history.getProjectId())) {
            throw new IllegalArgumentException("Project is required");
        }
        Project project = projectMapper.selectByUuid(history.getProjectId());
        if (project == null) {
            throw new IllegalArgumentException("Project does not exist");
        }
        if (history.getStageId() == null) {
            throw new IllegalArgumentException("Stage is required");
        }
        Stage stage = stageMapper.selectById(history.getStageId());
        if (stage == null) {
            throw new IllegalArgumentException("Stage does not exist");
        }
        if (!Boolean.TRUE.equals(stage.getIsActive())) {
            throw new IllegalArgumentException("Stage is disabled");
        }
        return project;
    }

    private void touchProjectCustomer(Project project) {
        if (project != null && StringUtils.hasText(project.getCustomerId())) {
            customerService.touchActivity(project.getCustomerId());
        }
    }
}
