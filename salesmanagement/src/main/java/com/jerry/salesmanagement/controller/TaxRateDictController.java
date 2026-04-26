package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.TaxRateDict;
import com.jerry.salesmanagement.service.TaxRateDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tax-rate-dicts")
@CrossOrigin
public class TaxRateDictController {

    @Autowired
    private TaxRateDictService taxRateDictService;

    @GetMapping
    public ApiResponse<List<TaxRateDict>> getAll() {
        return ApiResponse.success(taxRateDictService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<TaxRateDict> getById(@PathVariable Long id) {
        TaxRateDict item = taxRateDictService.getById(id);
        return item != null ? ApiResponse.success(item) : ApiResponse.notFound("Tax rate not found");
    }

    @PostMapping
    public ApiResponse<TaxRateDict> create(@RequestBody TaxRateDict taxRateDict) {
        try {
            return ApiResponse.success("Tax rate created", taxRateDictService.create(taxRateDict));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create tax rate: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<TaxRateDict> update(@PathVariable Long id, @RequestBody TaxRateDict taxRateDict) {
        try {
            taxRateDict.setId(id);
            return ApiResponse.success("Tax rate updated", taxRateDictService.update(taxRateDict));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update tax rate: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            taxRateDictService.delete(id);
            return ApiResponse.success("Tax rate deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete tax rate: " + e.getMessage());
        }
    }
}
