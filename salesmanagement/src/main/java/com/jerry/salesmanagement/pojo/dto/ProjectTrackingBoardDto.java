package com.jerry.salesmanagement.pojo.dto;

import com.jerry.salesmanagement.pojo.Stage;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectTrackingBoardDto {

    private List<Stage> stages = new ArrayList<>();
    private int stalledDaysThreshold;
    private Permissions permissions = new Permissions();
    private Summary summary = new Summary();
    private List<ProjectTrackingCardDto> items = new ArrayList<>();

    @Data
    public static class Permissions {
        private boolean canViewContracts;
        private boolean canViewFinance;
        private boolean canViewInventory;
        private boolean canManageProject;
        private boolean canAuditProjectLists;
        private boolean canAuditWarehouseDocs;
        private boolean canAuditFinanceVouchers;
    }

    @Data
    public static class Summary {
        private int totalProjects;
        private int activeProjects;
        private int completedProjects;
        private int overdueCount;
        private int stalledCount;
        private int warrantyExpiringCount;
        private int warrantyExpiredCount;
        private int pendingAuditProjectCount;
        private BigDecimal totalContractAmount;
        private BigDecimal totalReceivedAmount;
        private BigDecimal totalReceivableAmount;
    }
}
