package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.WarehouseDocumentRules;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

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
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        Warehouse existing = warehouseMapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (isReservedSystemWarehouse(existing)) {
            String identifier = StringUtils.hasText(existing.getWarehouseCode())
                    ? existing.getWarehouseCode().trim()
                    : existing.getWarehouseName().trim();
            throw new IllegalArgumentException("系统保留仓库不允许删除: " + identifier);
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "仓库",
                    new ReferenceCheckMapper.ReferenceUsage("出入库单", referenceCheckMapper.countWarehouseDocsByWarehouseId(id)),
                    new ReferenceCheckMapper.ReferenceUsage("库存流水", referenceCheckMapper.countInventoryTransactionsByWarehouseId(id))
            );
        }
        warehouseMapper.deleteById(id);
    }

    private boolean isReservedSystemWarehouse(Warehouse warehouse) {
        return matchesReservedWarehouseCode(warehouse.getWarehouseCode())
                || matchesReservedWarehouseCode(warehouse.getWarehouseName());
    }

    private boolean matchesReservedWarehouseCode(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        return WarehouseDocumentRules.PROJECT_WAREHOUSE.equalsIgnoreCase(trimmed)
                || WarehouseDocumentRules.CENTRAL_WAREHOUSE.equalsIgnoreCase(trimmed);
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
