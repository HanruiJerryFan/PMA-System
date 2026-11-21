package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.dto.ProjectMaterialReconciliationDto;
import com.jerry.salesmanagement.pojo.dto.ProjectReconciliationOverviewItemDto;

import java.util.List;

public interface ProjectMaterialReconciliationService {
    List<ProjectReconciliationOverviewItemDto> getOverview();

    ProjectMaterialReconciliationDto getProjectReconciliation(String projectUuid);
}
