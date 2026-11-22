package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.ProductSubcategory;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProductSubcategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-subcategory-dicts")
public class ProductSubcategoryController {

    @Autowired
    private ProductSubcategoryService service;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<ProductSubcategory>> getAll() {
        return ApiResponse.success(service.getAll());
    }

    @GetMapping("/options")
    public ApiResponse<List<ProductSubcategory>> getOptions() {
        return ApiResponse.success(service.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductSubcategory> getById(@PathVariable Long id) {
        ProductSubcategory item = service.getById(id);
        return item != null ? ApiResponse.success(item) : ApiResponse.notFound("Subcategory not found");
    }

    @PostMapping
    public ApiResponse<ProductSubcategory> create(@RequestBody ProductSubcategory item) {
        try {
            ProductSubcategory created = service.create(item);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("material-subcategory-dicts", "CREATE", "material-subcategory-dicts", String.valueOf(created.getId()), "Created material subcategory");
            }
            return ApiResponse.success("Created successfully", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create subcategory: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductSubcategory> update(@PathVariable Long id, @RequestBody ProductSubcategory item) {
        try {
            item.setId(id);
            ProductSubcategory updated = service.update(item);
            if (auditTrailService != null) {
                auditTrailService.record("material-subcategory-dicts", "UPDATE", "material-subcategory-dicts", String.valueOf(id), "Updated material subcategory");
            }
            return ApiResponse.success("Updated successfully", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update subcategory: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            if (auditTrailService != null) {
                auditTrailService.record("material-subcategory-dicts", "DELETE", "material-subcategory-dicts", String.valueOf(id), "Deleted material subcategory");
            }
            return ApiResponse.success("Deleted successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete subcategory: " + e.getMessage());
        }
    }
}
