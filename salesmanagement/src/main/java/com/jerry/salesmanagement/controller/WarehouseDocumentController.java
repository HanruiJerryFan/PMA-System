package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.common.DownloadResponseBuilder;
import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentCustomerOption;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.SystemConfigService;
import com.jerry.salesmanagement.service.WarehouseDocumentExcelExportService;
import com.jerry.salesmanagement.service.WarehouseDocumentPdfExportService;
import com.jerry.salesmanagement.service.WarehouseDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/warehouse-documents")
public class WarehouseDocumentController {

    private static final Set<String> SUPPLIER_TYPE_CODES = Set.of("3", "4");
    private static final Set<String> RECEIVER_TYPE_CODES = Set.of("2", "3", "4", "5");

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Autowired
    private WarehouseDocumentService warehouseDocumentService;

    @Autowired
    private WarehouseDocumentPdfExportService warehouseDocumentPdfExportService;

    @Autowired
    private WarehouseDocumentExcelExportService warehouseDocumentExcelExportService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @Autowired
    private MaterialMasterMapper materialMasterMapper;

    @Autowired
    private ProductBrandMapper productBrandMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerTypeMapper customerTypeMapper;

    @GetMapping
    public ApiResponse<List<WarehouseDocument>> getAll() {
        try {
            return ApiResponse.success(warehouseDocumentService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query warehouse documents: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResponse<List<WarehouseDocument>> getOptions() {
        try {
            return ApiResponse.success(warehouseDocumentService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query warehouse document options: " + e.getMessage());
        }
    }

    @GetMapping("/material-options")
    public ApiResponse<List<MaterialMaster>> getMaterialOptions() {
        try {
            Map<Long, String> brandNameMap = productBrandMapper.selectAll()
                    .stream()
                    .collect(Collectors.toMap(ProductBrand::getId, ProductBrand::getName));
            List<MaterialMaster> materials = materialMasterMapper.selectAll();
            materials.forEach(material -> {
                if (material.getBrandId() != null) {
                    material.setBrandName(brandNameMap.get(material.getBrandId()));
                }
            });
            return ApiResponse.success(materials);
        } catch (Exception e) {
            return ApiResponse.error("Failed to query warehouse document material options: " + e.getMessage());
        }
    }

    @GetMapping("/project-options")
    public ApiResponse<List<Project>> getProjectOptions() {
        try {
            return ApiResponse.success(projectMapper.selectAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query warehouse document project options: " + e.getMessage());
        }
    }

    @GetMapping("/customer-options")
    public ApiResponse<List<WarehouseDocumentCustomerOption>> getCustomerOptions() {
        try {
            Map<Long, CustomerType> customerTypeMap = customerTypeMapper.selectAll()
                    .stream()
                    .collect(Collectors.toMap(CustomerType::getId, Function.identity()));
            List<WarehouseDocumentCustomerOption> options = customerMapper.selectAll()
                    .stream()
                    .map(customer -> {
                        CustomerType customerType = customer.getCustomerTypeId() != null
                                ? customerTypeMap.get(customer.getCustomerTypeId())
                                : null;
                        if (customerType == null) {
                            return null;
                        }
                        String typeCode = customerType.getTypeCode();
                        if (!SUPPLIER_TYPE_CODES.contains(typeCode) && !RECEIVER_TYPE_CODES.contains(typeCode)) {
                            return null;
                        }
                        WarehouseDocumentCustomerOption option = new WarehouseDocumentCustomerOption();
                        option.setUuid(customer.getUuid());
                        option.setCustomerCode(customer.getCustomerCode());
                        option.setCustomerName(customer.getCustomerName());
                        option.setCustomerTypeId(customer.getCustomerTypeId());
                        option.setCustomerTypeCode(customerType.getTypeCode());
                        option.setCustomerTypeName(customerType.getTypeName());
                        option.setOfficeAddress(customer.getOfficeAddress());
                        option.setRegisterAddress(customer.getRegisterAddress());
                        return option;
                    })
                    .filter(option -> option != null)
                    .collect(Collectors.toList());
            return ApiResponse.success(options);
        } catch (Exception e) {
            return ApiResponse.error("Failed to query warehouse document customer options: " + e.getMessage());
        }
    }

    @GetMapping("/print-config")
    public ApiResponse<Map<String, String>> getPrintConfig() {
        try {
            return ApiResponse.success(Map.of(
                    "companyName", WarehouseDocumentPrintSupport.resolveCompanyName(systemConfigService)
            ));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query warehouse document print config: " + e.getMessage());
        }
    }

    @GetMapping("/{docNumber}")
    public ApiResponse<WarehouseDocument> getByDocNumber(@PathVariable String docNumber) {
        try {
            WarehouseDocument warehouseDocument = warehouseDocumentService.getByDocNumber(docNumber);
            return warehouseDocument != null
                    ? ApiResponse.success(warehouseDocument)
                    : ApiResponse.notFound("Warehouse document not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query warehouse document: " + e.getMessage());
        }
    }

    @GetMapping(value = "/{docNumber}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable String docNumber) {
        try {
            WarehouseDocument warehouseDocument = warehouseDocumentService.getByDocNumber(docNumber);
            if (warehouseDocument == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] pdf = warehouseDocumentPdfExportService.exportPdf(
                    warehouseDocument,
                    resolveProjectName(warehouseDocument)
            );
            return DownloadResponseBuilder.build(docNumber + ".pdf", MediaType.APPLICATION_PDF, pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Failed to export warehouse document PDF: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/{docNumber}/export-excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String docNumber) {
        try {
            WarehouseDocument warehouseDocument = warehouseDocumentService.getByDocNumber(docNumber);
            if (warehouseDocument == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] workbookBytes = warehouseDocumentExcelExportService.exportExcel(
                    warehouseDocument,
                    resolveProjectName(warehouseDocument)
            );
            return DownloadResponseBuilder.build(docNumber + ".xlsx", XLSX_MEDIA_TYPE, workbookBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Failed to export warehouse document Excel: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private String resolveProjectName(WarehouseDocument warehouseDocument) {
        Project project = warehouseDocument.getProjectId() != null
                ? projectMapper.selectByUuid(warehouseDocument.getProjectId())
                : null;
        return project != null && project.getProjectName() != null
                ? project.getProjectName().trim()
                : "";
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('inventory.warehouse-doc.entry', 'inventory.manage')")
    public ApiResponse<WarehouseDocument> create(@RequestBody WarehouseDocument warehouseDocument) {
        try {
            warehouseDocument.setDocNumber(null);
            WarehouseDocument created = warehouseDocumentService.create(warehouseDocument);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("warehouse-documents", "CREATE", "warehouse-documents", created.getDocNumber(), "Created warehouse document");
            }
            return ApiResponse.success(created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create warehouse document: " + e.getMessage());
        }
    }

    @PutMapping("/{docNumber}")
    @PreAuthorize("hasAnyAuthority('inventory.warehouse-doc.entry', 'inventory.manage')")
    public ApiResponse<WarehouseDocument> update(
            @PathVariable String docNumber,
            @RequestBody WarehouseDocument warehouseDocument
    ) {
        try {
            warehouseDocument.setDocNumber(docNumber);
            WarehouseDocument updated = warehouseDocumentService.update(warehouseDocument);
            if (auditTrailService != null) {
                auditTrailService.record("warehouse-documents", "UPDATE", "warehouse-documents", docNumber, "Updated warehouse document");
            }
            return ApiResponse.success(updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update warehouse document: " + e.getMessage());
        }
    }

    @PostMapping("/{docNumber}/audit")
    @PreAuthorize("hasAnyAuthority('inventory.warehouse-doc.audit', 'inventory.manage')")
    public ApiResponse<WarehouseDocument> audit(@PathVariable String docNumber) {
        try {
            WarehouseDocument audited = warehouseDocumentService.audit(docNumber);
            if (auditTrailService != null) {
                auditTrailService.record("warehouse-documents", "AUDIT", "warehouse-documents", docNumber, "Audited warehouse document");
            }
            return ApiResponse.success("Warehouse document audited", audited);
        } catch (Exception e) {
            return ApiResponse.error("Failed to audit warehouse document: " + e.getMessage());
        }
    }

    @DeleteMapping("/{docNumber}")
    @PreAuthorize("hasAnyAuthority('inventory.warehouse-doc.entry', 'inventory.manage')")
    public ApiResponse<Void> delete(@PathVariable String docNumber) {
        try {
            warehouseDocumentService.delete(docNumber);
            if (auditTrailService != null) {
                auditTrailService.record("warehouse-documents", "DELETE", "warehouse-documents", docNumber, "Deleted warehouse document");
            }
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete warehouse document: " + e.getMessage());
        }
    }
}
