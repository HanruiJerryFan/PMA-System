package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ExcelImportError {
    private int rowNumber;
    private String message;
    private Map<String, String> rowData;
}
