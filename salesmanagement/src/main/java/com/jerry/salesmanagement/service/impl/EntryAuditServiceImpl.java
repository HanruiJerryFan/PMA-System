package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.pojo.EntryAuditable;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.EntryAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntryAuditServiceImpl implements EntryAuditService {

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public void applyCreate(EntryAuditable record) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        record.setEntryUser(currentUserId);
        record.setAuditorUser(null);
    }

    @Override
    public void applyUpdate(EntryAuditable incoming, EntryAuditable existing) {
        if (canOverrideEntryAuditUsers()) {
            return;
        }
        if (existing != null && existing.getAuditorUser() != null) {
            throw new IllegalArgumentException("已审核单据不可修改，请联系具备录入审核维护权限的管理员");
        }
        if (existing != null) {
            incoming.setEntryUser(existing.getEntryUser());
            incoming.setAuditorUser(existing.getAuditorUser());
        }
    }

    @Override
    public void assertCanDelete(EntryAuditable existing) {
        if (existing != null && existing.getAuditorUser() != null && !canOverrideEntryAuditUsers()) {
            throw new IllegalArgumentException("已审核单据不可删除，请联系具备录入审核维护权限的管理员");
        }
    }

    @Override
    public void applyAudit(EntryAuditable record) {
        if (record == null) {
            throw new IllegalArgumentException("Record is required");
        }
        if (record.getAuditorUser() != null) {
            throw new IllegalArgumentException("单据已审核，不可重复审核");
        }
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (record.getEntryUser() != null
                && record.getEntryUser().equals(currentUserId)
                && !canOverrideEntryAuditUsers()) {
            throw new IllegalArgumentException("录入人与审核人不能为同一人");
        }
        record.setAuditorUser(currentUserId);
    }

    @Override
    public boolean canOverrideEntryAuditUsers() {
        return currentUserService.isSystemOrAdmin();
    }
}
