package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryTransactionServiceImpl implements InventoryTransactionService {

    private static final List<String> ALLOWED_CATEGORIES = Arrays.asList(
            "PROJECT_PURCHASE",
            "PROJECT_SALES",
            "CENTRALIZED_PURCHASE",
            "WAREHOUSE_TRANSFER_TO_PROJECT",
            "PROJECT_TRANSFER_TO_WAREHOUSE"
    );

    private static final List<String> ALLOWED_TXN_TYPES = Arrays.asList("IN", "OUT");

    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;

    @Autowired
    private MaterialMasterMapper materialMasterMapper;

    @Autowired
    private ProductBrandMapper productBrandMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public List<InventoryTransaction> getAll() {
        return inventoryTransactionMapper.selectAll();
    }

    @Override
    public InventoryTransaction getByUuid(String uuid) {
        return inventoryTransactionMapper.selectByUuid(uuid);
    }

    @Override
    public List<InventoryTransaction> getByContractUuid(String contractUuid) {
        return inventoryTransactionMapper.selectBySourceRef("CONTRACT", contractUuid);
    }

    @Override
    public InventoryTransaction create(InventoryTransaction inventoryTransaction) {
        validateTransaction(inventoryTransaction);
        if (!StringUtils.hasText(inventoryTransaction.getUuid())) {
            inventoryTransaction.setUuid(UUID.randomUUID().toString());
        }
        inventoryTransactionMapper.insert(inventoryTransaction);
        return inventoryTransaction;
    }

    @Override
    public InventoryTransaction update(InventoryTransaction inventoryTransaction) {
        validateTransaction(inventoryTransaction);
        inventoryTransactionMapper.updateByUuid(inventoryTransaction);
        return inventoryTransaction;
    }

    @Override
    public void delete(String uuid) {
        inventoryTransactionMapper.deleteByUuid(uuid);
    }

    private void validateTransaction(InventoryTransaction inventoryTransaction) {
        if (!StringUtils.hasText(inventoryTransaction.getMaterialId())) {
            throw new IllegalArgumentException("Material is required");
        }
        if (inventoryTransaction.getWarehouseId() == null) {
            throw new IllegalArgumentException("Warehouse is required");
        }
        if (warehouseMapper.selectById(inventoryTransaction.getWarehouseId()) == null) {
            throw new IllegalArgumentException("Warehouse does not exist");
        }
        if (!StringUtils.hasText(inventoryTransaction.getCategory())) {
            throw new IllegalArgumentException("Business category is required");
        }
        inventoryTransaction.setCategory(inventoryTransaction.getCategory().trim().toUpperCase());
        if (!ALLOWED_CATEGORIES.contains(inventoryTransaction.getCategory())) {
            throw new IllegalArgumentException("Unsupported business category");
        }
        if (!StringUtils.hasText(inventoryTransaction.getTxnType())) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        inventoryTransaction.setTxnType(inventoryTransaction.getTxnType().trim().toUpperCase());
        if (!ALLOWED_TXN_TYPES.contains(inventoryTransaction.getTxnType())) {
            throw new IllegalArgumentException("Transaction type must be IN or OUT");
        }
        if (inventoryTransaction.getQuantity() == null || inventoryTransaction.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (inventoryTransaction.getUnitPrice() != null && inventoryTransaction.getUnitPrice() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (StringUtils.hasText(inventoryTransaction.getProjectId())
                && projectMapper.selectByUuid(inventoryTransaction.getProjectId()) == null) {
            throw new IllegalArgumentException("Project does not exist");
        }

        MaterialMaster materialMaster = materialMasterMapper.selectByUuid(inventoryTransaction.getMaterialId());
        if (materialMaster == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        ProductBrand brand = materialMaster.getBrandId() != null
                ? productBrandMapper.selectById(materialMaster.getBrandId())
                : null;
        inventoryTransaction.setMaterialCode(materialMaster.getMaterialCode());
        inventoryTransaction.setMaterialName(materialMaster.getProductName());
        inventoryTransaction.setModel(materialMaster.getProductModel());
        inventoryTransaction.setBrand(brand != null ? brand.getName() : null);
        inventoryTransaction.setTxnTime(inventoryTransaction.getTxnTime() != null ? inventoryTransaction.getTxnTime() : new Date());
        inventoryTransaction.setAmount(calculateAmount(inventoryTransaction));
    }

    private Double calculateAmount(InventoryTransaction inventoryTransaction) {
        if (inventoryTransaction.getUnitPrice() == null || inventoryTransaction.getQuantity() == null) {
            return null;
        }
        return inventoryTransaction.getUnitPrice() * inventoryTransaction.getQuantity();
    }
}
