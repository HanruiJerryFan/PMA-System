package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ProjectType;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProjectTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-types")
public class ProjectTypeController {

    @Autowired
    private ProjectTypeService service;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public List<ProjectType> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProjectType getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ProjectType create(@RequestBody ProjectType projectType) {
        ProjectType created = service.create(projectType);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("project-types", "CREATE", "project-types", String.valueOf(created.getId()), "Created project type");
        }
        return created;
    }

    @PutMapping("/{id}")
    public ProjectType update(@PathVariable Long id, @RequestBody ProjectType projectType) {
        projectType.setId(id);
        ProjectType updated = service.update(projectType);
        if (auditTrailService != null) {
            auditTrailService.record("project-types", "UPDATE", "project-types", String.valueOf(id), "Updated project type");
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
        if (auditTrailService != null) {
            auditTrailService.record("project-types", "DELETE", "project-types", String.valueOf(id), "Deleted project type");
        }
    }
}
