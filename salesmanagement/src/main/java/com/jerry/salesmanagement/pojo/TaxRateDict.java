package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxRateDict {
    private Long id;
    private BigDecimal rate;
    private String label;
    private Integer sortOrder;
    private Boolean isActive;
}
