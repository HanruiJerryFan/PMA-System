package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.dto.PageResult;

import java.util.List;

public interface InventoryTransactionService {
    List<InventoryTransaction> getAll();

    PageResult<InventoryTransaction> getPage(
            String materialCode,
            String materialName,
            String category,
            String txnType,
            String projectId,
            Long warehouseId,
            String sourceRefId,
            int page,
            int pageSize
    );

    InventoryTransaction getByUuid(String uuid);

    List<InventoryTransaction> getByContractUuid(String contractUuid);

    InventoryTransaction create(InventoryTransaction inventoryTransaction);

    InventoryTransaction createForWarehouseDocument(InventoryTransaction inventoryTransaction);

    InventoryTransaction update(InventoryTransaction inventoryTransaction);

    void delete(String uuid);
}
