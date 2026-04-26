package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.AttachmentMapper;
import com.jerry.salesmanagement.pojo.Attachment;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private static final Set<String> BUSINESS_TYPES = Set.of(
            "projects",
            "project-lists",
            "finance-vouchers",
            "contracts",
            "customers",
            "warehouse-documents",
            "other"
    );

    @Autowired
    private AttachmentMapper mapper;

    @Autowired
    private CustomerService customerService;

    @Override
    public Attachment getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<Attachment> getAll() {
        return mapper.selectAll();
    }

    @Override
    public List<Attachment> getByBusiness(String businessType, String businessUuid) {
        return mapper.selectByBusiness(businessType, businessUuid);
    }

    @Override
    @Transactional
    public Attachment create(Attachment attachment) {
        validateAttachment(attachment);
        if (!StringUtils.hasText(attachment.getUuid())) {
            attachment.setUuid(UUID.randomUUID().toString());
        }
        mapper.insert(attachment);
        touchCustomerAttachment(attachment);
        return attachment;
    }

    @Override
    @Transactional
    public Attachment update(Attachment attachment) {
        Attachment existing = mapper.selectByUuid(attachment.getUuid());
        if (existing != null) {
            if (!StringUtils.hasText(attachment.getBusinessType())) {
                attachment.setBusinessType(existing.getBusinessType());
            }
            if (!StringUtils.hasText(attachment.getBusinessUuid())) {
                attachment.setBusinessUuid(existing.getBusinessUuid());
            }
        }
        validateAttachment(attachment);
        mapper.updateByUuid(attachment);
        touchCustomerAttachment(attachment);
        return attachment;
    }

    @Override
    public void delete(String uuid) {
        mapper.deleteByUuid(uuid);
    }

    private void validateAttachment(Attachment attachment) {
        if (!StringUtils.hasText(attachment.getBusinessType())
                || !BUSINESS_TYPES.contains(attachment.getBusinessType())) {
            throw new IllegalArgumentException("Business type is invalid");
        }
        if (!StringUtils.hasText(attachment.getFileName())) {
            throw new IllegalArgumentException("File name is required");
        }
        if (!StringUtils.hasText(attachment.getStoragePath())) {
            throw new IllegalArgumentException("Storage path is required");
        }
        if (attachment.getFileSize() != null && attachment.getFileSize() < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
    }

    private void touchCustomerAttachment(Attachment attachment) {
        if ("customers".equals(attachment.getBusinessType()) && StringUtils.hasText(attachment.getBusinessUuid())) {
            customerService.touchActivity(attachment.getBusinessUuid());
        }
    }
}
