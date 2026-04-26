package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
@PreAuthorize("hasAuthority('permission.users.manage')")
public class SysUserController {

    private final SysUserService sysUserService;
    private final AuditTrailService auditTrailService;

    public SysUserController(SysUserService sysUserService, AuditTrailService auditTrailService) {
        this.sysUserService = sysUserService;
        this.auditTrailService = auditTrailService;
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<SysUser> getUserById(@PathVariable Long id) {
        try {
            SysUser user = sysUserService.getById(id);
            if (user == null) {
                return ApiResponse.notFound("User not found");
            }
            return ApiResponse.success(sanitizeUser(user));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query user: " + e.getMessage());
        }
    }

    @GetMapping("/username/{username}")
    public ApiResponse<SysUser> getUserByUsername(@PathVariable String username) {
        try {
            SysUser user = sysUserService.getByUsername(username);
            if (user == null) {
                return ApiResponse.notFound("User not found");
            }
            return ApiResponse.success(sanitizeUser(user));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query user: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> getUserOptions() {
        try {
            List<Map<String, Object>> options = sysUserService.getAll().stream()
                    .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                    .map(user -> Map.<String, Object>of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "realName", user.getRealName() == null ? "" : user.getRealName(),
                            "status", user.getStatus()
                    ))
                    .collect(Collectors.toList());
            return ApiResponse.success(options);
        } catch (Exception e) {
            return ApiResponse.error("Failed to query user options: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<SysUser>> getAllUsers() {
        try {
            return ApiResponse.success(sysUserService.getAll().stream()
                    .map(this::sanitizeUser)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query users: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<SysUser> createUser(@RequestBody SysUser user) {
        try {
            SysUser created = sysUserService.create(user);
            auditTrailService.record("users", "CREATE", "users", String.valueOf(created.getId()), "Created user");
            return ApiResponse.success("User created", sanitizeUser(created));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create user: " + e.getMessage());
        }
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        try {
            user.setId(id);
            SysUser updated = sysUserService.update(user);
            auditTrailService.record("users", "UPDATE", "users", String.valueOf(id), "Updated user");
            return ApiResponse.success("User updated", sanitizeUser(updated));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update user: " + e.getMessage());
        }
    }

    @PostMapping("/{id:\\d+}/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable Long id, @RequestParam Long operatorId) {
        try {
            String temporaryPassword = sysUserService.adminResetPassword(id, operatorId);
            auditTrailService.record("users", "RESET_PASSWORD", "users", String.valueOf(id), "Admin reset user password");
            return ApiResponse.success("Password reset successfully", Map.of("temporaryPassword", temporaryPassword));
        } catch (Exception e) {
            return ApiResponse.error("Failed to reset password: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        try {
            sysUserService.delete(id);
            auditTrailService.record("users", "DELETE", "users", String.valueOf(id), "Deleted user");
            return ApiResponse.success("User deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete user: " + e.getMessage());
        }
    }

    private SysUser sanitizeUser(SysUser user) {
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
