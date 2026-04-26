package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ProjectStatusHistory {
    private Long id;
    private String uuid;
    private String projectId;
    private Long stageId;
    private String stageName;
    private Date createdAt;
    private Long createdBy;
}
