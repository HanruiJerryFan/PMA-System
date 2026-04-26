package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class CodeSequence {
    private String sequenceKey;
    private Integer currentValue;
    private String description;
    private Date createTime;
    private Date updateTime;
}
