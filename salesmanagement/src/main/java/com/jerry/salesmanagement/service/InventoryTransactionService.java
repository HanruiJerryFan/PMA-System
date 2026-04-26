package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.InventoryTransaction;

import java.util.List;

public interface InventoryTransactionService {
    List<InventoryTransaction> getAll();

    InventoryTransaction getByUuid(String uuid);

    List<InventoryTransaction> getByContractUuid(String contractUuid);

    InventoryTransaction create(InventoryTransaction inventoryTransaction);

    InventoryTransaction update(InventoryTransaction inventoryTransaction);

    void delete(String uuid);
}
