package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PendingApprovalSummaryDto {
    private int totalPendingCount;
    private int projectListCount;
    private int warehouseDocCount;
    private int financeVoucherCount;
    private boolean canAuditProjectLists;
    private boolean canAuditWarehouseDocs;
    private boolean canAuditFinanceVouchers;
    private List<PendingApprovalItemDto> projectLists = new ArrayList<>();
    private List<PendingApprovalItemDto> warehouseDocs = new ArrayList<>();
    private List<PendingApprovalItemDto> financeVouchers = new ArrayList<>();
}
