package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class WarehouseDocumentCustomerOption {
    private String uuid;
    private String customerCode;
    private String customerName;
    private Long customerTypeId;
    private String customerTypeCode;
    private String customerTypeName;
    private String officeAddress;
    private String registerAddress;
}
