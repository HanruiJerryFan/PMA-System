package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.SystemConfig;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system-configs")
@CrossOrigin
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping
    public ApiResponse<List<SystemConfig>> getAll() {
        try {
            return ApiResponse.success(systemConfigService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query system configs: " + e.getMessage());
        }
    }

    @GetMapping("/{configKey}")
    public ApiResponse<SystemConfig> getByKey(@PathVariable String configKey) {
        try {
            SystemConfig config = systemConfigService.getByKey(configKey);
            return config != null ? ApiResponse.success(config) : ApiResponse.notFound("System config not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query system config: " + e.getMessage());
        }
    }

    @PutMapping("/{configKey}")
    public ApiResponse<SystemConfig> put(@PathVariable String configKey, @RequestBody SystemConfig systemConfig) {
        try {
            systemConfig.setConfigKey(configKey);
            return ApiResponse.success("System config saved", systemConfigService.createOrUpdate(systemConfig));
        } catch (Exception e) {
            return ApiResponse.error("Failed to save system config: " + e.getMessage());
        }
    }

    @DeleteMapping("/{configKey}")
    public ApiResponse<Void> delete(@PathVariable String configKey) {
        try {
            systemConfigService.delete(configKey);
            return ApiResponse.success("System config deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete system config: " + e.getMessage());
        }
    }
}
