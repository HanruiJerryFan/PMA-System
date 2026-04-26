package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.SysPermission;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysPermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin
@PreAuthorize("hasAuthority('permission.items.manage')")
public class SysPermissionController {

    private final SysPermissionService permissionService;
    private final AuditTrailService auditTrailService;

    public SysPermissionController(SysPermissionService permissionService, AuditTrailService auditTrailService) {
        this.permissionService = permissionService;
        this.auditTrailService = auditTrailService;
    }

    @GetMapping
    public List<SysPermission> getAll() {
        return permissionService.getAll();
    }

    @GetMapping("/{id}")
    public SysPermission getById(@PathVariable Long id) {
        return permissionService.getById(id);
    }

    @PostMapping
    public SysPermission create(@RequestBody SysPermission permission) {
        SysPermission created = permissionService.create(permission);
        auditTrailService.record("permissions", "CREATE", "permissions", String.valueOf(created.getId()), "Created permission item");
        return created;
    }

    @PutMapping("/{id}")
    public SysPermission update(@PathVariable Long id, @RequestBody SysPermission permission) {
        permission.setId(id);
        SysPermission updated = permissionService.update(permission);
        auditTrailService.record("permissions", "UPDATE", "permissions", String.valueOf(id), "Updated permission item");
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        permissionService.delete(id);
        auditTrailService.record("permissions", "DELETE", "permissions", String.valueOf(id), "Deleted permission item");
    }
}
