package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PdfTableExportRequest {
    private String title;
    private String subtitle;
    private String fileName;
    private String orientation;
    private List<PdfTableKeyValue> metadata = new ArrayList<>();
    private List<PdfTableKeyValue> summaries = new ArrayList<>();
    private List<PdfTableColumn> columns = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
    private List<PdfTableSection> extraSections = new ArrayList<>();
}
