package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SysUserMapper;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.SysPermissionService;
import com.jerry.salesmanagement.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class SysUserServiceImpl implements SysUserService {

    private static final String SYSTEM_USERNAME = "System";
    private static final Set<String> STATUS_VALUES = Set.of("ACTIVE", "DISABLED", "CLOSED");

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private SysPermissionService sysPermissionService;

    @Override
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    @Override
    public SysUser getByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }

    @Override
    public List<SysUser> getAll() {
        return sysUserMapper.selectAll();
    }

    @Override
    public SysUser create(SysUser user) {
        validateForCreate(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (!StringUtils.hasText(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
        if (user.getForcePasswordChange() == null) {
            user.setForcePasswordChange(Boolean.FALSE);
        }
        sysUserMapper.insert(user);
        return user;
    }

    @Override
    public SysUser update(SysUser user) {
        SysUser existing = validateForUpdate(user);
        validateActorForUserMutation(user, existing);
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserMapper.update(user);
        return user;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword, Long operatorId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (!StringUtils.hasText(oldPassword)) {
            throw new IllegalArgumentException("Current password is required");
        }
        validateNewPassword(newPassword);

        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("User does not exist");
        }
        if (!passwordEncoder.matches(oldPassword, existing.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword.trim()));
        update.setForcePasswordChange(Boolean.FALSE);
        update.setUpdateUser(operatorId);
        sysUserMapper.update(update);
    }

    @Override
    public String adminResetPassword(Long userId, Long operatorId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("User does not exist");
        }
        if (!"ACTIVE".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalArgumentException("Only active users can be reset");
        }
        if (isSystemUser(existing) && hasAuthenticatedActor() && !isActorSystem()) {
            throw new IllegalArgumentException("内置 System 账号不可由其他账号重置密码");
        }
        assertActorDominatesTargetUser(existing.getId());

        String temporaryPassword = generateTemporaryPassword();
        SysUser update = new SysUser();
        update.setId(existing.getId());
        update.setPassword(passwordEncoder.encode(temporaryPassword));
        update.setForcePasswordChange(Boolean.TRUE);
        update.setUpdateUser(operatorId);
        sysUserMapper.update(update);
        return temporaryPassword;
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        SysUser existing = sysUserMapper.selectById(id);
        if (isSystemUser(existing)) {
            throw new IllegalArgumentException("内置 System 账号不可删除");
        }
        Long actorId = currentActorId();
        if (actorId != null && Objects.equals(actorId, id)) {
            throw new IllegalArgumentException("不能删除当前登录账号");
        }
        if (existing != null) {
            assertActorDominatesTargetUser(existing.getId());
        }
        sysUserMapper.deleteById(id);
    }

    private void validateForCreate(SysUser user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }
        validateNewPassword(user.getPassword());
        SysUser existing = sysUserMapper.selectByUsername(user.getUsername().trim());
        if (existing != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        validateStatus(user.getStatus());
    }

    private SysUser validateForUpdate(SysUser user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        SysUser currentRecord = sysUserMapper.selectById(user.getId());
        if (currentRecord == null) {
            throw new IllegalArgumentException("User does not exist");
        }
        if (StringUtils.hasText(user.getUsername())) {
            SysUser existing = sysUserMapper.selectByUsername(user.getUsername().trim());
            if (existing != null && !user.getId().equals(existing.getId())) {
                throw new IllegalArgumentException("Username already exists");
            }
        }
        if (StringUtils.hasText(user.getStatus())) {
            validateStatus(user.getStatus());
        }
        return currentRecord;
    }

    private void validateActorForUserMutation(SysUser incoming, SysUser existing) {
        boolean actorPresent = hasAuthenticatedActor();
        boolean actorIsSystem = isActorSystem();
        if (isSystemUser(existing)) {
            if (actorPresent && !actorIsSystem) {
                throw new IllegalArgumentException("内置 System 账号不可由其他账号修改");
            }
            if (StringUtils.hasText(incoming.getUsername())
                    && !SYSTEM_USERNAME.equalsIgnoreCase(incoming.getUsername().trim())) {
                throw new IllegalArgumentException("内置 System 账号用户名不可修改");
            }
            if (StringUtils.hasText(incoming.getStatus())
                    && !"ACTIVE".equalsIgnoreCase(incoming.getStatus().trim())) {
                throw new IllegalArgumentException("内置 System 账号不可禁用或关闭");
            }
        }
        Long actorId = currentActorId();
        if (actorId != null
                && Objects.equals(actorId, existing.getId())
                && StringUtils.hasText(incoming.getStatus())
                && !"ACTIVE".equalsIgnoreCase(incoming.getStatus().trim())) {
            throw new IllegalArgumentException("不能禁用或关闭当前登录账号");
        }
        if (StringUtils.hasText(incoming.getPassword())
                && (actorId == null || !Objects.equals(actorId, existing.getId()))) {
            assertActorDominatesTargetUser(existing.getId());
        }
    }

    private void assertActorDominatesTargetUser(Long targetUserId) {
        if (currentUserService == null
                || sysPermissionService == null
                || currentUserService.getCurrentUser() == null
                || currentUserService.isSystemUser()) {
            return;
        }
        for (String permissionCode : sysPermissionService.getPermissionCodesByUserId(targetUserId)) {
            if (StringUtils.hasText(permissionCode) && !currentUserService.hasAnyAuthority(permissionCode.trim())) {
                throw new IllegalArgumentException("不能操作权限超出当前账号范围的用户");
            }
        }
    }

    private boolean hasAuthenticatedActor() {
        return currentUserService != null && currentUserService.getCurrentUser() != null;
    }

    private boolean isActorSystem() {
        return currentUserService != null && currentUserService.isSystemUser();
    }

    private Long currentActorId() {
        if (currentUserService == null) {
            return null;
        }
        SysUser actor = currentUserService.getCurrentUser();
        return actor == null ? null : actor.getId();
    }

    private boolean isSystemUser(SysUser user) {
        return user != null
                && StringUtils.hasText(user.getUsername())
                && SYSTEM_USERNAME.equalsIgnoreCase(user.getUsername().trim());
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return;
        }
        if (!STATUS_VALUES.contains(status.trim())) {
            throw new IllegalArgumentException("Status must be ACTIVE, DISABLED, or CLOSED");
        }
    }

    private void validateNewPassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Password is required");
        }
        if (password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    private String generateTemporaryPassword() {
        return "Tmp@" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
