package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectListAggregateView {
    private String projectId;
    private String aggregateType;
    private String listName;
    private String customerName;
    private int sourceListCount;
    private int itemCount;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private List<ProjectListAggregateSource> sourceLists = new ArrayList<>();
    private List<ProjectListAggregateItem> items = new ArrayList<>();
}
