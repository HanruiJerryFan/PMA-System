package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class StageServiceImpl implements StageService {

    private static final Pattern STAGE_CODE_PATTERN = Pattern.compile("^\\d+\\.\\d+$");

    @Autowired
    private StageMapper mapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public Stage getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Stage> getAll() {
        return mapper.selectAll();
    }

    @Override
    @Transactional
    public Stage create(Stage stage) {
        validate(stage);
        mapper.insert(stage);
        return stage;
    }

    @Override
    @Transactional
    public Stage update(Stage stage) {
        if (stage.getId() == null) {
            throw new IllegalArgumentException("Stage ID is required");
        }
        Stage existing = mapper.selectById(stage.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Stage does not exist");
        }
        validate(stage);
        mapper.update(stage);
        return mapper.selectById(stage.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "项目阶段",
                    new ReferenceCheckMapper.ReferenceUsage("项目状态历史", referenceCheckMapper.countProjectStatusHistoryByStageId(id))
            );
        }
        mapper.deleteById(id);
    }

    private void validate(Stage stage) {
        if (!StringUtils.hasText(stage.getStageCode())) {
            throw new IllegalArgumentException("Stage code is required");
        }
        String code = stage.getStageCode().trim();
        if (!STAGE_CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Stage code must use format like 1.1");
        }
        stage.setStageCode(code);
        if (!StringUtils.hasText(stage.getDescription())) {
            throw new IllegalArgumentException("Stage description is required");
        }
        stage.setDescription(stage.getDescription().trim());
        if (stage.getSortOrder() == null) {
            throw new IllegalArgumentException("Sort order is required");
        }
        if (stage.getIsActive() == null) {
            throw new IllegalArgumentException("Stage active flag is required");
        }
        List<Stage> allStages = mapper.selectAll();
        for (Stage other : allStages) {
            if (other == null || Objects.equals(other.getId(), stage.getId())) {
                continue;
            }
            if (other.getStageCode() != null && other.getStageCode().trim().equalsIgnoreCase(code)) {
                throw new IllegalArgumentException("Stage code already exists: " + code);
            }
            if (Boolean.TRUE.equals(stage.getIsActive())
                    && Boolean.TRUE.equals(other.getIsActive())
                    && Objects.equals(other.getSortOrder(), stage.getSortOrder())) {
                throw new IllegalArgumentException("启用阶段的排序值不能重复: " + stage.getSortOrder());
            }
        }
    }
}
