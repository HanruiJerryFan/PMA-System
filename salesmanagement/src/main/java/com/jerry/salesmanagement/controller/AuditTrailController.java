package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.AuditTrail;
import com.jerry.salesmanagement.service.AuditTrailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-trails")
@CrossOrigin
public class AuditTrailController {

    @Autowired
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<AuditTrail>> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action
    ) {
        try {
            return ApiResponse.success(auditTrailService.getAll(userId, module, action));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query audit trails: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditTrail> getById(@PathVariable Long id) {
        try {
            AuditTrail auditTrail = auditTrailService.getById(id);
            return auditTrail != null ? ApiResponse.success(auditTrail) : ApiResponse.notFound("Audit trail not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query audit trail: " + e.getMessage());
        }
    }
}
