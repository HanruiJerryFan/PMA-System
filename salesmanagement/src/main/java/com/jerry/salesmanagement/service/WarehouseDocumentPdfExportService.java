package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.WarehouseDocument;

public interface WarehouseDocumentPdfExportService {
    byte[] exportPdf(WarehouseDocument warehouseDocument, String projectLabel);
}
