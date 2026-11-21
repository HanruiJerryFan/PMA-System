package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingBoardDto;
import com.jerry.salesmanagement.service.ProjectTrackingBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectTrackingController {

    @Autowired
    private ProjectTrackingBoardService projectTrackingBoardService;

    @GetMapping("/tracking-board")
    public ApiResponse<ProjectTrackingBoardDto> getTrackingBoard(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) Long projectTypeId,
            @RequestParam(required = false) String riskTag
    ) {
        try {
            return ApiResponse.success(
                    projectTrackingBoardService.getTrackingBoard(keyword, managerId, projectTypeId, riskTag)
            );
        } catch (Exception e) {
            return ApiResponse.error("Failed to load project tracking board: " + e.getMessage());
        }
    }
}
