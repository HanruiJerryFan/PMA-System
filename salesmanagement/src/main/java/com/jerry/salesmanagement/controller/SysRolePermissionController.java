package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.SysRolePermission;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysRolePermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sysrolepermission")
@CrossOrigin
@PreAuthorize("hasAuthority('permission.role-permissions.manage')")
public class SysRolePermissionController {

    private final SysRolePermissionService rolePermissionService;
    private final AuditTrailService auditTrailService;

    public SysRolePermissionController(
            SysRolePermissionService rolePermissionService,
            AuditTrailService auditTrailService
    ) {
        this.rolePermissionService = rolePermissionService;
        this.auditTrailService = auditTrailService;
    }

    @GetMapping("/byRole/{roleId}")
    public List<SysRolePermission> getByRoleId(@PathVariable Long roleId) {
        return rolePermissionService.getByRoleId(roleId);
    }

    @PostMapping
    public void addRolePermission(@RequestBody SysRolePermission rolePermission) {
        rolePermissionService.addRolePermission(rolePermission);
        auditTrailService.record(
                "role-permissions",
                "CREATE",
                "role-permissions",
                rolePermission.getRoleId() + ":" + rolePermission.getPermissionId(),
                "Assigned permission to role"
        );
    }

    @DeleteMapping
    public void deleteRolePermission(@RequestParam Long roleId, @RequestParam Long permissionId) {
        rolePermissionService.deleteRolePermission(roleId, permissionId);
        auditTrailService.record(
                "role-permissions",
                "DELETE",
                "role-permissions",
                roleId + ":" + permissionId,
                "Removed permission from role"
        );
    }
}
