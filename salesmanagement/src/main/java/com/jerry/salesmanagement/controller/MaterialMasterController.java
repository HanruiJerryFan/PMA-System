package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.common.DownloadResponseBuilder;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ExcelImportTemplateService;
import com.jerry.salesmanagement.service.MaterialMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/materials")
public class MaterialMasterController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Autowired
    private MaterialMasterService materialMasterService;

    @Autowired
    private ExcelImportTemplateService excelImportTemplateService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<MaterialMaster>> getAll() {
        try {
            return ApiResponse.success(materialMasterService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch materials: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<MaterialMaster>> getPage(
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String productModel,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            return ApiResponse.success(materialMasterService.getPage(
                    materialCode, productName, productModel, brandId, isActive, page, pageSize
            ));
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch materials page: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResponse<List<MaterialMaster>> getOptions() {
        try {
            return ApiResponse.success(materialMasterService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch material options: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<MaterialMaster> getByUuid(@PathVariable String uuid) {
        try {
            MaterialMaster materialMaster = materialMasterService.getByUuid(uuid);
            return materialMaster != null ? ApiResponse.success(materialMaster) : ApiResponse.notFound("Material not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch material: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<MaterialMaster> create(@RequestBody MaterialMaster materialMaster) {
        try {
            MaterialMaster created = materialMasterService.create(materialMaster);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("materials", "CREATE", "materials", created.getUuid(), "Created material");
            }
            return ApiResponse.success("Material created", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create material: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<MaterialMaster> update(@PathVariable String uuid, @RequestBody MaterialMaster materialMaster) {
        try {
            materialMaster.setUuid(uuid);
            MaterialMaster updated = materialMasterService.update(materialMaster);
            if (auditTrailService != null) {
                auditTrailService.record("materials", "UPDATE", "materials", uuid, "Updated material");
            }
            return ApiResponse.success("Material updated", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update material: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            materialMasterService.delete(uuid);
            if (auditTrailService != null) {
                auditTrailService.record("materials", "DELETE", "materials", uuid, "Deleted material");
            }
            return ApiResponse.success("Material deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete material: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    public ApiResponse<ExcelImportResult> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PARTIAL_SUCCESS") String importMode
    ) {
        try {
            ExcelImportResult result = materialMasterService.importFromExcel(file, importMode);
            if (auditTrailService != null && result != null && result.getImportedRows() > 0) {
                auditTrailService.record("materials", "IMPORT", "materials", "batch",
                        "Imported " + result.getImportedRows() + " materials");
            }
            return ApiResponse.success("Material import completed", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Failed to import materials: " + e.getMessage());
        }
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        return DownloadResponseBuilder.build(
                "materials-import-template.xlsx",
                XLSX_MEDIA_TYPE,
                excelImportTemplateService.buildMaterialMasterTemplate()
        );
    }
}
