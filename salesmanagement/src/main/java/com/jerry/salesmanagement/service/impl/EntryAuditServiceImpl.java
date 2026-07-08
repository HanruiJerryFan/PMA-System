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
        incoming.setEntryUser(existing.getEntryUser());
        incoming.setAuditorUser(existing.getAuditorUser());
    }

    @Override
    public void applyAudit(EntryAuditable record) {
        record.setAuditorUser(currentUserService.requireCurrentUserId());
    }

    @Override
    public boolean canOverrideEntryAuditUsers() {
        return currentUserService.isSystemOrAdmin();
    }
}
