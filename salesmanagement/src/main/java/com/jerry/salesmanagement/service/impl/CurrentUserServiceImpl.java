package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private static final String SYSTEM_USERNAME = "System";
    private static final String SYSTEM_ROLE = "ROLE_System";
    private static final String ADMIN_ROLE = "ROLE_管理员";

    @Autowired
    private SysUserService sysUserService;

    @Override
    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (!StringUtils.hasText(username) || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }

        return sysUserService.getByUsername(username);
    }

    @Override
    public Long getCurrentUserId() {
        SysUser currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.getId();
    }

    @Override
    public Long requireCurrentUserId() {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("Current user is required");
        }
        return currentUserId;
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> currentAuthorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return Arrays.stream(authorities).anyMatch(currentAuthorities::contains);
    }

    @Override
    public boolean isSystemOrAdmin() {
        SysUser currentUser = getCurrentUser();
        return (currentUser != null && SYSTEM_USERNAME.equalsIgnoreCase(currentUser.getUsername()))
                || hasAnyAuthority(SYSTEM_ROLE, ADMIN_ROLE, "document.entry-audit.manage");
    }
}
