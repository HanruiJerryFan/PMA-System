package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.WarehouseDocument;

import java.util.List;

public interface WarehouseDocumentService {
    List<WarehouseDocument> getAll();

    WarehouseDocument getByDocNumber(String docNumber);

    WarehouseDocument create(WarehouseDocument warehouseDocument);

    WarehouseDocument update(WarehouseDocument warehouseDocument);

    void delete(String docNumber);
}
