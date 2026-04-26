package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.service.ProductBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-brand-dicts")
@CrossOrigin
public class ProductBrandController {

    @Autowired
    private ProductBrandService brandService;

    @GetMapping
    public List<ProductBrand> getAll() {
        return brandService.getAll();
    }

    @GetMapping("/options")
    public List<ProductBrand> getOptions() {
        return brandService.getAll();
    }

    @GetMapping("/{id}")
    public ProductBrand getById(@PathVariable Long id) {
        return brandService.getById(id);
    }

    @PostMapping
    public ProductBrand create(@RequestBody ProductBrand brand) {
        return brandService.create(brand);
    }

    @PutMapping("/{id}")
    public ProductBrand update(@PathVariable Long id, @RequestBody ProductBrand brand) {
        brand.setId(id);
        return brandService.update(brand);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        brandService.delete(id);
    }
}
