package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBandMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProductCategoryMapper;
import com.jerry.salesmanagement.mapper.ProductSubcategoryMapper;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBand;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.pojo.ProductCategory;
import com.jerry.salesmanagement.pojo.ProductSubcategory;
import com.jerry.salesmanagement.pojo.dto.ExcelImportError;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.MaterialMasterService;
import com.jerry.salesmanagement.service.SystemConfigService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MaterialMasterServiceImpl implements MaterialMasterService {

    private static final String IMPORT_MODE_FAIL_FAST = "FAIL_FAST";
    private static final String IMPORT_MODE_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final String CONFIG_PATTERN = "material.code.pattern";
    private static final String CONFIG_SEQUENCE_LENGTH = "material.code.sequence.length";
    private static final String CONFIG_SEQUENCE_SCOPE = "material.code.sequence.scope";
    private static final String DEFAULT_PATTERN = "{category}{subcategory}{brand}{sequence}{band}";
    private static final int DEFAULT_SEQUENCE_LENGTH = 3;
    private static final String DEFAULT_SEQUENCE_SCOPE = "CATEGORY_SUBCATEGORY_BRAND";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{(category|subcategory|brand|sequence|band)\\}");
    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("名称", "name"),
            Map.entry("物料名称", "name"),
            Map.entry("商品名称", "name"),
            Map.entry("大类编码", "categorycode"),
            Map.entry("类别编码", "categorycode"),
            Map.entry("分类编码", "categorycode"),
            Map.entry("分项编码", "subcategorycode"),
            Map.entry("子类编码", "subcategorycode"),
            Map.entry("子分类编码", "subcategorycode"),
            Map.entry("品牌编码", "brandcode"),
            Map.entry("频段编码", "bandcode"),
            Map.entry("频率编码", "bandcode"),
            Map.entry("单位", "unit"),
            Map.entry("型号", "model"),
            Map.entry("规格参数", "specification"),
            Map.entry("规格", "specification"),
            Map.entry("厂家", "manufacturer"),
            Map.entry("生产厂家", "manufacturer"),
            Map.entry("其他说明", "othernote"),
            Map.entry("备注", "othernote"),
            Map.entry("是否启用", "isactive"),
            Map.entry("启用", "isactive")
    );

    @Autowired
    private MaterialMasterMapper materialMasterMapper;

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Autowired
    private ProductSubcategoryMapper subcategoryMapper;

    @Autowired
    private ProductBrandMapper brandMapper;

    @Autowired
    private ProductBandMapper bandMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private CodeSequenceService codeSequenceService;

    @Override
    public List<MaterialMaster> getAll() {
        return materialMasterMapper.selectAll();
    }

    @Override
    public MaterialMaster getByUuid(String uuid) {
        return materialMasterMapper.selectByUuid(uuid);
    }

    @Override
    @Transactional
    public MaterialMaster create(MaterialMaster materialMaster) {
        validateMaterial(materialMaster, false);
        if (!StringUtils.hasText(materialMaster.getUuid())) {
            materialMaster.setUuid(UUID.randomUUID().toString());
        }
        fillGeneratedFieldsForCreate(materialMaster);
        materialMasterMapper.insert(materialMaster);
        return materialMaster;
    }

    @Override
    public MaterialMaster update(MaterialMaster materialMaster) {
        MaterialMaster existing = materialMasterMapper.selectByUuid(materialMaster.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        validateMaterial(materialMaster, true);
        preserveGeneratedFields(existing, materialMaster);
        materialMasterMapper.updateByUuid(materialMaster);
        return materialMaster;
    }

    @Override
    public void delete(String uuid) {
        materialMasterMapper.deleteByUuid(uuid);
    }

    @Override
    public ExcelImportResult importFromExcel(MultipartFile file, String importMode) {
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
            if (!headerIndexMap.containsKey("name") || !headerIndexMap.containsKey("categorycode")
                    || !headerIndexMap.containsKey("subcategorycode") || !headerIndexMap.containsKey("brandcode")
                    || !headerIndexMap.containsKey("bandcode") || !headerIndexMap.containsKey("unit")) {
                throw new IllegalArgumentException("Excel must contain Name, Category Code, Subcategory Code, Brand Code, Band Code, and Unit columns");
            }

            int totalRows = 0;
            int skippedRows = 0;
            int importedRows = 0;
            List<MaterialMaster> validMaterials = new ArrayList<>();
            List<ExcelImportError> errors = new ArrayList<>();

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyDataRow(row, headerIndexMap)) {
                    skippedRows++;
                    continue;
                }

                totalRows++;
                Map<String, String> rowData = extractRowData(row, headerIndexMap);
                try {
                    MaterialMaster materialMaster = mapRowToMaterial(row, headerIndexMap, rowIndex + 1);
                    validateMaterial(materialMaster, false);
                    validMaterials.add(materialMaster);
                } catch (Exception e) {
                    errors.add(buildImportError(rowIndex + 1, e.getMessage(), rowData));
                }
            }

            if (IMPORT_MODE_FAIL_FAST.equals(normalizedImportMode) && !errors.isEmpty()) {
                return buildImportResult(normalizedImportMode, totalRows, 0, skippedRows, errors);
            }

            for (MaterialMaster materialMaster : validMaterials) {
                create(materialMaster);
                importedRows++;
            }

            return buildImportResult(normalizedImportMode, totalRows, importedRows, skippedRows, errors);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Excel file: " + e.getMessage(), e);
        }
    }

    private void validateMaterial(MaterialMaster materialMaster, boolean updating) {
        ProductCategory category = getCategory(materialMaster.getCategoryId());
        ProductSubcategory subcategory = getSubcategory(materialMaster.getSubcategoryId());
        ProductBrand brand = getBrand(materialMaster.getBrandId());
        ProductBand band = getBand(materialMaster.getFrequency());

        if (!StringUtils.hasText(materialMaster.getProductName())) {
            throw new IllegalArgumentException("Material name is required");
        }
        if (!StringUtils.hasText(materialMaster.getUnit())) {
            throw new IllegalArgumentException("Material unit is required");
        }
        if (materialMaster.getIsActive() == null) {
            throw new IllegalArgumentException("Material active flag is required");
        }

        if (StringUtils.hasText(materialMaster.getMaterialCode())
                && !matchesConfiguredPattern(materialMaster.getMaterialCode(), category, subcategory, brand, band)) {
            throw new IllegalArgumentException("Material code format is invalid");
        }

        if (!StringUtils.hasText(category.getCode()) || category.getCode().length() != 2) {
            throw new IllegalArgumentException("Category code is invalid");
        }
        if (!StringUtils.hasText(subcategory.getCode()) || subcategory.getCode().length() != 2) {
            throw new IllegalArgumentException("Subcategory code is invalid");
        }
        if (!StringUtils.hasText(brand.getCode()) || brand.getCode().length() != 2) {
            throw new IllegalArgumentException("Brand code is invalid");
        }
        if (!StringUtils.hasText(band.getCode()) || band.getCode().length() > 3) {
            throw new IllegalArgumentException("Band code is invalid");
        }
    }

    private void fillGeneratedFieldsForCreate(MaterialMaster materialMaster) {
        ProductCategory category = getCategory(materialMaster.getCategoryId());
        ProductSubcategory subcategory = getSubcategory(materialMaster.getSubcategoryId());
        ProductBrand brand = getBrand(materialMaster.getBrandId());
        ProductBand band = getBand(materialMaster.getFrequency());
        String pattern = resolveMaterialCodePattern();
        int sequenceLength = resolveSequenceLength();
        String sequenceScope = resolveSequenceScope();
        String lookupPrefix = resolveLookupPrefix(sequenceScope, category, subcategory, brand, band);
        String currentMax = materialMasterMapper.selectMaxMaterialCodeByPrefix(lookupPrefix);
        int sequenceStart = resolveTokenStart(pattern, "sequence", category, subcategory, brand, band, sequenceLength);
        String currentSequence = currentMax != null && currentMax.length() >= sequenceStart + sequenceLength
                ? currentMax.substring(sequenceStart, sequenceStart + sequenceLength)
                : null;
        int currentSequenceValue = currentSequence == null ? 0 : Integer.parseInt(currentSequence);
        int maxSequence = (int) Math.pow(10, sequenceLength) - 1;
        int nextSequence = codeSequenceService.nextValue(
                "material:" + sequenceScope + ":" + sequenceLength + ":" + lookupPrefix,
                currentSequenceValue,
                maxSequence,
                "Material code sequence for " + lookupPrefix
        );
        String renderedSequence = String.format("%0" + sequenceLength + "d", nextSequence);

        materialMaster.setCategoryCode(category.getCode());
        materialMaster.setSubcategoryCode(subcategory.getCode());
        materialMaster.setBrandCode(brand.getCode());
        materialMaster.setBandCode(band.getCode());
        materialMaster.setSequenceNo(renderedSequence);
        materialMaster.setMaterialCode(renderMaterialCode(pattern, category, subcategory, brand, band, renderedSequence));
        materialMaster.setIsActive(Boolean.TRUE.equals(materialMaster.getIsActive()));
    }

    private void preserveGeneratedFields(MaterialMaster existing, MaterialMaster materialMaster) {
        materialMaster.setCategoryCode(existing.getCategoryCode());
        materialMaster.setSubcategoryCode(existing.getSubcategoryCode());
        materialMaster.setBrandCode(existing.getBrandCode());
        materialMaster.setBandCode(existing.getBandCode());
        materialMaster.setSequenceNo(existing.getSequenceNo());
        materialMaster.setMaterialCode(existing.getMaterialCode());
        if (materialMaster.getIsActive() == null) {
            materialMaster.setIsActive(existing.getIsActive());
        }
    }

    private ProductCategory getCategory(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Category is required");
        }
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("Category does not exist");
        }
        return category;
    }

    private ProductSubcategory getSubcategory(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Subcategory is required");
        }
        ProductSubcategory subcategory = subcategoryMapper.selectById(id);
        if (subcategory == null) {
            throw new IllegalArgumentException("Subcategory does not exist");
        }
        return subcategory;
    }

    private ProductBrand getBrand(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Brand is required");
        }
        ProductBrand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new IllegalArgumentException("Brand does not exist");
        }
        return brand;
    }

    private ProductBand getBand(String bandCode) {
        if (!StringUtils.hasText(bandCode)) {
            throw new IllegalArgumentException("Band is required");
        }
        return bandMapper.selectAll().stream()
                .filter(item -> bandCode.equals(item.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Band does not exist"));
    }

    private int findHeaderRowIndex(Sheet sheet) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(row);
            if (headerIndexMap.containsKey("name") && headerIndexMap.containsKey("categorycode")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow) {
        Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
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
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .replace("　", "");
        return HEADER_ALIASES.getOrDefault(normalized, normalized);
    }

    private boolean isEmptyDataRow(Row row, Map<String, Integer> headerIndexMap) {
        if (row == null) {
            return true;
        }
        DataFormatter formatter = new DataFormatter();
        for (String key : new String[]{"name", "categorycode", "subcategorycode", "brandcode", "bandcode", "unit"}) {
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

    private MaterialMaster mapRowToMaterial(Row row, Map<String, Integer> headerIndexMap, int excelRowNumber) {
        MaterialMaster materialMaster = new MaterialMaster();
        materialMaster.setProductName(requiredCell(row, headerIndexMap, "name", "Name", excelRowNumber));
        materialMaster.setCategoryId(findCategoryId(extractLeadingToken(requiredCell(row, headerIndexMap, "categorycode", "Category Code", excelRowNumber)), excelRowNumber));
        materialMaster.setSubcategoryId(findSubcategoryId(extractLeadingToken(requiredCell(row, headerIndexMap, "subcategorycode", "Subcategory Code", excelRowNumber)), excelRowNumber));
        materialMaster.setBrandId(findBrandId(extractLeadingToken(requiredCell(row, headerIndexMap, "brandcode", "Brand Code", excelRowNumber)), excelRowNumber));
        materialMaster.setFrequency(extractLeadingToken(requiredCell(row, headerIndexMap, "bandcode", "Band Code", excelRowNumber)).toUpperCase(Locale.ROOT));
        materialMaster.setUnit(requiredCell(row, headerIndexMap, "unit", "Unit", excelRowNumber));
        materialMaster.setProductModel(optionalCell(row, headerIndexMap, "model"));
        materialMaster.setSpecification(optionalCell(row, headerIndexMap, "specification"));
        materialMaster.setOtherNote(optionalCell(row, headerIndexMap, "othernote"));
        materialMaster.setManufacturer(optionalCell(row, headerIndexMap, "manufacturer"));
        String activeValue = optionalCell(row, headerIndexMap, "isactive");
        materialMaster.setIsActive(!StringUtils.hasText(activeValue) || parseBoolean(activeValue, excelRowNumber));
        return materialMaster;
    }

    private String requiredCell(Row row, Map<String, Integer> headerIndexMap, String key, String fieldName, int excelRowNumber) {
        String value = optionalCell(row, headerIndexMap, key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Row " + excelRowNumber + ": " + fieldName + " is required");
        }
        return value.trim();
    }

    private String optionalCell(Row row, Map<String, Integer> headerIndexMap, String key) {
        Integer columnIndex = headerIndexMap.get(key);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean parseBoolean(String value, int excelRowNumber) {
        String normalized = extractLeadingToken(value).toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized) || "y".equals(normalized)
                || "启用".equals(normalized) || "是".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized) || "n".equals(normalized)
                || "停用".equals(normalized) || "否".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Row " + excelRowNumber + ": invalid Is Active value");
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

    private Long findCategoryId(String code, int excelRowNumber) {
        return categoryMapper.selectAll().stream()
                .filter(item -> code.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .map(ProductCategory::getId)
                .orElseThrow(() -> new IllegalArgumentException("Row " + excelRowNumber + ": category code does not exist"));
    }

    private Long findSubcategoryId(String code, int excelRowNumber) {
        return subcategoryMapper.selectAll().stream()
                .filter(item -> code.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .map(ProductSubcategory::getId)
                .orElseThrow(() -> new IllegalArgumentException("Row " + excelRowNumber + ": subcategory code does not exist"));
    }

    private Long findBrandId(String code, int excelRowNumber) {
        return brandMapper.selectAll().stream()
                .filter(item -> code.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .map(ProductBrand::getId)
                .orElseThrow(() -> new IllegalArgumentException("Row " + excelRowNumber + ": brand code does not exist"));
    }

    private boolean matchesConfiguredPattern(
            String materialCode,
            ProductCategory category,
            ProductSubcategory subcategory,
            ProductBrand brand,
            ProductBand band
    ) {
        String pattern = resolveMaterialCodePattern();
        int sequenceLength = resolveSequenceLength();
        StringBuilder regex = new StringBuilder();
        Matcher matcher = TOKEN_PATTERN.matcher(pattern);
        int cursor = 0;
        while (matcher.find()) {
            regex.append(Pattern.quote(pattern.substring(cursor, matcher.start())));
            regex.append(tokenRegex(matcher.group(1), category, subcategory, brand, band, sequenceLength));
            cursor = matcher.end();
        }
        regex.append(Pattern.quote(pattern.substring(cursor)));
        return materialCode.matches(regex.toString());
    }

    private String tokenRegex(
            String token,
            ProductCategory category,
            ProductSubcategory subcategory,
            ProductBrand brand,
            ProductBand band,
            int sequenceLength
    ) {
        return switch (token) {
            case "category" -> "[A-Z0-9]{" + category.getCode().length() + "}";
            case "subcategory" -> "[A-Z0-9]{" + subcategory.getCode().length() + "}";
            case "brand" -> "[A-Z0-9]{" + brand.getCode().length() + "}";
            case "band" -> "[A-Z0-9]{" + band.getCode().length() + "}";
            case "sequence" -> "\\d{" + sequenceLength + "}";
            default -> "";
        };
    }

    private String resolveMaterialCodePattern() {
        String configured = systemConfigService.getString(CONFIG_PATTERN, DEFAULT_PATTERN);
        if (!StringUtils.hasText(configured) || !configured.contains("{sequence}")) {
            return DEFAULT_PATTERN;
        }
        return configured.trim();
    }

    private int resolveSequenceLength() {
        int configured = systemConfigService.getInt(CONFIG_SEQUENCE_LENGTH, DEFAULT_SEQUENCE_LENGTH);
        return configured > 0 && configured <= 9 ? configured : DEFAULT_SEQUENCE_LENGTH;
    }

    private String resolveSequenceScope() {
        String configured = systemConfigService.getString(CONFIG_SEQUENCE_SCOPE, DEFAULT_SEQUENCE_SCOPE);
        if ("CATEGORY_SUBCATEGORY_BRAND_BAND".equalsIgnoreCase(configured)) {
            return "CATEGORY_SUBCATEGORY_BRAND_BAND";
        }
        return DEFAULT_SEQUENCE_SCOPE;
    }

    private String resolveLookupPrefix(
            String scope,
            ProductCategory category,
            ProductSubcategory subcategory,
            ProductBrand brand,
            ProductBand band
    ) {
        if ("CATEGORY_SUBCATEGORY_BRAND_BAND".equals(scope)) {
            return renderMaterialCodePrefix(resolveMaterialCodePattern(), category, subcategory, brand, band, true);
        }
        return category.getCode() + subcategory.getCode() + brand.getCode();
    }

    private String renderMaterialCode(
            String pattern,
            ProductCategory category,
            ProductSubcategory subcategory,
            ProductBrand brand,
            ProductBand band,
            String sequence
    ) {
        return pattern
                .replace("{category}", category.getCode())
                .replace("{subcategory}", subcategory.getCode())
                .replace("{brand}", brand.getCode())
                .replace("{sequence}", sequence)
                .replace("{band}", band.getCode());
    }

    private String renderMaterialCodePrefix(
            String pattern,
            ProductCategory category,
            ProductSubcategory subcategory,
            ProductBrand brand,
            ProductBand band,
            boolean includeBand
    ) {
        String placeholder = includeBand ? "{band}" : "";
        return pattern
                .replace("{category}", category.getCode())
                .replace("{subcategory}", subcategory.getCode())
                .replace("{brand}", brand.getCode())
                .replace("{sequence}", "")
                .replace("{band}", placeholder)
                .replace("{band}", band.getCode());
    }

    private int resolveTokenStart(
            String pattern,
            String tokenName,
            ProductCategory category,
            ProductSubcategory subcategory,
            ProductBrand brand,
            ProductBand band,
            int sequenceLength
    ) {
        Matcher matcher = TOKEN_PATTERN.matcher(pattern);
        int cursor = 0;
        int logicalLength = 0;
        while (matcher.find()) {
            logicalLength += matcher.start() - cursor;
            String token = matcher.group(1);
            if (tokenName.equals(token)) {
                return logicalLength;
            }
            logicalLength += tokenLength(token, category, subcategory, brand, band, sequenceLength);
            cursor = matcher.end();
        }
        return logicalLength + pattern.substring(cursor).length();
    }

    private int tokenLength(
            String token,
            ProductCategory category,
            ProductSubcategory subcategory,
            ProductBrand brand,
            ProductBand band,
            int sequenceLength
    ) {
        return switch (token) {
            case "category" -> category.getCode().length();
            case "subcategory" -> subcategory.getCode().length();
            case "brand" -> brand.getCode().length();
            case "band" -> band.getCode().length();
            case "sequence" -> sequenceLength;
            default -> 0;
        };
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
            String importMode,
            int totalRows,
            int importedRows,
            int skippedRows,
            List<ExcelImportError> errors
    ) {
        ExcelImportResult result = new ExcelImportResult();
        result.setResourceType("MATERIAL_MASTER");
        result.setImportMode(importMode);
        result.setTotalRows(totalRows);
        result.setImportedRows(importedRows);
        result.setSkippedRows(skippedRows);
        result.setFailedRows(errors.size());
        result.setErrors(errors);
        return result;
    }
}
