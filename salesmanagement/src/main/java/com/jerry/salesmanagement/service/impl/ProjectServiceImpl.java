package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.ProjectTypeMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectType;
import com.jerry.salesmanagement.pojo.dto.ProjectStageSortOrder;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.ProjectService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Pattern PROJECT_NUMBER_PATTERN = Pattern.compile("^[A-Z]{2}-\\d{4}$");
    private static final String PROJECT_DELETE_LOCK_STAGE_SORT_ORDER_KEY = "project.delete.lock_stage_sort_order";
    private static final int DEFAULT_PROJECT_DELETE_LOCK_STAGE_SORT_ORDER = 5;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectTypeMapper projectTypeMapper;

    @Autowired
    private ProjectStatusHistoryMapper projectStatusHistoryMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private CustomerService customerService;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public Project getByUuid(String uuid) {
        Project project = projectMapper.selectByUuid(uuid);
        enrichDeletionState(project);
        return project;
    }

    @Override
    public List<Project> getAll() {
        List<Project> projects = projectMapper.selectAll();
        enrichDeletionStates(projects);
        return projects;
    }

    @Override
    @Transactional
    public Project create(Project project) {
        validateProject(project);
        if (!StringUtils.hasText(project.getUuid())) {
            project.setUuid(UUID.randomUUID().toString());
        }
        projectMapper.insert(project);
        customerService.touchActivity(project.getCustomerId());
        return project;
    }

    @Override
    @Transactional
    public Project update(Project project) {
        validateProject(project);
        projectMapper.updateByUuid(project);
        customerService.touchActivity(project.getCustomerId());
        return project;
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        Project project = projectMapper.selectByUuid(uuid);
        if (project == null) {
            throw new IllegalArgumentException("Project does not exist");
        }
        enrichDeletionState(project);
        if (!Boolean.TRUE.equals(project.getCanDelete())) {
            throw new IllegalArgumentException("Project cannot be deleted after reaching the configured locked stage");
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "项目",
                    new ReferenceCheckMapper.ReferenceUsage("项目清单", referenceCheckMapper.countProjectListsByProjectUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("合同", referenceCheckMapper.countContractsByProjectUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("财务凭证", referenceCheckMapper.countFinanceVouchersByProjectUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("出入库单", referenceCheckMapper.countWarehouseDocsByProjectUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("库存流水", referenceCheckMapper.countInventoryTransactionsByProjectUuid(uuid))
            );
        }
        projectMapper.deleteByUuid(uuid);
    }

    private void validateProject(Project project) {
        if (!StringUtils.hasText(project.getProjectName())) {
            throw new IllegalArgumentException("Project name is required");
        }
        if (!StringUtils.hasText(project.getProjectNumber())) {
            throw new IllegalArgumentException("Project number is required");
        }
        String normalizedProjectNumber = project.getProjectNumber().trim().toUpperCase();
        project.setProjectNumber(normalizedProjectNumber);
        if (!PROJECT_NUMBER_PATTERN.matcher(normalizedProjectNumber).matches()) {
            throw new IllegalArgumentException("Project number must use format like AB-1234");
        }
        Project existingByNumber = projectMapper.selectByProjectNumber(normalizedProjectNumber);
        if (existingByNumber != null && !sameProject(existingByNumber, project)) {
            throw new IllegalArgumentException("Project number already exists");
        }
        if (!StringUtils.hasText(project.getCustomerId())) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (project.getManagerId() == null) {
            throw new IllegalArgumentException("Manager is required");
        }
        if (project.getRegionId() == null) {
            throw new IllegalArgumentException("Region is required");
        }
        if (project.getProjectTypeId() == null) {
            throw new IllegalArgumentException("Project type is required");
        }
        ProjectType projectType = projectTypeMapper.selectById(project.getProjectTypeId());
        if (projectType == null) {
            throw new IllegalArgumentException("Project type does not exist");
        }
        if (!Boolean.TRUE.equals(projectType.getIsActive())) {
            throw new IllegalArgumentException("Project type is disabled");
        }
    }

    private boolean sameProject(Project existing, Project incoming) {
        return existing != null
                && incoming != null
                && StringUtils.hasText(existing.getUuid())
                && existing.getUuid().equals(incoming.getUuid());
    }

    private void enrichDeletionState(Project project) {
        if (project == null || !StringUtils.hasText(project.getUuid())) {
            return;
        }
        Integer currentStageSortOrder = projectStatusHistoryMapper.selectCurrentStageSortOrderByProjectId(project.getUuid());
        project.setCurrentStageSortOrder(currentStageSortOrder);
        project.setCanDelete(isProjectDeletable(currentStageSortOrder));
    }

    void enrichDeletionStates(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        List<String> projectIds = projects.stream()
                .filter(project -> project != null && StringUtils.hasText(project.getUuid()))
                .map(Project::getUuid)
                .distinct()
                .toList();
        if (projectIds.isEmpty()) {
            return;
        }
        int lockStageSortOrder = systemConfigService.getInt(
                PROJECT_DELETE_LOCK_STAGE_SORT_ORDER_KEY,
                DEFAULT_PROJECT_DELETE_LOCK_STAGE_SORT_ORDER
        );
        Map<String, Integer> sortOrderByProjectId = new HashMap<>();
        List<ProjectStageSortOrder> stageSortOrders = projectStatusHistoryMapper.selectCurrentStageSortOrdersByProjectIds(projectIds);
        if (stageSortOrders != null) {
            for (ProjectStageSortOrder item : stageSortOrders) {
                if (item != null && StringUtils.hasText(item.getProjectId())) {
                    sortOrderByProjectId.put(item.getProjectId(), item.getMaxSortOrder());
                }
            }
        }
        for (Project project : projects) {
            if (project == null || !StringUtils.hasText(project.getUuid())) {
                continue;
            }
            Integer currentStageSortOrder = sortOrderByProjectId.get(project.getUuid());
            project.setCurrentStageSortOrder(currentStageSortOrder);
            project.setCanDelete(isProjectDeletable(currentStageSortOrder, lockStageSortOrder));
        }
    }

    private boolean isProjectDeletable(Integer currentStageSortOrder) {
        if (currentStageSortOrder == null) {
            return true;
        }
        int lockStageSortOrder = systemConfigService.getInt(
                PROJECT_DELETE_LOCK_STAGE_SORT_ORDER_KEY,
                DEFAULT_PROJECT_DELETE_LOCK_STAGE_SORT_ORDER
        );
        return isProjectDeletable(currentStageSortOrder, lockStageSortOrder);
    }

    private boolean isProjectDeletable(Integer currentStageSortOrder, int lockStageSortOrder) {
        return currentStageSortOrder == null || currentStageSortOrder < lockStageSortOrder;
    }
}
