package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.ProductSubcategory;
import com.jerry.salesmanagement.service.ProductSubcategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-subcategory-dicts")
@CrossOrigin
public class ProductSubcategoryController {

    @Autowired
    private ProductSubcategoryService service;

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
            return ApiResponse.success("Created successfully", service.create(item));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create subcategory: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductSubcategory> update(@PathVariable Long id, @RequestBody ProductSubcategory item) {
        try {
            item.setId(id);
            return ApiResponse.success("Updated successfully", service.update(item));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update subcategory: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ApiResponse.success("Deleted successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete subcategory: " + e.getMessage());
        }
    }
}
