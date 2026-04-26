package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.SysRole;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin
@PreAuthorize("hasAuthority('permission.roles.manage')")
public class SysRoleController {

    private final SysRoleService roleService;
    private final AuditTrailService auditTrailService;

    public SysRoleController(SysRoleService roleService, AuditTrailService auditTrailService) {
        this.roleService = roleService;
        this.auditTrailService = auditTrailService;
    }

    @GetMapping
    public List<SysRole> getAll() {
        return roleService.getAll();
    }

    @GetMapping("/{id}")
    public SysRole getById(@PathVariable Long id) {
        return roleService.getById(id);
    }

    @PostMapping
    public SysRole create(@RequestBody SysRole role) {
        SysRole created = roleService.create(role);
        auditTrailService.record("roles", "CREATE", "roles", String.valueOf(created.getId()), "Created role");
        return created;
    }

    @PutMapping("/{id}")
    public SysRole update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        SysRole updated = roleService.update(role);
        auditTrailService.record("roles", "UPDATE", "roles", String.valueOf(id), "Updated role");
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
        auditTrailService.record("roles", "DELETE", "roles", String.valueOf(id), "Deleted role");
    }
}
