package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExcelImportResult {
    private String resourceType;
    private String contextId;
    private String importMode;
    private boolean replaceExisting;
    private int totalRows;
    private int importedRows;
    private int skippedRows;
    private int failedRows;
    private List<ExcelImportError> errors = new ArrayList<>();
}
