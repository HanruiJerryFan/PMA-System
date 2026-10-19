package com.jerry.salesmanagement.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class WarehouseDocument implements EntryAuditable {
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
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String sourceRefType;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String sourceRefId;
    private Long entryUser;
    private String entryUserName;
    private Long auditorUser;
    private String auditorUserName;
    private String remark;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
    private List<WarehouseDocumentItem> items = new ArrayList<>();
}
