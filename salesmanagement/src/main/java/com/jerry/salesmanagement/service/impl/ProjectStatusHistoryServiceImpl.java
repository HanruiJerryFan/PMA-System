package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.ProjectStatusHistoryService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProjectStatusHistoryServiceImpl implements ProjectStatusHistoryService {

    private static final String PROJECT_DELETE_LOCK_STAGE_SORT_ORDER_KEY = "project.delete.lock_stage_sort_order";
    private static final int DEFAULT_PROJECT_DELETE_LOCK_STAGE_SORT_ORDER = 5;

    @Autowired
    private ProjectStatusHistoryMapper mapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private StageMapper stageMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired(required = false)
    private SystemConfigService systemConfigService;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

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
        ValidationContext ctx = validate(history, false, null);
        assertForwardProgression(ctx.project().getUuid(), ctx.stage());
        history.setUuid(UUID.randomUUID().toString());
        if (history.getCreatedAt() == null) {
            history.setCreatedAt(new Date());
        }
        if (history.getCreatedBy() == null && currentUserService != null) {
            history.setCreatedBy(currentUserService.getCurrentUserId());
        }
        mapper.insert(history);
        touchProjectCustomer(ctx.project());
        return mapper.selectByUuid(history.getUuid());
    }

    @Override
    @Transactional
    public ProjectStatusHistory update(ProjectStatusHistory history) {
        if (!StringUtils.hasText(history.getUuid())) {
            throw new IllegalArgumentException("History UUID is required");
        }
        ProjectStatusHistory existing = mapper.selectByUuid(history.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Status history record does not exist");
        }
        if (StringUtils.hasText(history.getProjectId())
                && !Objects.equals(existing.getProjectId(), history.getProjectId())) {
            throw new IllegalArgumentException("项目状态历史记录创建后不可更换所属项目");
        }
        history.setProjectId(existing.getProjectId());

        ValidationContext ctx = validate(history, true, existing);
        Stage oldStage = existing.getStageId() != null ? stageMapper.selectById(existing.getStageId()) : null;
        int lockThreshold = resolveLockSortOrder();
        int oldSort = oldStage != null && oldStage.getSortOrder() != null ? oldStage.getSortOrder() : -1;
        int newSort = ctx.stage().getSortOrder() != null ? ctx.stage().getSortOrder() : -1;
        if (oldSort >= lockThreshold && newSort < lockThreshold && !isSystemOperator()) {
            throw new IllegalArgumentException("已达到删除锁定阈值阶段的历史记录不允许降级回退");
        }

        if (history.getCreatedAt() == null) {
            history.setCreatedAt(existing.getCreatedAt() != null ? existing.getCreatedAt() : new Date());
        }
        mapper.update(history);
        touchProjectCustomer(ctx.project());
        return mapper.selectByUuid(history.getUuid());
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        if (!StringUtils.hasText(uuid)) {
            return;
        }
        ProjectStatusHistory existing = mapper.selectByUuid(uuid);
        if (existing == null) {
            return;
        }
        Stage stage = existing.getStageId() != null ? stageMapper.selectById(existing.getStageId()) : null;
        int lockThreshold = resolveLockSortOrder();
        if (stage != null && stage.getSortOrder() != null && stage.getSortOrder() >= lockThreshold && !isSystemOperator()) {
            throw new IllegalArgumentException("已达到删除锁定阈值阶段的状态历史不允许删除");
        }
        mapper.deleteByUuid(uuid);
    }

    private ValidationContext validate(ProjectStatusHistory history, boolean requireUuid, ProjectStatusHistory existing) {
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
        boolean unchangedHistoricalStage = existing != null && Objects.equals(existing.getStageId(), stage.getId());
        if (!unchangedHistoricalStage && !Boolean.TRUE.equals(stage.getIsActive())) {
            throw new IllegalArgumentException("Stage is disabled");
        }
        return new ValidationContext(project, stage);
    }

    private void assertForwardProgression(String projectId, Stage targetStage) {
        List<ProjectStatusHistory> histories = mapper.selectByProjectId(projectId);
        if (histories == null || histories.isEmpty()) {
            return;
        }
        ProjectStatusHistory latest = histories.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing(ProjectStatusHistory::getCreatedAt, Comparator.nullsFirst(Date::compareTo))
                        .thenComparing(h -> h.getId() == null ? Long.MIN_VALUE : h.getId()))
                .orElse(null);
        if (latest == null) {
            return;
        }
        if (Objects.equals(latest.getStageId(), targetStage.getId())) {
            throw new IllegalArgumentException("项目当前已处于该阶段，请勿重复推进");
        }
        Stage currentStage = latest.getStageId() != null ? stageMapper.selectById(latest.getStageId()) : null;
        if (currentStage != null
                && currentStage.getSortOrder() != null
                && targetStage.getSortOrder() != null
                && targetStage.getSortOrder() < currentStage.getSortOrder()) {
            throw new IllegalArgumentException("项目阶段只允许向前推进，不能回退至早期阶段");
        }
    }

    private int resolveLockSortOrder() {
        if (systemConfigService == null) {
            return DEFAULT_PROJECT_DELETE_LOCK_STAGE_SORT_ORDER;
        }
        int configured = systemConfigService.getInt(
                PROJECT_DELETE_LOCK_STAGE_SORT_ORDER_KEY,
                DEFAULT_PROJECT_DELETE_LOCK_STAGE_SORT_ORDER
        );
        return configured > 0 ? configured : DEFAULT_PROJECT_DELETE_LOCK_STAGE_SORT_ORDER;
    }

    private boolean isSystemOperator() {
        return currentUserService != null && currentUserService.isSystemUser();
    }

    private void touchProjectCustomer(Project project) {
        if (project != null && StringUtils.hasText(project.getCustomerId())) {
            customerService.touchActivity(project.getCustomerId());
        }
    }

    private record ValidationContext(Project project, Stage stage) {
    }
}
