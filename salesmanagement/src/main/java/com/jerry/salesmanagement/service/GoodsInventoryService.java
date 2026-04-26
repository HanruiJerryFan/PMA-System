package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.GoodsInventory;

import java.util.List;

public interface GoodsInventoryService {
    GoodsInventory getByUuid(String uuid);
    List<GoodsInventory> getAll();
}
