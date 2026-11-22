package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.SystemConfig;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system-configs")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

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
            SystemConfig saved = systemConfigService.createOrUpdate(systemConfig);
            if (auditTrailService != null) {
                auditTrailService.record("system-configs", "UPDATE", "system-configs", configKey, "Updated system config");
            }
            return ApiResponse.success("System config saved", saved);
        } catch (Exception e) {
            return ApiResponse.error("Failed to save system config: " + e.getMessage());
        }
    }

    @DeleteMapping("/{configKey}")
    public ApiResponse<Void> delete(@PathVariable String configKey) {
        try {
            systemConfigService.delete(configKey);
            if (auditTrailService != null) {
                auditTrailService.record("system-configs", "DELETE", "system-configs", configKey, "Deleted system config");
            }
            return ApiResponse.success("System config deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete system config: " + e.getMessage());
        }
    }
}
