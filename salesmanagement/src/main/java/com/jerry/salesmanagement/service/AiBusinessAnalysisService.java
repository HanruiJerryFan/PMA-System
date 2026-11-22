package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.dto.AiAnalysisResponse;
import com.jerry.salesmanagement.pojo.dto.AiAskRequest;
import com.jerry.salesmanagement.pojo.dto.AiStatusDto;

public interface AiBusinessAnalysisService {
    AiStatusDto getStatus();

    AiAnalysisResponse generateBriefing(String projectUuid);

    AiAnalysisResponse askQuestion(AiAskRequest request);
}
