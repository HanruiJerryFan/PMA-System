package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.LoginRecord;
import com.jerry.salesmanagement.pojo.dto.PageResult;

import java.util.Date;
import java.util.List;

public interface LoginRecordService {
    LoginRecord getById(Long id);
    List<LoginRecord> getAll();
    List<LoginRecord> getByUserId(Long userId);
    PageResult<LoginRecord> getPage(Long userId, int page, int pageSize);
    LoginRecord create(LoginRecord record);
    LoginRecord update(LoginRecord record);
    void touchSession(Long loginRecordId, Date lastSeenAt);
    void completeSession(Long loginRecordId, Date loginEndTime, String logoutReason);
    int reconcileTimedOutSessions(Date cutoffTime);
}
