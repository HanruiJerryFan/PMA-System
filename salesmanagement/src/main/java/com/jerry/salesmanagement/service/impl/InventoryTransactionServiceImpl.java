package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class InventoryTransactionServiceImpl implements InventoryTransactionService {

    private static final String CONFIG_ALLOW_NEGATIVE_STOCK = "inventory.allow_negative_stock";
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

    @Autowired(required = false)
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private SystemConfigService systemConfigService;

    @Override
    public List<InventoryTransaction> getAll() {
        return inventoryTransactionMapper.selectAll();
    }

    @Override
    public PageResult<InventoryTransaction> getPage(
            String materialCode,
            String materialName,
            String category,
            String txnType,
            String projectId,
            Long warehouseId,
            String sourceRefId,
            int page,
            int pageSize
    ) {
        return PageResult.paginate(
                page,
                pageSize,
                () -> inventoryTransactionMapper.countPage(
                        materialCode, materialName, category, txnType, projectId, warehouseId, sourceRefId),
                (offset, limit) -> inventoryTransactionMapper.selectPage(
                        materialCode, materialName, category, txnType, projectId, warehouseId, sourceRefId, offset, limit)
        );
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
    @Transactional
    public InventoryTransaction create(InventoryTransaction inventoryTransaction) {
        if (isWarehouseDocRef(inventoryTransaction.getSourceRefType())) {
            throw new IllegalArgumentException("不允许手工创建关联出入库单的库存流水，请在出入库单中维护");
        }
        validateTransaction(inventoryTransaction, true);
        normalizeBlankRefs(inventoryTransaction);
        if (currentUserService != null) {
            inventoryTransaction.setCreatedBy(currentUserService.requireCurrentUserId());
        }
        if (!StringUtils.hasText(inventoryTransaction.getUuid())) {
            inventoryTransaction.setUuid(UUID.randomUUID().toString());
        }
        inventoryTransactionMapper.insert(inventoryTransaction);
        assertNonNegativeStock(inventoryTransaction.getWarehouseId(), inventoryTransaction.getMaterialId());
        return inventoryTransaction;
    }

    @Override
    @Transactional
    public InventoryTransaction createForWarehouseDocument(InventoryTransaction inventoryTransaction) {
        validateTransaction(inventoryTransaction, false);
        if (!StringUtils.hasText(inventoryTransaction.getUuid())) {
            inventoryTransaction.setUuid(UUID.randomUUID().toString());
        }
        inventoryTransactionMapper.insert(inventoryTransaction);
        return inventoryTransaction;
    }

    @Override
    @Transactional
    public InventoryTransaction update(InventoryTransaction inventoryTransaction) {
        if (!StringUtils.hasText(inventoryTransaction.getUuid())) {
            throw new IllegalArgumentException("Inventory transaction UUID is required");
        }
        InventoryTransaction existing = inventoryTransactionMapper.selectByUuid(inventoryTransaction.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Inventory transaction does not exist");
        }
        if (isWarehouseDocRef(existing.getSourceRefType()) || isWarehouseDocRef(inventoryTransaction.getSourceRefType())) {
            throw new IllegalArgumentException("由出入库单生成的库存流水不允许直接修改，请在出入库单中维护");
        }
        boolean materialChanged = !Objects.equals(existing.getMaterialId(), inventoryTransaction.getMaterialId());
        validateTransaction(inventoryTransaction, materialChanged);
        normalizeBlankRefs(inventoryTransaction);
        if (currentUserService != null) {
            inventoryTransaction.setUpdatedBy(currentUserService.requireCurrentUserId());
        }
        inventoryTransactionMapper.updateByUuid(inventoryTransaction);
        Set<StockKey> affected = new LinkedHashSet<>();
        affected.add(new StockKey(existing.getWarehouseId(), existing.getMaterialId()));
        affected.add(new StockKey(inventoryTransaction.getWarehouseId(), inventoryTransaction.getMaterialId()));
        for (StockKey key : affected) {
            assertNonNegativeStock(key.warehouseId(), key.materialId());
        }
        return inventoryTransactionMapper.selectByUuid(inventoryTransaction.getUuid());
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        if (!StringUtils.hasText(uuid)) {
            return;
        }
        InventoryTransaction existing = inventoryTransactionMapper.selectByUuid(uuid);
        if (existing == null) {
            return;
        }
        if (isWarehouseDocRef(existing.getSourceRefType())) {
            throw new IllegalArgumentException("由出入库单生成的库存流水不允许直接删除，请删除对应出入库单");
        }
        inventoryTransactionMapper.deleteByUuid(uuid);
        assertNonNegativeStock(existing.getWarehouseId(), existing.getMaterialId());
    }

    private void validateTransaction(InventoryTransaction inventoryTransaction, boolean requireActiveMaterial) {
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
        inventoryTransaction.setCategory(inventoryTransaction.getCategory().trim().toUpperCase(Locale.ROOT));
        if (!ALLOWED_CATEGORIES.contains(inventoryTransaction.getCategory())) {
            throw new IllegalArgumentException("Unsupported business category");
        }
        if (!StringUtils.hasText(inventoryTransaction.getTxnType())) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        inventoryTransaction.setTxnType(inventoryTransaction.getTxnType().trim().toUpperCase(Locale.ROOT));
        if (!ALLOWED_TXN_TYPES.contains(inventoryTransaction.getTxnType())) {
            throw new IllegalArgumentException("Transaction type must be IN or OUT");
        }
        if (inventoryTransaction.getQuantity() == null || inventoryTransaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (inventoryTransaction.getUnitPrice() != null && inventoryTransaction.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (StringUtils.hasText(inventoryTransaction.getProjectId())) {
            inventoryTransaction.setProjectId(inventoryTransaction.getProjectId().trim());
            if (projectMapper.selectByUuid(inventoryTransaction.getProjectId()) == null) {
                throw new IllegalArgumentException("Project does not exist");
            }
        } else {
            inventoryTransaction.setProjectId(null);
        }

        MaterialMaster materialMaster = materialMasterMapper.selectByUuid(inventoryTransaction.getMaterialId());
        if (materialMaster == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        if (requireActiveMaterial && Boolean.FALSE.equals(materialMaster.getIsActive())) {
            throw new IllegalArgumentException("Material is disabled");
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

    private BigDecimal calculateAmount(InventoryTransaction inventoryTransaction) {
        if (inventoryTransaction.getUnitPrice() == null || inventoryTransaction.getQuantity() == null) {
            return null;
        }
        return inventoryTransaction.getUnitPrice()
                .multiply(inventoryTransaction.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void normalizeBlankRefs(InventoryTransaction txn) {
        txn.setSourceRefType(StringUtils.hasText(txn.getSourceRefType()) ? txn.getSourceRefType().trim().toUpperCase(Locale.ROOT) : null);
        txn.setSourceRefId(StringUtils.hasText(txn.getSourceRefId()) ? txn.getSourceRefId().trim() : null);
    }

    private boolean isWarehouseDocRef(String sourceRefType) {
        return StringUtils.hasText(sourceRefType)
                && "WAREHOUSE_DOC".equalsIgnoreCase(sourceRefType.trim());
    }

    private void assertNonNegativeStock(Long warehouseId, String materialId) {
        if (warehouseId == null || !StringUtils.hasText(materialId) || isAllowNegativeStock()) {
            return;
        }
        BigDecimal balance = inventoryTransactionMapper.selectWarehouseMaterialBalance(warehouseId, materialId);
        if (balance != null && balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("库存不足：该仓库物料可用库存不足，当前操作将导致负库存（剩余可用 " + balance.stripTrailingZeros().toPlainString() + "）");
        }
    }

    private boolean isAllowNegativeStock() {
        if (systemConfigService == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(systemConfigService.getString(CONFIG_ALLOW_NEGATIVE_STOCK, "false")).trim());
    }

    private record StockKey(Long warehouseId, String materialId) {
    }
}
