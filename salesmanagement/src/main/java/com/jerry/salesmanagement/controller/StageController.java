package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-stages")
public class StageController {

    @Autowired
    private StageService stageService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<Stage>> getAll() {
        return ApiResponse.success(stageService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Stage> getById(@PathVariable Long id) {
        Stage item = stageService.getById(id);
        return item != null ? ApiResponse.success(item) : ApiResponse.notFound("Stage not found");
    }

    @PostMapping
    public ApiResponse<Stage> create(@RequestBody Stage stage) {
        try {
            Stage created = stageService.create(stage);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("project-stages", "CREATE", "project-stages", String.valueOf(created.getId()), "Created project stage");
            }
            return ApiResponse.success("Created successfully", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create stage: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Stage> update(@PathVariable Long id, @RequestBody Stage stage) {
        try {
            stage.setId(id);
            Stage updated = stageService.update(stage);
            if (auditTrailService != null) {
                auditTrailService.record("project-stages", "UPDATE", "project-stages", String.valueOf(id), "Updated project stage");
            }
            return ApiResponse.success("Updated successfully", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update stage: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            stageService.delete(id);
            if (auditTrailService != null) {
                auditTrailService.record("project-stages", "DELETE", "project-stages", String.valueOf(id), "Deleted project stage");
            }
            return ApiResponse.success("Deleted successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete stage: " + e.getMessage());
        }
    }
}
