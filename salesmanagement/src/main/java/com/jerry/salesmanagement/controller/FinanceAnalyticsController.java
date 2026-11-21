package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.dto.FinanceAnalyticsOverviewDto;
import com.jerry.salesmanagement.service.FinanceAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/analytics")
public class FinanceAnalyticsController {

    @Autowired
    private FinanceAnalyticsService financeAnalyticsService;

    @GetMapping("/overview")
    public ApiResponse<FinanceAnalyticsOverviewDto> getOverview() {
        try {
            return ApiResponse.success(financeAnalyticsService.getOverview());
        } catch (Exception e) {
            return ApiResponse.error("Failed to load finance analytics overview: " + e.getMessage());
        }
    }
}
