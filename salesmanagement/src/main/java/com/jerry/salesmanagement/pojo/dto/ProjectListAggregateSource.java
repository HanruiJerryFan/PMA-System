package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ProjectListAggregateSource {
    private String uuid;
    private String listName;
    private String listType;
    private Date entryDate;
    private Long entryUser;
    private String entryUserName;
    private Long auditorUser;
    private String auditorUserName;
}
