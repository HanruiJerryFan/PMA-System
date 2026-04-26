package com.jerry.salesmanagement.service;

public interface ExcelImportTemplateService {
    byte[] buildMaterialMasterTemplate();

    byte[] buildProjectListItemTemplate();
}
