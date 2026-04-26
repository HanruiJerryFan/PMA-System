package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ProductCategory;
import com.jerry.salesmanagement.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-category-dicts")
@CrossOrigin
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService categoryService;

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
        return categoryService.create(category);
    }

    @PutMapping("/{id}")
    public ProductCategory update(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        return categoryService.update(category);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
