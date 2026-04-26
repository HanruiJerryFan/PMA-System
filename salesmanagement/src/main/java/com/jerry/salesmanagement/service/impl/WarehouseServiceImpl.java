package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public Warehouse getById(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Override
    public List<Warehouse> getAll() {
        return warehouseMapper.selectAll();
    }

    @Override
    public Warehouse create(Warehouse warehouse) {
        validate(warehouse);
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

    @Override
    public Warehouse update(Warehouse warehouse) {
        validate(warehouse);
        warehouseMapper.update(warehouse);
        return warehouse;
    }

    @Override
    public void delete(Long id) {
        warehouseMapper.deleteById(id);
    }

    private void validate(Warehouse warehouse) {
        if (!StringUtils.hasText(warehouse.getWarehouseCode())) {
            throw new IllegalArgumentException("Warehouse code is required");
        }
        if (!StringUtils.hasText(warehouse.getWarehouseName())) {
            throw new IllegalArgumentException("Warehouse name is required");
        }
    }
}
