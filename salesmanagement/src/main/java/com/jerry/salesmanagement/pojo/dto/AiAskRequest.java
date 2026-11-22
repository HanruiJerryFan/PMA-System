package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

@Data
public class AiAskRequest {
    private String question;
    private String projectUuid;
}
