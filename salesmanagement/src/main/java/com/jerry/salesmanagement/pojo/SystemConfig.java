package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class SystemConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private Date createdAt;
    private Date updatedAt;
}
