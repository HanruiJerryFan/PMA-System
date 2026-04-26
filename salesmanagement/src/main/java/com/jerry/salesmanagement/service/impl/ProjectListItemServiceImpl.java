package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProjectListItemMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.ProjectListItem;
import com.jerry.salesmanagement.pojo.dto.ExcelImportError;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.ProjectListItemService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectListItemServiceImpl implements ProjectListItemService {

    private static final String IMPORT_MODE_FAIL_FAST = "FAIL_FAST";
    private static final String IMPORT_MODE_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";

    private static final Set<String> SOURCE_TYPES = Set.of(
            "PROJECT_PURCHASE",
            "WAREHOUSE_TRANSFER_TO_PROJECT"
    );

    @Autowired
    private ProjectListItemMapper mapper;

    @Autowired
    private ProjectListMapper projectListMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private MaterialMasterMapper materialMasterMapper;

    @Autowired
    private ProductBrandMapper productBrandMapper;

    @Autowired
    private CustomerService customerService;

    @Override
    public ProjectListItem getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<ProjectListItem> getByProjectListId(String projectListId) {
        return mapper.selectByProjectListId(projectListId);
    }

    @Override
    @Transactional
    public ProjectListItem create(ProjectListItem item) {
        ProjectList projectList = validateItem(item);
        if (!StringUtils.hasText(item.getUuid())) {
            item.setUuid(UUID.randomUUID().toString());
        }
        item.setTotalAmount(calculateTotalAmount(item));
        mapper.insert(item);
        touchProjectListCustomer(projectList);
        return item;
    }

    @Override
    @Transactional
    public ProjectListItem update(ProjectListItem item) {
        ProjectList projectList = validateItem(item);
        item.setTotalAmount(calculateTotalAmount(item));
        mapper.updateByUuid(item);
        touchProjectListCustomer(projectList);
        return item;
    }

    @Override
    public void delete(String uuid) {
        mapper.deleteByUuid(uuid);
    }

    @Override
    public void deleteByProjectListId(String projectListId) {
        mapper.deleteByProjectListId(projectListId);
    }

    @Override
    @Transactional
    public ExcelImportResult importFromExcel(String projectListId, MultipartFile file, boolean replaceExisting, String importMode) {
        if (!StringUtils.hasText(projectListId)) {
            throw new IllegalArgumentException("Project list is required");
        }
        if (projectListMapper.selectByUuid(projectListId) == null) {
            throw new IllegalArgumentException("Project list does not exist");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        String normalizedImportMode = normalizeImportMode(importMode);

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet is empty");
            }

            int headerRowIndex = findHeaderRowIndex(sheet);
            if (headerRowIndex < 0) {
                throw new IllegalArgumentException("Header row was not found");
            }

            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(sheet.getRow(headerRowIndex));
            if (!headerIndexMap.containsKey("itemname") || !headerIndexMap.containsKey("quantity")) {
                throw new IllegalArgumentException("Excel must contain Item Name and Quantity columns");
            }

            int totalRows = 0;
            int importedRows = 0;
            int skippedRows = 0;
            List<ProjectListItem> validItems = new java.util.ArrayList<>();
            List<ExcelImportError> errors = new java.util.ArrayList<>();

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyDataRow(row, headerIndexMap)) {
                    skippedRows++;
                    continue;
                }

                totalRows++;
                Map<String, String> rowData = extractRowData(row, headerIndexMap);
                try {
                    ProjectListItem item = mapRowToItem(projectListId, row, headerIndexMap, rowIndex + 1);
                    validateItem(item);
                    validItems.add(item);
                } catch (Exception e) {
                    errors.add(buildImportError(rowIndex + 1, e.getMessage(), rowData));
                }
            }

            if (IMPORT_MODE_FAIL_FAST.equals(normalizedImportMode) && !errors.isEmpty()) {
                return buildImportResult(projectListId, normalizedImportMode, replaceExisting, totalRows, 0, skippedRows, errors);
            }

            if (replaceExisting && !validItems.isEmpty()) {
                deleteByProjectListId(projectListId);
            }

            for (ProjectListItem item : validItems) {
                create(item);
                importedRows++;
            }

            return buildImportResult(projectListId, normalizedImportMode, replaceExisting, totalRows, importedRows, skippedRows, errors);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Excel file: " + e.getMessage(), e);
        }
    }

    private ProjectList validateItem(ProjectListItem item) {
        if (!StringUtils.hasText(item.getProjectListId())) {
            throw new IllegalArgumentException("Project list is required");
        }
        ProjectList projectList = projectListMapper.selectByUuid(item.getProjectListId());
        if (projectList == null) {
            throw new IllegalArgumentException("Project list does not exist");
        }
        hydrateMaterialFields(item);
        if (!StringUtils.hasText(item.getItemName())) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (item.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        boolean allowNegativeQuantity = "CHANGE".equals(projectList.getListType());
        if (allowNegativeQuantity) {
            if (item.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Change list quantity cannot be 0");
            }
        } else if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (StringUtils.hasText(item.getMaterialCode()) && item.getMaterialCode().trim().length() != 12) {
            throw new IllegalArgumentException("Material code must be 12 characters");
        }
        if (StringUtils.hasText(item.getSourceType()) && !SOURCE_TYPES.contains(item.getSourceType())) {
            throw new IllegalArgumentException("Source type is invalid");
        }
        if (StringUtils.hasText(item.getMaterialId())
                && materialMasterMapper.selectByUuid(item.getMaterialId()) == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        return projectList;
    }

    private void touchProjectListCustomer(ProjectList projectList) {
        if (projectList == null || !StringUtils.hasText(projectList.getProjectId())) {
            return;
        }
        Project project = projectMapper.selectByUuid(projectList.getProjectId());
        if (project != null && StringUtils.hasText(project.getCustomerId())) {
            customerService.touchActivity(project.getCustomerId());
        }
    }

    private void hydrateMaterialFields(ProjectListItem item) {
        if (StringUtils.hasText(item.getMaterialCode())) {
            String normalizedMaterialCode = item.getMaterialCode().trim().toUpperCase(Locale.ROOT);
            item.setMaterialCode(normalizedMaterialCode);
            if (normalizedMaterialCode.length() != 12) {
                throw new IllegalArgumentException("Material code must be 12 characters");
            }
            MaterialMaster material = materialMasterMapper.selectByMaterialCode(normalizedMaterialCode);
            if (material == null) {
                throw new IllegalArgumentException("Material code does not exist");
            }
            hydrateSnapshotFields(item, material);
            return;
        }

        if (StringUtils.hasText(item.getMaterialId())) {
            MaterialMaster material = materialMasterMapper.selectByUuid(item.getMaterialId());
            if (material == null) {
                throw new IllegalArgumentException("Material does not exist");
            }
            hydrateSnapshotFields(item, material);
        }
    }

    private void hydrateSnapshotFields(ProjectListItem item, MaterialMaster material) {
        item.setMaterialId(material.getUuid());
        item.setMaterialCode(material.getMaterialCode());
        item.setItemName(material.getProductName());
        item.setModel(material.getProductModel());
        item.setBrand(resolveBrandName(material));
        item.setUnit(material.getUnit());
    }

    private String resolveBrandName(MaterialMaster material) {
        if (material.getBrandId() != null) {
            ProductBrand brand = productBrandMapper.selectById(material.getBrandId());
            if (brand != null && StringUtils.hasText(brand.getName())) {
                return brand.getName();
            }
        }
        if (StringUtils.hasText(material.getBrandCode())) {
            return material.getBrandCode();
        }
        return null;
    }

    private BigDecimal calculateTotalAmount(ProjectListItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return null;
        }
        return item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
    }

    private int findHeaderRowIndex(Sheet sheet) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(row);
            if (headerIndexMap.containsKey("itemname") && headerIndexMap.containsKey("quantity")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow) {
        Map<String, Integer> headerIndexMap = new HashMap<>();
        if (headerRow == null) {
            return headerIndexMap;
        }

        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(formatter.formatCellValue(cell));
            if (!normalized.isEmpty()) {
                headerIndexMap.putIfAbsent(normalized, cell.getColumnIndex());
            }
        }
        return headerIndexMap;
    }

    private String normalizeHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private Map<String, String> extractRowData(Row row, Map<String, Integer> headerIndexMap) {
        Map<String, String> rowData = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        headerIndexMap.forEach((key, columnIndex) -> {
            String value = row != null && row.getCell(columnIndex) != null
                    ? formatter.formatCellValue(row.getCell(columnIndex)).trim()
                    : "";
            rowData.put(key, value);
        });
        return rowData;
    }

    private boolean isEmptyDataRow(Row row, Map<String, Integer> headerIndexMap) {
        if (row == null) {
            return true;
        }

        DataFormatter formatter = new DataFormatter();
        String[] keys = {"materialcode", "itemname", "model", "brand", "unit", "quantity", "unitprice", "remark"};
        for (String key : keys) {
            Integer columnIndex = headerIndexMap.get(key);
            if (columnIndex == null) {
                continue;
            }
            String value = formatter.formatCellValue(row.getCell(columnIndex)).trim();
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ProjectListItem mapRowToItem(
            String projectListId,
            Row row,
            Map<String, Integer> headerIndexMap,
            int excelRowNumber
    ) {
        DataFormatter formatter = new DataFormatter();
        ProjectListItem item = new ProjectListItem();
        item.setProjectListId(projectListId);
        item.setMaterialId(getCellValue(row, headerIndexMap, "materialid", formatter));
        item.setMaterialCode(getCellValue(row, headerIndexMap, "materialcode", formatter));
        item.setItemName(getCellValue(row, headerIndexMap, "itemname", formatter));
        item.setModel(getCellValue(row, headerIndexMap, "model", formatter));
        item.setBrand(getCellValue(row, headerIndexMap, "brand", formatter));
        item.setUnit(getCellValue(row, headerIndexMap, "unit", formatter));
        item.setSourceType(getCellValue(row, headerIndexMap, "sourcetype", formatter));
        item.setRemark(getCellValue(row, headerIndexMap, "remark", formatter));
        item.setQuantity(parseDecimal(
                getCellValue(row, headerIndexMap, "quantity", formatter),
                "Quantity",
                excelRowNumber,
                true
        ));
        item.setUnitPrice(parseDecimal(
                getCellValue(row, headerIndexMap, "unitprice", formatter),
                "Unit Price",
                excelRowNumber,
                false
        ));
        return item;
    }

    private String getCellValue(Row row, Map<String, Integer> headerIndexMap, String key, DataFormatter formatter) {
        Integer columnIndex = headerIndexMap.get(key);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal parseDecimal(String rawValue, String fieldName, int excelRowNumber, boolean required) {
        if (!StringUtils.hasText(rawValue)) {
            if (required) {
                throw new IllegalArgumentException("Row " + excelRowNumber + ": " + fieldName + " is required");
            }
            return null;
        }

        try {
            return new BigDecimal(rawValue.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Row " + excelRowNumber + ": invalid " + fieldName);
        }
    }

    private String normalizeImportMode(String importMode) {
        String normalized = StringUtils.hasText(importMode) ? importMode.trim().toUpperCase(Locale.ROOT) : IMPORT_MODE_PARTIAL_SUCCESS;
        if (!IMPORT_MODE_FAIL_FAST.equals(normalized) && !IMPORT_MODE_PARTIAL_SUCCESS.equals(normalized)) {
            throw new IllegalArgumentException("Import mode must be FAIL_FAST or PARTIAL_SUCCESS");
        }
        return normalized;
    }

    private ExcelImportError buildImportError(int rowNumber, String message, Map<String, String> rowData) {
        ExcelImportError error = new ExcelImportError();
        error.setRowNumber(rowNumber);
        error.setMessage(message);
        error.setRowData(rowData);
        return error;
    }

    private ExcelImportResult buildImportResult(
            String projectListId,
            String importMode,
            boolean replaceExisting,
            int totalRows,
            int importedRows,
            int skippedRows,
            List<ExcelImportError> errors
    ) {
        ExcelImportResult result = new ExcelImportResult();
        result.setResourceType("PROJECT_LIST_ITEM");
        result.setContextId(projectListId);
        result.setImportMode(importMode);
        result.setReplaceExisting(replaceExisting);
        result.setTotalRows(totalRows);
        result.setImportedRows(importedRows);
        result.setSkippedRows(skippedRows);
        result.setFailedRows(errors.size());
        result.setErrors(errors);
        return result;
    }
}
