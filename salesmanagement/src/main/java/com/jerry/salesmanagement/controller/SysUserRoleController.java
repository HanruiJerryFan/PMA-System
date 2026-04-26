package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.SysUserRole;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysUserRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sysuserrole")
@CrossOrigin
@PreAuthorize("hasAuthority('permission.user-roles.manage')")
public class SysUserRoleController {

    private final SysUserRoleService userRoleService;
    private final AuditTrailService auditTrailService;

    public SysUserRoleController(SysUserRoleService userRoleService, AuditTrailService auditTrailService) {
        this.userRoleService = userRoleService;
        this.auditTrailService = auditTrailService;
    }

    @GetMapping("/byUser/{userId}")
    public List<SysUserRole> getByUserId(@PathVariable Long userId) {
        return userRoleService.getByUserId(userId);
    }

    @GetMapping("/byRole/{roleId}")
    public List<SysUserRole> getByRoleId(@PathVariable Long roleId) {
        return userRoleService.getByRoleId(roleId);
    }

    @PostMapping
    public void addUserRole(@RequestBody SysUserRole userRole) {
        userRoleService.addUserRole(userRole);
        auditTrailService.record(
                "user-roles",
                "CREATE",
                "user-roles",
                userRole.getUserId() + ":" + userRole.getRoleId(),
                "Assigned role to user"
        );
    }

    @DeleteMapping
    public void deleteUserRole(@RequestParam Long userId, @RequestParam Long roleId) {
        userRoleService.deleteUserRole(userId, roleId);
        auditTrailService.record("user-roles", "DELETE", "user-roles", userId + ":" + roleId, "Removed role from user");
    }
}
