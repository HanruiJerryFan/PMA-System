package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.LoginRecordMapper;
import com.jerry.salesmanagement.pojo.LoginRecord;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.LoginRecordService;
import com.jerry.salesmanagement.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class LoginRecordServiceImpl implements LoginRecordService {

    private static final String SYSTEM_USERNAME = "System";

    @Autowired
    private LoginRecordMapper loginRecordMapper;

    @Autowired
    private SysUserService sysUserService;

    @Value("${server.servlet.session.timeout:900}")
    private int sessionTimeoutSeconds;

    @Override
    public LoginRecord getById(Long id) {
        LoginRecord record = loginRecordMapper.selectById(id);
        return record != null && isSystemUserId(record.getUserId()) ? null : record;
    }

    @Override
    public List<LoginRecord> getAll() {
        return loginRecordMapper.selectAll().stream()
                .filter(record -> !isSystemUserId(record.getUserId()))
                .toList();
    }

    @Override
    public List<LoginRecord> getByUserId(Long userId) {
        if (isSystemUserId(userId)) {
            return List.of();
        }
        return loginRecordMapper.selectByUserId(userId);
    }

    @Override
    public LoginRecord create(LoginRecord record) {
        if (record == null || isSystemUserId(record.getUserId())) {
            return record;
        }
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(record.getLoginStartTime());
        }
        if (record.getLastSeenAt() == null) {
            record.setLastSeenAt(record.getLoginStartTime());
        }
        loginRecordMapper.insert(record);
        return record;
    }

    @Override
    public LoginRecord update(LoginRecord record) {
        loginRecordMapper.update(record);
        return record;
    }

    @Override
    public void touchSession(Long loginRecordId, Date lastSeenAt) {
        if (loginRecordId == null || lastSeenAt == null) {
            return;
        }
        LoginRecord record = new LoginRecord();
        record.setId(loginRecordId);
        record.setLastSeenAt(lastSeenAt);
        loginRecordMapper.update(record);
    }

    @Override
    public void completeSession(Long loginRecordId, Date loginEndTime, String logoutReason) {
        if (loginRecordId == null || loginEndTime == null) {
            return;
        }
        LoginRecord existing = loginRecordMapper.selectById(loginRecordId);
        if (existing == null
                || isSystemUserId(existing.getUserId())
                || existing.getLoginStartTime() == null
                || existing.getLoginEndTime() != null) {
            return;
        }

        LoginRecord update = new LoginRecord();
        update.setId(loginRecordId);
        update.setLoginEndTime(loginEndTime);
        update.setLastSeenAt(loginEndTime);
        update.setDurationSeconds(Math.max(0L, (loginEndTime.getTime() - existing.getLoginStartTime().getTime()) / 1000L));
        update.setLogoutReason(logoutReason);
        loginRecordMapper.update(update);
    }

    @Override
    public int reconcileTimedOutSessions(Date cutoffTime) {
        List<LoginRecord> timedOutRecords = loginRecordMapper.selectTimedOutActive(cutoffTime);
        for (LoginRecord record : timedOutRecords) {
            Date endTime = record.getLastSeenAt() != null ? record.getLastSeenAt() : cutoffTime;
            completeSession(record.getId(), endTime, "TIMEOUT");
        }
        return timedOutRecords.size();
    }

    @Scheduled(fixedDelayString = "${app.login-record.reconcile-interval-ms:60000}")
    public void reconcileTimedOutSessionsOnSchedule() {
        long timeoutMillis = Math.max(60L, sessionTimeoutSeconds) * 1000L;
        Date cutoffTime = new Date(System.currentTimeMillis() - timeoutMillis);
        reconcileTimedOutSessions(cutoffTime);
    }

    private boolean isSystemUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = sysUserService.getById(userId);
        return user != null && SYSTEM_USERNAME.equalsIgnoreCase(user.getUsername());
    }
}
