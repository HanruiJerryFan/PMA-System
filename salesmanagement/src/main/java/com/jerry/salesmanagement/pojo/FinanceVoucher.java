package com.jerry.salesmanagement.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
public class FinanceVoucher implements EntryAuditable {
    private Long id;
    private String uuid;
    private String voucherNo;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate occurredOn;
    private String projectId;
    private Long level1SubjectId;
    private Long level2SubjectId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String level1SubjectName;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String level2SubjectName;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String level2SubjectCode;
    private String summary;
    private String transactionDirection;
    private BigDecimal taxRate;
    private String counterpartyCustomerId;
    private String counterpartyNameSnapshot;
    private String invoiceStatus;
    private String invoiceType;
    private String invoiceNo;
    private BigDecimal invoiceAmount;
    private BigDecimal actualIncomeAmount;
    private BigDecimal actualExpenseAmount;
    private BigDecimal bookedAmount;
    private Boolean isCompleted;
    private Long entryUser;
    private String entryUserName;
    private Long auditorUser;
    private String auditorUserName;
    private String remark;
    private Date createdAt;
    private Long createdBy;
    private Date updatedAt;
    private Long updatedBy;
}
