package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class Warehouse {
    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseAddress;
    private String contactName;
    private String contactPhone;
    private String remark;
}
