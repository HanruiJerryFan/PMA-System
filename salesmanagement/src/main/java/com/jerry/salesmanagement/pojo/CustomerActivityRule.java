package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class CustomerActivityRule {
    private Long id;
    private String statusCode;
    private Integer minIdleDays;
    private Integer maxIdleDays;
    private Integer sortOrder;
    private Boolean isActive;
}
