package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ProjectList implements EntryAuditable {
    private Long id;
    private String uuid;
    private String projectId;
    private String listName;
    private String listType;
    private String customerName;
    private Date entryDate;
    private Long entryUser;
    private String entryUserName;
    private Long auditorUser;
    private String auditorUserName;
    private String pdfAttachmentId;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
}
