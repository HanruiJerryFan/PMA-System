package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ProjectTrackingCardDto {
    private String uuid;
    private String projectNumber;
    private String projectName;
    private String customerId;
    private String customerName;
    private Long managerId;
    private String managerName;
    private Integer regionId;
    private String regionName;
    private Long projectTypeId;
    private String projectTypeName;
    private Date planStartTime;
    private Date planEndTime;
    private Date warrantyUntil;
    private Boolean canDelete;

    private Long currentStageId;
    private String currentStageCode;
    private String currentStageName;
    private Integer currentStageSortOrder;
    private Date stageEnteredAt;
    private Long stageStayDays;
    private Long nextStageId;
    private String nextStageCode;
    private String nextStageName;
    private boolean completed;

    private boolean overdue;
    private Long daysToPlanEnd;
    private String warrantyStatus;
    private Long daysToWarrantyEnd;

    private Integer listCount;
    private Integer pendingListCount;
    private Integer contractCount;
    private BigDecimal contractAmount;
    private BigDecimal receivedAmount;
    private BigDecimal expenseAmount;
    private BigDecimal bookedExpenseAmount;
    private BigDecimal receivableAmount;
    private BigDecimal payableAmount;
    private BigDecimal collectionRate;
    private BigDecimal estimatedGrossProfit;
    private BigDecimal estimatedGrossMargin;
    private BigDecimal outboundAmount;
    private Integer pendingWarehouseDocCount;
    private Integer pendingFinanceVoucherCount;
    private Integer totalPendingAuditCount;

    private List<String> riskTags = new ArrayList<>();
}
