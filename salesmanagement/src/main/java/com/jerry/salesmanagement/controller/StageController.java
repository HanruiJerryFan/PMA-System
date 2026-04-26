package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-stages")
@CrossOrigin
public class StageController {

    @Autowired
    private StageService stageService;

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
            return ApiResponse.success("Created successfully", stageService.create(stage));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create stage: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Stage> update(@PathVariable Long id, @RequestBody Stage stage) {
        try {
            stage.setId(id);
            return ApiResponse.success("Updated successfully", stageService.update(stage));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update stage: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            stageService.delete(id);
            return ApiResponse.success("Deleted successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete stage: " + e.getMessage());
        }
    }
}
