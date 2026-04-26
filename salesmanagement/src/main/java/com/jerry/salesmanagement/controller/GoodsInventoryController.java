package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.GoodsInventory;
import com.jerry.salesmanagement.service.GoodsInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-stock")
@CrossOrigin
public class GoodsInventoryController {

    @Autowired
    private GoodsInventoryService service;

    @GetMapping
    public ApiResponse<List<GoodsInventory>> getAll() {
        try {
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("获取库存列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<GoodsInventory> getByUuid(@PathVariable String uuid) {
        try {
            GoodsInventory inventory = service.getByUuid(uuid);
            return inventory != null ? ApiResponse.success(inventory) : ApiResponse.notFound("库存记录不存在");
        } catch (Exception e) {
            return ApiResponse.error("获取库存记录失败: " + e.getMessage());
        }
    }
}
