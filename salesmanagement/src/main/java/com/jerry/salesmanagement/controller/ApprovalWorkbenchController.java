package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalSummaryDto;
import com.jerry.salesmanagement.service.ApprovalWorkbenchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalWorkbenchController {

    @Autowired
    private ApprovalWorkbenchService approvalWorkbenchService;

    @GetMapping("/pending")
    public ApiResponse<PendingApprovalSummaryDto> getPendingApprovals() {
        try {
            return ApiResponse.success(approvalWorkbenchService.getPendingApprovals());
        } catch (Exception e) {
            return ApiResponse.error("Failed to load pending approvals: " + e.getMessage());
        }
    }
}
