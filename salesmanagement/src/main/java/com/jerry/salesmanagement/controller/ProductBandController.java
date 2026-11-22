package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.ProductBand;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProductBandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-band-dicts")
public class ProductBandController {

    @Autowired
    private ProductBandService service;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<ProductBand>> getAll() {
        return ApiResponse.success(service.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductBand> getById(@PathVariable Long id) {
        ProductBand item = service.getById(id);
        return item != null ? ApiResponse.success(item) : ApiResponse.notFound("Band not found");
    }

    @PostMapping
    public ApiResponse<ProductBand> create(@RequestBody ProductBand item) {
        try {
            ProductBand created = service.create(item);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("material-band-dicts", "CREATE", "material-band-dicts", String.valueOf(created.getId()), "Created material band");
            }
            return ApiResponse.success("Created successfully", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create band: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductBand> update(@PathVariable Long id, @RequestBody ProductBand item) {
        try {
            item.setId(id);
            ProductBand updated = service.update(item);
            if (auditTrailService != null) {
                auditTrailService.record("material-band-dicts", "UPDATE", "material-band-dicts", String.valueOf(id), "Updated material band");
            }
            return ApiResponse.success("Updated successfully", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update band: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            if (auditTrailService != null) {
                auditTrailService.record("material-band-dicts", "DELETE", "material-band-dicts", String.valueOf(id), "Deleted material band");
            }
            return ApiResponse.success("Deleted successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete band: " + e.getMessage());
        }
    }
}
