package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class ProductSubcategory {
    private Long id;
    private String code;
    private String name;
    private Integer sortOrder;
    private Boolean isActive;
}
