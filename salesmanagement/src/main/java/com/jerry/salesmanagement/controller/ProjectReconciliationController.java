package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.dto.ProjectMaterialReconciliationDto;
import com.jerry.salesmanagement.pojo.dto.ProjectReconciliationOverviewItemDto;
import com.jerry.salesmanagement.service.ProjectMaterialReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectReconciliationController {

    @Autowired
    private ProjectMaterialReconciliationService reconciliationService;

    @GetMapping("/material-reconciliation/summary")
    public ApiResponse<List<ProjectReconciliationOverviewItemDto>> getOverview() {
        try {
            return ApiResponse.success(reconciliationService.getOverview());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query material reconciliation summary: " + e.getMessage());
        }
    }

    @GetMapping("/{projectUuid}/material-reconciliation")
    public ApiResponse<ProjectMaterialReconciliationDto> getProjectReconciliation(@PathVariable String projectUuid) {
        try {
            return ApiResponse.success(reconciliationService.getProjectReconciliation(projectUuid));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project material reconciliation: " + e.getMessage());
        }
    }
}
