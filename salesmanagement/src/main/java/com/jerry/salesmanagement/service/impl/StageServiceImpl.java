package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class StageServiceImpl implements StageService {

    private static final Pattern STAGE_CODE_PATTERN = Pattern.compile("^\\d+\\.\\d+$");

    @Autowired
    private StageMapper mapper;

    @Override
    public Stage getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Stage> getAll() {
        return mapper.selectAll();
    }

    @Override
    public Stage create(Stage stage) {
        validate(stage);
        mapper.insert(stage);
        return stage;
    }

    @Override
    public Stage update(Stage stage) {
        validate(stage);
        mapper.update(stage);
        return mapper.selectById(stage.getId());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void validate(Stage stage) {
        if (!StringUtils.hasText(stage.getStageCode())) {
            throw new IllegalArgumentException("Stage code is required");
        }
        if (!STAGE_CODE_PATTERN.matcher(stage.getStageCode().trim()).matches()) {
            throw new IllegalArgumentException("Stage code must use format like 1.1");
        }
        if (!StringUtils.hasText(stage.getDescription())) {
            throw new IllegalArgumentException("Stage description is required");
        }
        if (stage.getSortOrder() == null) {
            throw new IllegalArgumentException("Sort order is required");
        }
        if (stage.getIsActive() == null) {
            throw new IllegalArgumentException("Stage active flag is required");
        }
    }
}
