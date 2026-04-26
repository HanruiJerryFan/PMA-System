package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class Stage {
    private Long id;
    private String stageCode;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
}
