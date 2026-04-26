package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Attachment {
    private Long id;
    private String uuid;
    private String businessType;
    private String businessUuid;
    private String fileName;
    private String originalFileName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String storagePath;
    private Long uploadedBy;
    private Date uploadedAt;
}
