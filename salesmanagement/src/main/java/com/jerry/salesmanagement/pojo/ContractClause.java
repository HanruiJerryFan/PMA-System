package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractClause {
    private Long id;
    private String uuid;
    private String contractBasicInfoId; // 关联 contract_basic_info.uuid
    private Long clauseTypeId;          // 关联 clause_type.id
    private String clauseDescription;
    private BigDecimal clauseAmount;
}
