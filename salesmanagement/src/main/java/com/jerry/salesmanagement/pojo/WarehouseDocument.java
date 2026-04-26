package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class WarehouseDocument {
    private String docNumber;
    private String docType;
    private String businessCategory;
    private String projectId;
    private Long warehouseId;
    private String counterpartyCustomerId;
    private String counterpartyName;
    private String counterpartyAddress;
    private String counterpartyContact;
    private String contractNumber;
    private Date docDate;
    private Double totalAmount;
    private String remark;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
    private List<WarehouseDocumentItem> items = new ArrayList<>();
}
