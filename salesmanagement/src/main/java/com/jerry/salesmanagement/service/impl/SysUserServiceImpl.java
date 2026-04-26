package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SysUserMapper;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SysUserServiceImpl implements SysUserService {

    private static final Set<String> STATUS_VALUES = Set.of("ACTIVE", "DISABLED", "CLOSED");

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        validateForUpdate(user);
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

    private void validateForUpdate(SysUser user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID is required");
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
