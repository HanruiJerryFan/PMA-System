package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Project {
    private Long id;
    private String uuid;
    private String projectNumber;
    private String projectName;
    private String customerId;
    private Long managerId;
    private Long participant1UserId;
    private Long participant2UserId;
    private Long participant3UserId;
    private Integer regionId;
    private Long projectTypeId;
    private Date warrantyUntil;
    private Date planStartTime;
    private Date planEndTime;
    private String salesContractAttachmentUuid;
    private Date createTime;
    private Long createUser;
    private Date updateTime;
    private Long updateUser;
    private Integer currentStageSortOrder;
    private Boolean canDelete;
}
