package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.WarehouseDocument;

public interface WarehouseDocumentPdfExportService {
    default byte[] exportPdf(WarehouseDocument warehouseDocument, String projectName) {
        return exportPdf(warehouseDocument, projectName, true);
    }

    byte[] exportPdf(WarehouseDocument warehouseDocument, String projectName, boolean includePrice);
}
