package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.common.DownloadResponseBuilder;
import com.jerry.salesmanagement.pojo.ProjectListItem;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import com.jerry.salesmanagement.service.ExcelImportTemplateService;
import com.jerry.salesmanagement.service.ProjectListItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/project-list-items")
@CrossOrigin
public class ProjectListItemController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Autowired
    private ProjectListItemService service;

    @Autowired
    private ExcelImportTemplateService excelImportTemplateService;

    @GetMapping("/byList/{projectListId}")
    public ApiResponse<List<ProjectListItem>> getByProjectListId(@PathVariable String projectListId) {
        try {
            return ApiResponse.success(service.getByProjectListId(projectListId));
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch project list items: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('project.list.entry', 'project.manage')")
    public ApiResponse<ProjectListItem> create(@RequestBody ProjectListItem item) {
        try {
            return ApiResponse.success("Project list item created successfully", service.create(item));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create project list item: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('project.list.entry', 'project.manage')")
    public ApiResponse<ProjectListItem> update(@PathVariable String uuid, @RequestBody ProjectListItem item) {
        try {
            item.setUuid(uuid);
            return ApiResponse.success("Project list item updated successfully", service.update(item));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update project list item: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('project.list.entry', 'project.manage')")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            service.delete(uuid);
            return ApiResponse.success("Project list item deleted successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete project list item: " + e.getMessage());
        }
    }

    @PostMapping("/import/{projectListId}")
    @PreAuthorize("hasAnyAuthority('project.list.entry', 'project.manage')")
    public ApiResponse<ExcelImportResult> importExcel(
            @PathVariable String projectListId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean replaceExisting,
            @RequestParam(defaultValue = "PARTIAL_SUCCESS") String importMode
    ) {
        try {
            return ApiResponse.success(
                    "Project list items imported successfully",
                    service.importFromExcel(projectListId, file, replaceExisting, importMode)
            );
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Failed to import project list items: " + e.getMessage());
        }
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        return DownloadResponseBuilder.build(
                "project-list-items-import-template.xlsx",
                XLSX_MEDIA_TYPE,
                excelImportTemplateService.buildProjectListItemTemplate()
        );
    }
}
