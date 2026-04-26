package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class ProductBand {
    private Long id;
    private String code;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
}
