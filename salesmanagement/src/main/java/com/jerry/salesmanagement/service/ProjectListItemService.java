package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProjectListItem;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectListItemService {
    ProjectListItem getByUuid(String uuid);
    List<ProjectListItem> getByProjectListId(String projectListId);
    ProjectListItem create(ProjectListItem item);
    ProjectListItem update(ProjectListItem item);
    void delete(String uuid);
    void deleteByProjectListId(String projectListId);
    ExcelImportResult importFromExcel(String projectListId, MultipartFile file, boolean replaceExisting, String importMode);
}
