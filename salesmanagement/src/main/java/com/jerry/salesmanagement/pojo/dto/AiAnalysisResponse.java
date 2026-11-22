package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AiAnalysisResponse {
    private String mode;
    private boolean fallbackUsed;
    private String fallbackReason;
    private String model;
    private String projectUuid;
    private List<String> authorizedScopes = new ArrayList<>();
    private String summary;
    private List<String> highlights = new ArrayList<>();
    private List<AiRiskItemDto> risks = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private String answer;
    private Map<String, Object> evidence = new LinkedHashMap<>();
}
