package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.dto.ProjectListAggregateView;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProjectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-lists")
@CrossOrigin
public class ProjectListController {

    @Autowired
    private ProjectListService service;

    @Autowired
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<ProjectList>> getAll() {
        try {
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project lists: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResponse<List<ProjectList>> getOptions() {
        try {
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project list options: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<ProjectList> getByUuid(@PathVariable String uuid) {
        try {
            ProjectList projectList = service.getByUuid(uuid);
            return projectList != null ? ApiResponse.success(projectList) : ApiResponse.notFound("Project list not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project list: " + e.getMessage());
        }
    }

    @GetMapping("/byProject/{projectId}")
    public ApiResponse<List<ProjectList>> getByProject(@PathVariable String projectId) {
        try {
            return ApiResponse.success(service.getByProjectId(projectId));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project lists: " + e.getMessage());
        }
    }

    @GetMapping("/aggregate/{projectId}")
    public ApiResponse<ProjectListAggregateView> getAggregateView(
            @PathVariable String projectId,
            @RequestParam String aggregateType
    ) {
        try {
            return ApiResponse.success(service.getAggregateView(projectId, aggregateType));
        } catch (Exception e) {
            return ApiResponse.error("Failed to generate aggregated project list: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('project.list.entry', 'project.manage')")
    public ApiResponse<ProjectList> create(@RequestBody ProjectList projectList) {
        try {
            ProjectList created = service.create(projectList);
            auditTrailService.record("project-lists", "CREATE", "project-lists", created.getUuid(), "Created project list");
            return ApiResponse.success("Project list created", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create project list: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('project.list.entry', 'project.manage')")
    public ApiResponse<ProjectList> update(@PathVariable String uuid, @RequestBody ProjectList projectList) {
        try {
            projectList.setUuid(uuid);
            ProjectList updated = service.update(projectList);
            auditTrailService.record("project-lists", "UPDATE", "project-lists", uuid, "Updated project list");
            return ApiResponse.success("Project list updated", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update project list: " + e.getMessage());
        }
    }

    @PostMapping("/{uuid}/audit")
    @PreAuthorize("hasAnyAuthority('project.list.audit', 'project.manage')")
    public ApiResponse<ProjectList> audit(@PathVariable String uuid) {
        try {
            ProjectList audited = service.audit(uuid);
            auditTrailService.record("project-lists", "AUDIT", "project-lists", uuid, "Audited project list");
            return ApiResponse.success("Project list audited", audited);
        } catch (Exception e) {
            return ApiResponse.error("Failed to audit project list: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('project.list.entry', 'project.manage')")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            service.delete(uuid);
            auditTrailService.record("project-lists", "DELETE", "project-lists", uuid, "Deleted project list");
            return ApiResponse.success("Project list deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete project list: " + e.getMessage());
        }
    }
}
