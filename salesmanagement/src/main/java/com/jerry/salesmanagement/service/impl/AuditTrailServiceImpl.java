package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.AuditTrailMapper;
import com.jerry.salesmanagement.pojo.AuditTrail;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class AuditTrailServiceImpl implements AuditTrailService {

    private static final String SYSTEM_USERNAME = "System";

    @Autowired
    private AuditTrailMapper auditTrailMapper;

    @Autowired
    private SysUserService sysUserService;

    @Override
    public List<AuditTrail> getAll(Long userId, String module, String action) {
        String canonicalModule = canonicalizeResourceName(module);
        return auditTrailMapper.selectAll(userId, null, action).stream()
                .map(this::normalizeAuditTrail)
                .filter(record -> !isSystemUserId(record.getUserId()))
                .filter(record -> !StringUtils.hasText(canonicalModule) || canonicalModule.equals(record.getModule()))
                .toList();
    }

    @Override
    public AuditTrail getById(Long id) {
        AuditTrail auditTrail = normalizeAuditTrail(auditTrailMapper.selectById(id));
        return auditTrail != null && isSystemUserId(auditTrail.getUserId()) ? null : auditTrail;
    }

    @Override
    public AuditTrail create(AuditTrail auditTrail) {
        normalizeAuditTrail(auditTrail);
        if (shouldSkipAuditTrail(auditTrail)) {
            return auditTrail;
        }
        if (auditTrail.getOccurredAt() == null) {
            auditTrail.setOccurredAt(new Date());
        }
        auditTrailMapper.insert(auditTrail);
        return auditTrail;
    }

    @Override
    public void record(String module, String action, String targetType, String targetId, String detail) {
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setUserId(resolveCurrentUserId());
        auditTrail.setModule(module);
        auditTrail.setAction(action);
        auditTrail.setTargetType(targetType);
        auditTrail.setTargetId(targetId);
        auditTrail.setDetail(detail);
        auditTrail.setOccurredAt(new Date());
        create(auditTrail);
    }

    private AuditTrail normalizeAuditTrail(AuditTrail auditTrail) {
        if (auditTrail == null) {
            return null;
        }
        auditTrail.setModule(canonicalizeResourceName(auditTrail.getModule()));
        auditTrail.setTargetType(canonicalizeResourceName(auditTrail.getTargetType()));
        return auditTrail;
    }

    private String canonicalizeResourceName(String resourceName) {
        if (!StringUtils.hasText(resourceName)) {
            return resourceName;
        }
        return resourceName.trim().toLowerCase(Locale.ROOT);
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (username == null || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }

        SysUser user = sysUserService.getByUsername(username);
        return user != null ? user.getId() : null;
    }

    private boolean shouldSkipAuditTrail(AuditTrail auditTrail) {
        return auditTrail != null && isSystemUserId(auditTrail.getUserId());
    }

    private boolean isSystemUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = sysUserService.getById(userId);
        return user != null && SYSTEM_USERNAME.equalsIgnoreCase(user.getUsername());
    }
}
