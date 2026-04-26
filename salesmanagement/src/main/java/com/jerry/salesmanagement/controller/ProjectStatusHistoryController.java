package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProjectStatusHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-status-history")
@CrossOrigin
public class ProjectStatusHistoryController {

    @Autowired
    private ProjectStatusHistoryService service;

    @Autowired
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<ProjectStatusHistory>> getAll(@RequestParam(required = false) String projectId) {
        try {
            if (projectId != null && !projectId.trim().isEmpty()) {
                return ApiResponse.success(service.getByProjectId(projectId));
            }
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project status history: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<ProjectStatusHistory> getByUuid(@PathVariable String uuid) {
        try {
            ProjectStatusHistory history = service.getByUuid(uuid);
            return history != null ? ApiResponse.success(history) : ApiResponse.notFound("Project status history not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project status history: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<ProjectStatusHistory> create(@RequestBody ProjectStatusHistory history) {
        try {
            ProjectStatusHistory created = service.create(history);
            auditTrailService.record("project-status-history", "CREATE", "project-status-history", created.getUuid(), "Created project status history");
            return ApiResponse.success("Project status history created", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create project status history: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<ProjectStatusHistory> update(@PathVariable String uuid, @RequestBody ProjectStatusHistory history) {
        try {
            history.setUuid(uuid);
            ProjectStatusHistory updated = service.update(history);
            auditTrailService.record("project-status-history", "UPDATE", "project-status-history", uuid, "Updated project status history");
            return ApiResponse.success("Project status history updated", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update project status history: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            service.delete(uuid);
            auditTrailService.record("project-status-history", "DELETE", "project-status-history", uuid, "Deleted project status history");
            return ApiResponse.success("Project status history deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete project status history: " + e.getMessage());
        }
    }
}
