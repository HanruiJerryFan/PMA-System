package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.ProductHistoryPrice;
import com.jerry.salesmanagement.service.ProductHistoryPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-prices")
@CrossOrigin
public class ProductHistoryPriceController {

    @Autowired
    private ProductHistoryPriceService service;

    @GetMapping
    public ApiResponse<List<ProductHistoryPrice>> getAll() {
        try {
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch product price history: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductHistoryPrice> getById(@PathVariable Long id) {
        ProductHistoryPrice item = service.getById(id);
        return item != null ? ApiResponse.success(item) : ApiResponse.notFound("Price record not found");
    }

    @GetMapping("/byProduct/{uuid}")
    public ApiResponse<List<ProductHistoryPrice>> getByProductUuid(@PathVariable String uuid) {
        return ApiResponse.success(service.getByProductUuid(uuid));
    }

    @PostMapping
    public ApiResponse<ProductHistoryPrice> create(@RequestBody ProductHistoryPrice historyPrice) {
        try {
            return ApiResponse.success("Created successfully", service.create(historyPrice));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create price record: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductHistoryPrice> update(@PathVariable Long id, @RequestBody ProductHistoryPrice historyPrice) {
        try {
            historyPrice.setId(id);
            return ApiResponse.success("Updated successfully", service.update(historyPrice));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update price record: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ApiResponse.success("Deleted successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete price record: " + e.getMessage());
        }
    }
}
