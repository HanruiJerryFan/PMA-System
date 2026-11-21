package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.AuditTrail;
import com.jerry.salesmanagement.pojo.dto.PageResult;

import java.util.List;

public interface AuditTrailService {
    List<AuditTrail> getAll(Long userId, String module, String action);

    PageResult<AuditTrail> getPage(Long userId, String module, String action, int page, int pageSize);

    AuditTrail getById(Long id);

    AuditTrail create(AuditTrail auditTrail);

    void record(String module, String action, String targetType, String targetId, String detail);
}
