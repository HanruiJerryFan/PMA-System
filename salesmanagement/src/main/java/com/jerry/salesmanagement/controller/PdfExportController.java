package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.DownloadResponseBuilder;
import com.jerry.salesmanagement.pojo.PdfTableExportRequest;
import com.jerry.salesmanagement.service.TablePdfExportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/pdf-exports")
@CrossOrigin(exposedHeaders = "Content-Disposition")
public class PdfExportController {

    private final TablePdfExportService tablePdfExportService;

    public PdfExportController(TablePdfExportService tablePdfExportService) {
        this.tablePdfExportService = tablePdfExportService;
    }

    @PostMapping(value = "/table", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportTable(@RequestBody PdfTableExportRequest request) {
        try {
            byte[] pdf = tablePdfExportService.exportTable(request);
            return DownloadResponseBuilder.build(normalizeFileName(request), MediaType.APPLICATION_PDF, pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Failed to export PDF: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String normalizeFileName(PdfTableExportRequest request) {
        String fileName = request != null ? request.getFileName() : null;
        if (fileName == null || fileName.isBlank()) {
            fileName = request != null && request.getTitle() != null && !request.getTitle().isBlank()
                    ? request.getTitle().trim()
                    : "export";
        }
        return fileName.toLowerCase().endsWith(".pdf") ? fileName : fileName + ".pdf";
    }
}
