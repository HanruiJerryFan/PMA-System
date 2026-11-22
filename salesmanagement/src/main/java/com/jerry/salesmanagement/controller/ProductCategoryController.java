package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ProductCategory;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-category-dicts")
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService categoryService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public List<ProductCategory> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/options")
    public List<ProductCategory> getOptions() {
        return categoryService.getAll();
    }

    @GetMapping("/{id}")
    public ProductCategory getById(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @PostMapping
    public ProductCategory create(@RequestBody ProductCategory category) {
        ProductCategory created = categoryService.create(category);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("material-category-dicts", "CREATE", "material-category-dicts", String.valueOf(created.getId()), "Created material category");
        }
        return created;
    }

    @PutMapping("/{id}")
    public ProductCategory update(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        ProductCategory updated = categoryService.update(category);
        if (auditTrailService != null) {
            auditTrailService.record("material-category-dicts", "UPDATE", "material-category-dicts", String.valueOf(id), "Updated material category");
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
        if (auditTrailService != null) {
            auditTrailService.record("material-category-dicts", "DELETE", "material-category-dicts", String.valueOf(id), "Deleted material category");
        }
    }
}
