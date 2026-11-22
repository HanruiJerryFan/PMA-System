package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<Project>> getAll() {
        try {
            return ApiResponse.success(projectService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query projects: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<Project>> getPage(
            @RequestParam(required = false) String projectNumber,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Long managerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            return ApiResponse.success(projectService.getPage(
                    projectNumber, projectName, customerId, managerId, page, pageSize
            ));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query projects page: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResponse<List<Project>> getOptions() {
        try {
            return ApiResponse.success(projectService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project options: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<Project> getByUuid(@PathVariable String uuid) {
        try {
            Project project = projectService.getByUuid(uuid);
            return project != null ? ApiResponse.success(project) : ApiResponse.notFound("Project not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query project: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Project> create(@RequestBody Project project) {
        try {
            Project created = projectService.create(project);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("projects", "CREATE", "projects", created.getUuid(), "Created project");
            }
            return ApiResponse.success("Project created", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create project: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<Project> update(@PathVariable String uuid, @RequestBody Project project) {
        try {
            project.setUuid(uuid);
            Project updated = projectService.update(project);
            if (auditTrailService != null) {
                auditTrailService.record("projects", "UPDATE", "projects", uuid, "Updated project");
            }
            return ApiResponse.success("Project updated", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update project: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            projectService.delete(uuid);
            if (auditTrailService != null) {
                auditTrailService.record("projects", "DELETE", "projects", uuid, "Deleted project");
            }
            return ApiResponse.success("Project deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete project: " + e.getMessage());
        }
    }
}
