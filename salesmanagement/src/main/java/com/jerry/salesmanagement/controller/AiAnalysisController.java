package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.dto.AiAnalysisResponse;
import com.jerry.salesmanagement.pojo.dto.AiAskRequest;
import com.jerry.salesmanagement.pojo.dto.AiStatusDto;
import com.jerry.salesmanagement.service.AiBusinessAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    @Autowired
    private AiBusinessAnalysisService aiBusinessAnalysisService;

    @GetMapping("/status")
    public ApiResponse<AiStatusDto> getStatus() {
        try {
            return ApiResponse.success(aiBusinessAnalysisService.getStatus());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query AI analysis status: " + e.getMessage());
        }
    }

    @PostMapping("/briefing")
    public ApiResponse<AiAnalysisResponse> generateBriefing(
            @RequestParam(required = false) String projectUuid,
            @RequestBody(required = false) AiAskRequest body
    ) {
        try {
            String resolvedProjectUuid = projectUuid != null ? projectUuid : (body != null ? body.getProjectUuid() : null);
            return ApiResponse.success(aiBusinessAnalysisService.generateBriefing(resolvedProjectUuid));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Failed to generate AI briefing: " + e.getMessage());
        }
    }

    @PostMapping("/ask")
    public ApiResponse<AiAnalysisResponse> ask(@RequestBody AiAskRequest request) {
        try {
            return ApiResponse.success(aiBusinessAnalysisService.askQuestion(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Failed to answer AI question: " + e.getMessage());
        }
    }
}
