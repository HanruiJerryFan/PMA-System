package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class FinanceSubject {
    private Long id;
    private Integer subjectLevel;
    private String subjectCode;
    private String subjectName;
    private Integer sortOrder;
}
