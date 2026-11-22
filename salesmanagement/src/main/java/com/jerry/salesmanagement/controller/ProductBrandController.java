package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ProductBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-brand-dicts")
public class ProductBrandController {

    @Autowired
    private ProductBrandService brandService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

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
        ProductBrand created = brandService.create(brand);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("material-brand-dicts", "CREATE", "material-brand-dicts", String.valueOf(created.getId()), "Created material brand");
        }
        return created;
    }

    @PutMapping("/{id}")
    public ProductBrand update(@PathVariable Long id, @RequestBody ProductBrand brand) {
        brand.setId(id);
        ProductBrand updated = brandService.update(brand);
        if (auditTrailService != null) {
            auditTrailService.record("material-brand-dicts", "UPDATE", "material-brand-dicts", String.valueOf(id), "Updated material brand");
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        brandService.delete(id);
        if (auditTrailService != null) {
            auditTrailService.record("material-brand-dicts", "DELETE", "material-brand-dicts", String.valueOf(id), "Deleted material brand");
        }
    }
}
