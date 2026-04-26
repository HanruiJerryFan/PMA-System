package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class ProjectType {
    private Long id;
    private String code;
    private String name;
    private Boolean isActive;
}
