package com.jerry.salesmanagement.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PdfTableSection {
    private String title;
    private List<PdfTableColumn> columns = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
}
