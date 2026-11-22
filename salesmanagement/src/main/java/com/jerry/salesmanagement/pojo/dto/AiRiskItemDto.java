package com.jerry.salesmanagement.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRiskItemDto {
    private String severity;
    private String category;
    private String title;
    private String detail;
    private String targetRoute;
}
