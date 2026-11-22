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
import java.util.Objects;

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
    @Transactional
    public Warehouse create(Warehouse warehouse) {
        validate(warehouse, null);
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

    @Override
    @Transactional
    public Warehouse update(Warehouse warehouse) {
        if (warehouse.getId() == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        Warehouse existing = warehouseMapper.selectById(warehouse.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Warehouse does not exist");
        }
        if (isReservedSystemWarehouse(existing)) {
            String normalizedCode = warehouse.getWarehouseCode() == null ? "" : warehouse.getWarehouseCode().trim();
            String normalizedName = warehouse.getWarehouseName() == null ? "" : warehouse.getWarehouseName().trim();
            boolean reservedByExistingCode = matchesReservedWarehouseCode(existing.getWarehouseCode());
            if (reservedByExistingCode && !existing.getWarehouseCode().trim().equalsIgnoreCase(normalizedCode)) {
                throw new IllegalArgumentException("系统保留仓库编码不允许修改: " + existing.getWarehouseCode().trim());
            }
            if (!reservedByExistingCode && matchesReservedWarehouseCode(existing.getWarehouseName())
                    && !existing.getWarehouseName().trim().equalsIgnoreCase(normalizedName)) {
                throw new IllegalArgumentException("系统保留仓库名称不允许修改: " + existing.getWarehouseName().trim());
            }
        }
        validate(warehouse, existing);
        warehouseMapper.update(warehouse);
        return warehouseMapper.selectById(warehouse.getId());
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

    private void validate(Warehouse warehouse, Warehouse existing) {
        if (!StringUtils.hasText(warehouse.getWarehouseCode())) {
            throw new IllegalArgumentException("Warehouse code is required");
        }
        if (!StringUtils.hasText(warehouse.getWarehouseName())) {
            throw new IllegalArgumentException("Warehouse name is required");
        }
        String code = warehouse.getWarehouseCode().trim();
        String name = warehouse.getWarehouseName().trim();
        warehouse.setWarehouseCode(code);
        warehouse.setWarehouseName(name);
        warehouse.setWarehouseAddress(trimToNull(warehouse.getWarehouseAddress()));
        warehouse.setContactName(trimToNull(warehouse.getContactName()));
        warehouse.setContactPhone(trimToNull(warehouse.getContactPhone()));
        warehouse.setRemark(trimToNull(warehouse.getRemark()));

        boolean reservedExisting = existing != null && isReservedSystemWarehouse(existing);
        if (!reservedExisting && (matchesReservedWarehouseCode(code) || matchesReservedWarehouseCode(name))) {
            throw new IllegalArgumentException("不能创建或修改为与系统保留仓库重名的编码/名称: " + (matchesReservedWarehouseCode(code) ? code : name));
        }

        List<Warehouse> allWarehouses = warehouseMapper.selectAll();
        for (Warehouse other : allWarehouses) {
            if (other == null || Objects.equals(other.getId(), warehouse.getId())) {
                continue;
            }
            if (other.getWarehouseCode() != null && other.getWarehouseCode().trim().equalsIgnoreCase(code)) {
                throw new IllegalArgumentException("Warehouse code already exists: " + code);
            }
            if (reservedExisting && matchesReservedWarehouseCode(name)
                    && ((other.getWarehouseCode() != null && other.getWarehouseCode().trim().equalsIgnoreCase(name))
                    || (other.getWarehouseName() != null && other.getWarehouseName().trim().equalsIgnoreCase(name)))) {
                throw new IllegalArgumentException("Warehouse name conflicts with reserved warehouse: " + name);
            }
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
