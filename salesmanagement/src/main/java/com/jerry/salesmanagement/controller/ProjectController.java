package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ApiResponse<List<Project>> getAll() {
        try {
            return ApiResponse.success(projectService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query projects: " + e.getMessage());
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
            return ApiResponse.success("Project created", projectService.create(project));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create project: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<Project> update(@PathVariable String uuid, @RequestBody Project project) {
        try {
            project.setUuid(uuid);
            return ApiResponse.success("Project updated", projectService.update(project));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update project: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            projectService.delete(uuid);
            return ApiResponse.success("Project deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete project: " + e.getMessage());
        }
    }
}
