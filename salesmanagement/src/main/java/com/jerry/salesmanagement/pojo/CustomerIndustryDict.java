package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class CustomerIndustryDict {
    private Long id;
    private String industryName;
    private Boolean isActive;
}
