package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.PdfTableExportRequest;

public interface TablePdfExportService {
    byte[] exportTable(PdfTableExportRequest request);
}
