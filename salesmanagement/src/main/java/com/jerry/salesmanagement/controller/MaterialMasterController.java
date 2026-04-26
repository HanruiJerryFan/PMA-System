package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import com.jerry.salesmanagement.service.MaterialMasterService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/materials")
@CrossOrigin
public class MaterialMasterController {

    @Autowired
    private MaterialMasterService materialMasterService;

    @GetMapping
    public ApiResponse<List<MaterialMaster>> getAll() {
        try {
            return ApiResponse.success(materialMasterService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch materials: " + e.getMessage());
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
            return ApiResponse.success("Material created", materialMasterService.create(materialMaster));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create material: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<MaterialMaster> update(@PathVariable String uuid, @RequestBody MaterialMaster materialMaster) {
        try {
            materialMaster.setUuid(uuid);
            return ApiResponse.success("Material updated", materialMasterService.update(materialMaster));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update material: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            materialMasterService.delete(uuid);
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
            return ApiResponse.success("Material import completed", materialMasterService.importFromExcel(file, importMode));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Failed to import materials: " + e.getMessage());
        }
    }
}
