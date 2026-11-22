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
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.EntryAuditService;
import com.jerry.salesmanagement.service.ProjectListItemService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectListItemServiceImpl implements ProjectListItemService {

    private static final String IMPORT_MODE_FAIL_FAST = "FAIL_FAST";
    private static final String IMPORT_MODE_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";

    private static final Set<String> ITEM_REFERENCE_HEADERS = Set.of("materialid", "materialcode", "itemname");
    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("materialuuid", "materialid"),
            Map.entry("物料id", "materialid"),
            Map.entry("材料id", "materialid"),
            Map.entry("物料编码", "materialcode"),
            Map.entry("物料代码", "materialcode"),
            Map.entry("材料编码", "materialcode"),
            Map.entry("材料代码", "materialcode"),
            Map.entry("materialname", "itemname"),
            Map.entry("productname", "itemname"),
            Map.entry("name", "itemname"),
            Map.entry("物料名称", "itemname"),
            Map.entry("材料名称", "itemname"),
            Map.entry("商品名称", "itemname"),
            Map.entry("名称", "itemname"),
            Map.entry("型号", "model"),
            Map.entry("规格型号", "model"),
            Map.entry("型号规格", "model"),
            Map.entry("品牌", "brand"),
            Map.entry("单位", "unit"),
            Map.entry("数量", "quantity"),
            Map.entry("qty", "quantity"),
            Map.entry("销售价格", "unitprice"),
            Map.entry("销售价", "unitprice"),
            Map.entry("销售单价", "unitprice"),
            Map.entry("采购价格", "unitprice"),
            Map.entry("采购价", "unitprice"),
            Map.entry("采购单价", "unitprice"),
            Map.entry("单价", "unitprice"),
            Map.entry("价格", "unitprice"),
            Map.entry("备注", "remark"),
            Map.entry("说明", "remark"),
            Map.entry("来源类型", "sourcetype")
    );

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

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private EntryAuditService entryAuditService;

    @Override
    public ProjectListItem getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<ProjectListItem> getByProjectListId(String projectListId) {
        return mapper.selectByProjectListId(projectListId);
    }

    @Override
    public List<ProjectListItem> getByProjectListIds(List<String> projectListIds) {
        if (projectListIds == null || projectListIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectByProjectListIds(projectListIds);
    }

    @Override
    @Transactional
    public ProjectListItem create(ProjectListItem item) {
        ProjectList projectList = validateItem(item, true);
        if (!StringUtils.hasText(item.getUuid())) {
            item.setUuid(UUID.randomUUID().toString());
        }
        item.setTotalAmount(calculateTotalAmount(item));
        Long currentUserId = currentUserService.requireCurrentUserId();
        entryAuditService.applyCreate(item);
        item.setCreateUser(currentUserId);
        mapper.insert(item);
        touchProjectListCustomer(projectList);
        return mapper.selectByUuid(item.getUuid());
    }

    @Override
    @Transactional
    public ProjectListItem update(ProjectListItem item) {
        ProjectListItem existing = mapper.selectByUuid(item.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Project list item does not exist");
        }
        if (StringUtils.hasText(item.getProjectListId())
                && !Objects.equals(existing.getProjectListId(), item.getProjectListId())) {
            throw new IllegalArgumentException("不允许将项目清单明细移动到其他清单");
        }
        if (!StringUtils.hasText(item.getProjectListId())) {
            item.setProjectListId(existing.getProjectListId());
        }
        ProjectList projectList = validateItem(item, true);
        item.setTotalAmount(calculateTotalAmount(item));
        Long currentUserId = currentUserService.requireCurrentUserId();
        entryAuditService.applyUpdate(item, existing);
        item.setUpdateUser(currentUserId);
        mapper.updateByUuid(item);
        touchProjectListCustomer(projectList);
        return mapper.selectByUuid(item.getUuid());
    }

    @Override
    @Transactional
    public ProjectListItem audit(String uuid) {
        ProjectListItem existing = mapper.selectByUuid(uuid);
        if (existing == null) {
            throw new IllegalArgumentException("Project list item does not exist");
        }
        entryAuditService.applyAudit(existing);
        Long currentUserId = currentUserService.requireCurrentUserId();
        mapper.updateAuditorByUuid(uuid, existing.getAuditorUser(), currentUserId);
        return mapper.selectByUuid(uuid);
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        ProjectListItem existing = mapper.selectByUuid(uuid);
        if (existing == null) {
            return;
        }
        ProjectList parentList = projectListMapper.selectByUuid(existing.getProjectListId());
        assertParentListUnlockedForItemMutation(parentList);
        if (entryAuditService != null) {
            entryAuditService.assertCanDelete(existing);
        }
        mapper.deleteByUuid(uuid);
    }

    @Override
    @Transactional
    public void deleteByProjectListId(String projectListId) {
        if (!StringUtils.hasText(projectListId)) {
            return;
        }
        if (entryAuditService != null) {
            for (ProjectListItem item : mapper.selectByProjectListId(projectListId)) {
                entryAuditService.assertCanDelete(item);
            }
        }
        mapper.deleteByProjectListId(projectListId);
    }

    @Override
    @Transactional
    public ExcelImportResult importFromExcel(String projectListId, MultipartFile file, boolean replaceExisting, String importMode) {
        if (!StringUtils.hasText(projectListId)) {
            throw new IllegalArgumentException("Project list is required");
        }
        ProjectList projectList = projectListMapper.selectByUuid(projectListId);
        if (projectList == null) {
            throw new IllegalArgumentException("Project list does not exist");
        }
        assertParentListUnlockedForItemMutation(projectList);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        String normalizedImportMode = normalizeImportMode(importMode);

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet is empty");
            }

            int headerRowIndex = findHeaderRowIndex(sheet, evaluator);
            if (headerRowIndex < 0) {
                throw new IllegalArgumentException("Header row was not found");
            }

            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(sheet.getRow(headerRowIndex), evaluator);
            if (!hasItemReferenceColumn(headerIndexMap) || !headerIndexMap.containsKey("quantity")) {
                throw new IllegalArgumentException("Excel must contain Quantity and one of Material ID, Material Code, or Item Name columns");
            }

            int totalRows = 0;
            int importedRows = 0;
            int skippedRows = 0;
            List<ProjectListItem> validItems = new ArrayList<>();
            List<ExcelImportError> errors = new ArrayList<>();

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyDataRow(row, headerIndexMap, evaluator)) {
                    skippedRows++;
                    continue;
                }

                totalRows++;
                Map<String, String> rowData = extractRowData(row, headerIndexMap, evaluator);
                try {
                    ProjectListItem item = mapRowToItem(projectListId, row, headerIndexMap, rowIndex + 1, evaluator);
                    validateItem(item, true);
                    validItems.add(item);
                } catch (Exception e) {
                    errors.add(buildImportError(rowIndex + 1, e.getMessage(), rowData));
                }
            }

            if (IMPORT_MODE_FAIL_FAST.equals(normalizedImportMode) && !errors.isEmpty()) {
                return buildImportResult(projectListId, normalizedImportMode, replaceExisting, totalRows, 0, skippedRows, errors);
            }

            if (replaceExisting && !errors.isEmpty()) {
                throw new IllegalArgumentException("覆盖导入不允许存在失败行，请先修正 Excel 中的 " + errors.size() + " 条错误行后重试（原清单明细未做任何删除）");
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

    private ProjectList validateItem(ProjectListItem item, boolean checkParentAuditLock) {
        if (!StringUtils.hasText(item.getProjectListId())) {
            throw new IllegalArgumentException("Project list is required");
        }
        ProjectList projectList = projectListMapper.selectByUuid(item.getProjectListId());
        if (projectList == null) {
            throw new IllegalArgumentException("Project list does not exist");
        }
        if (checkParentAuditLock) {
            assertParentListUnlockedForItemMutation(projectList);
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
        if (StringUtils.hasText(item.getMaterialCode())) {
            String code = extractLeadingToken(item.getMaterialCode());
            if (code.length() < 10 || code.length() > 12) {
                throw new IllegalArgumentException("Material code must be 10 to 12 characters");
            }
        }
        if (StringUtils.hasText(item.getSourceType())) {
            item.setSourceType(item.getSourceType().trim().toUpperCase(Locale.ROOT));
            if (!SOURCE_TYPES.contains(item.getSourceType())) {
                throw new IllegalArgumentException("Source type is invalid");
            }
        } else {
            item.setSourceType(null);
        }
        if (StringUtils.hasText(item.getMaterialId())
                && materialMasterMapper.selectByUuid(item.getMaterialId()) == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        item.setModel(trimToNull(item.getModel()));
        item.setBrand(trimToNull(item.getBrand()));
        item.setUnit(trimToNull(item.getUnit()));
        item.setRemark(trimToNull(item.getRemark()));
        return projectList;
    }

    private void assertParentListUnlockedForItemMutation(ProjectList projectList) {
        if (projectList != null && projectList.getAuditorUser() != null) {
            boolean canOverride = entryAuditService != null && entryAuditService.canOverrideEntryAuditUsers();
            if (!canOverride) {
                throw new IllegalArgumentException("所属项目清单已审核，不能新增、修改、删除或覆盖导入明细，请联系管理员处理");
            }
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String extractLeadingToken(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        int firstSpace = -1;
        for (int index = 0; index < trimmed.length(); index++) {
            if (Character.isWhitespace(trimmed.charAt(index)) || trimmed.charAt(index) == '　') {
                firstSpace = index;
                break;
            }
        }
        return firstSpace >= 0 ? trimmed.substring(0, firstSpace) : trimmed;
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
            String normalizedMaterialCode = extractLeadingToken(item.getMaterialCode()).toUpperCase(Locale.ROOT);
            item.setMaterialCode(normalizedMaterialCode);
            if (normalizedMaterialCode.length() < 10 || normalizedMaterialCode.length() > 12) {
                throw new IllegalArgumentException("Material code must be 10 to 12 characters");
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

    private int findHeaderRowIndex(Sheet sheet, FormulaEvaluator evaluator) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(row, evaluator);
            if (hasItemReferenceColumn(headerIndexMap) && headerIndexMap.containsKey("quantity")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow, FormulaEvaluator evaluator) {
        Map<String, Integer> headerIndexMap = new HashMap<>();
        if (headerRow == null) {
            return headerIndexMap;
        }

        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(formatter.formatCellValue(cell, evaluator));
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
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .replace("　", "");
        return HEADER_ALIASES.getOrDefault(normalized, normalized);
    }

    private boolean hasItemReferenceColumn(Map<String, Integer> headerIndexMap) {
        return ITEM_REFERENCE_HEADERS.stream().anyMatch(headerIndexMap::containsKey);
    }

    private Map<String, String> extractRowData(Row row, Map<String, Integer> headerIndexMap, FormulaEvaluator evaluator) {
        Map<String, String> rowData = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        headerIndexMap.forEach((key, columnIndex) -> {
            String value = row != null && row.getCell(columnIndex) != null
                    ? formatter.formatCellValue(row.getCell(columnIndex), evaluator).trim()
                    : "";
            rowData.put(key, value);
        });
        return rowData;
    }

    private boolean isEmptyDataRow(Row row, Map<String, Integer> headerIndexMap, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }

        DataFormatter formatter = new DataFormatter();
        for (Integer columnIndex : headerIndexMap.values()) {
            if (columnIndex == null) {
                continue;
            }
            String value = formatter.formatCellValue(row.getCell(columnIndex), evaluator).trim();
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
            int excelRowNumber,
            FormulaEvaluator evaluator
    ) {
        DataFormatter formatter = new DataFormatter();
        ProjectListItem item = new ProjectListItem();
        item.setProjectListId(projectListId);
        item.setMaterialId(getCellValue(row, headerIndexMap, "materialid", formatter, evaluator));
        item.setMaterialCode(getCellValue(row, headerIndexMap, "materialcode", formatter, evaluator));
        item.setItemName(getCellValue(row, headerIndexMap, "itemname", formatter, evaluator));
        item.setModel(getCellValue(row, headerIndexMap, "model", formatter, evaluator));
        item.setBrand(getCellValue(row, headerIndexMap, "brand", formatter, evaluator));
        item.setUnit(getCellValue(row, headerIndexMap, "unit", formatter, evaluator));
        item.setSourceType(getCellValue(row, headerIndexMap, "sourcetype", formatter, evaluator));
        item.setRemark(getCellValue(row, headerIndexMap, "remark", formatter, evaluator));
        item.setQuantity(parseDecimal(
                getCellValue(row, headerIndexMap, "quantity", formatter, evaluator),
                "Quantity",
                excelRowNumber,
                true
        ));
        item.setUnitPrice(parseDecimal(
                getCellValue(row, headerIndexMap, "unitprice", formatter, evaluator),
                "Unit Price",
                excelRowNumber,
                false
        ));
        return item;
    }

    private String getCellValue(Row row, Map<String, Integer> headerIndexMap, String key, DataFormatter formatter, FormulaEvaluator evaluator) {
        Integer columnIndex = headerIndexMap.get(key);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell, evaluator);
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
