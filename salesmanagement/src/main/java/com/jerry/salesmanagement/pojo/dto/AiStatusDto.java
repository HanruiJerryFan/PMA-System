package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiStatusDto {
    private boolean enabled;
    private boolean configured;
    private boolean localEndpoint;
    private String provider;
    private String model;
    private boolean maskSensitiveNames;
    private String mode;
    private List<String> authorizedScopes = new ArrayList<>();
}
