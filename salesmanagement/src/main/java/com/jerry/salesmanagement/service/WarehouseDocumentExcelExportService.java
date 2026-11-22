package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.WarehouseDocument;

public interface WarehouseDocumentExcelExportService {
    byte[] exportExcel(WarehouseDocument warehouseDocument, String projectName);
}
