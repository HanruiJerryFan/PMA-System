package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class ContractType {
    private Long id;
    private String typeName; // 合同类别名称
    private String typeCode; // 合同类别代号
}
