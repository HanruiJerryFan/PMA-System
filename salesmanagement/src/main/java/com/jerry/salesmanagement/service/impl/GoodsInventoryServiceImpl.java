package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.GoodsInventoryMapper;
import com.jerry.salesmanagement.pojo.GoodsInventory;
import com.jerry.salesmanagement.service.GoodsInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsInventoryServiceImpl implements GoodsInventoryService {

    @Autowired
    private GoodsInventoryMapper mapper;

    @Override
    public GoodsInventory getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<GoodsInventory> getAll() {
        return mapper.selectAll();
    }
}
