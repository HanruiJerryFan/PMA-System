package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.WarehouseDocument;

public interface WarehouseDocumentExcelExportService {
    default byte[] exportExcel(WarehouseDocument warehouseDocument, String projectName) {
        return exportExcel(warehouseDocument, projectName, true);
    }

    byte[] exportExcel(WarehouseDocument warehouseDocument, String projectName, boolean includePrice);
}
