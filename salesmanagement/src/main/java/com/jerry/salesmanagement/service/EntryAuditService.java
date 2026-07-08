package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.EntryAuditable;

public interface EntryAuditService {
    void applyCreate(EntryAuditable record);

    void applyUpdate(EntryAuditable incoming, EntryAuditable existing);

    void applyAudit(EntryAuditable record);

    boolean canOverrideEntryAuditUsers();
}
