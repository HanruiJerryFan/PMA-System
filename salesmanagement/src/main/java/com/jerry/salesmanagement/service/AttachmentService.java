package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Attachment;

import java.util.List;

public interface AttachmentService {
    Attachment getByUuid(String uuid);
    List<Attachment> getAll();
    List<Attachment> getByBusiness(String businessType, String businessUuid);
    Attachment create(Attachment attachment);
    Attachment update(Attachment attachment);
    void delete(String uuid);
}
