package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBandMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProductCategoryMapper;
import com.jerry.salesmanagement.mapper.ProductSubcategoryMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBand;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.pojo.ProductCategory;
import com.jerry.salesmanagement.pojo.ProductSubcategory;
import com.jerry.salesmanagement.pojo.dto.ExcelImportError;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.MaterialMasterService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final Pattern ALPHANUMERIC_CODE_PATTERN = Pattern.compile("^[A-Z0-9]+$");
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

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Override
    public List<MaterialMaster> getAll() {
        return materialMasterMapper.selectAll();
    }

    @Override
    public PageResult<MaterialMaster> getPage(
            String materialCode,
            String productName,
            String productModel,
            Long brandId,
            Boolean isActive,
            int page,
            int pageSize
    ) {
        return PageResult.paginate(
                page,
                pageSize,
                () -> materialMasterMapper.countPage(materialCode, productName, productModel, brandId, isActive),
                (offset, limit) -> materialMasterMapper.selectPage(
                        materialCode, productName, productModel, brandId, isActive, offset, limit)
        );
    }

    @Override
    public MaterialMaster getByUuid(String uuid) {
        return materialMasterMapper.selectByUuid(uuid);
    }

    @Override
    @Transactional
    public MaterialMaster create(MaterialMaster materialMaster) {
        validateMaterial(materialMaster, null);
        if (!StringUtils.hasText(materialMaster.getUuid())) {
            materialMaster.setUuid(UUID.randomUUID().toString());
        }
        if (currentUserService != null) {
            materialMaster.setCreatedBy(currentUserService.requireCurrentUserId());
        }
        fillGeneratedFieldsForCreate(materialMaster);
        materialMasterMapper.insert(materialMaster);
        return materialMaster;
    }

    @Override
    @Transactional
    public MaterialMaster update(MaterialMaster materialMaster) {
        MaterialMaster existing = materialMasterMapper.selectByUuid(materialMaster.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        validateMaterial(materialMaster, existing);
        if (currentUserService != null) {
            materialMaster.setUpdatedBy(currentUserService.requireCurrentUserId());
        }
        preserveGeneratedFields(existing, materialMaster);
        materialMasterMapper.updateByUuid(materialMaster);
        MaterialMaster updated = materialMasterMapper.selectByUuid(materialMaster.getUuid());
        return updated != null ? updated : materialMaster;
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        MaterialMaster existing = materialMasterMapper.selectByUuid(uuid);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "物料",
                    new ReferenceCheckMapper.ReferenceUsage("项目清单明细", referenceCheckMapper.countProjectListItemsByMaterialUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("出入库单明细", referenceCheckMapper.countWarehouseDocItemsByMaterialUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("库存流水", referenceCheckMapper.countInventoryTransactionsByMaterialUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("物料历史价格", referenceCheckMapper.countProductHistoryPricesByMaterialUuid(uuid))
            );
        }
        if (!StringUtils.hasText(existing.getCategoryCode()) || !StringUtils.hasText(existing.getSubcategoryCode())
                || !StringUtils.hasText(existing.getBrandCode()) || !StringUtils.hasText(existing.getBandCode())
                || existing.getSequenceNo() == null || !existing.getSequenceNo().matches("[0-9]{1,9}")) {
            materialMasterMapper.deleteByUuid(uuid);
            return;
        }
        // Use the codes stored on the material, not dictionary values which may have since changed.
        ProductCategory category = new ProductCategory();
        category.setCode(existing.getCategoryCode());
        ProductSubcategory subcategory = new ProductSubcategory();
        subcategory.setCode(existing.getSubcategoryCode());
        ProductBrand brand = new ProductBrand();
        brand.setCode(existing.getBrandCode());
        ProductBand band = new ProductBand();
        band.setCode(existing.getBandCode());
        String scope = resolveSequenceScope();
        int sequenceLength = existing.getSequenceNo().length();
        String prefix = resolveLookupPrefix(scope, category, subcategory, brand, band);
        String bandCode = "CATEGORY_SUBCATEGORY_BRAND_BAND".equals(scope) ? existing.getBandCode() : null;
        codeSequenceService.deleteAndReclaimTail("material:" + scope + ":" + sequenceLength + ":" + prefix,
                () -> materialMasterMapper.deleteByUuid(uuid),
                () -> materialMasterMapper.selectMaxMaterialSequenceForUpdate(existing.getCategoryCode(),
                        existing.getSubcategoryCode(), existing.getBrandCode(), bandCode, sequenceLength));
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

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            int headerRowIndex = findHeaderRowIndex(sheet, formatter, evaluator);
            if (headerRowIndex < 0) {
                throw new IllegalArgumentException("Header row was not found");
            }

            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(sheet.getRow(headerRowIndex), formatter, evaluator);
            if (!headerIndexMap.containsKey("name") || !headerIndexMap.containsKey("categorycode")
                    || !headerIndexMap.containsKey("subcategorycode") || !headerIndexMap.containsKey("brandcode")
                    || !headerIndexMap.containsKey("bandcode") || !headerIndexMap.containsKey("unit")) {
                throw new IllegalArgumentException("Excel must contain Name, Category Code, Subcategory Code, Brand Code, Band Code, and Unit columns");
            }

            DictionaryCache cache = loadDictionaryCache();
            Set<String> seenMaterialKeys = new HashSet<>();
            for (MaterialMaster existing : materialMasterMapper.selectAll()) {
                seenMaterialKeys.add(materialDuplicateKey(existing));
            }

            int totalRows = 0;
            int skippedRows = 0;
            int importedRows = 0;
            List<ValidImportMaterial> validMaterials = new ArrayList<>();
            List<ExcelImportError> errors = new ArrayList<>();

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyDataRow(row, headerIndexMap, formatter, evaluator)) {
                    skippedRows++;
                    continue;
                }

                totalRows++;
                int excelRowNumber = rowIndex + 1;
                Map<String, String> rowData = extractRowData(row, headerIndexMap, formatter, evaluator);
                try {
                    MaterialMaster materialMaster = mapRowToMaterial(row, headerIndexMap, excelRowNumber, cache, formatter, evaluator);
                    validateMaterial(materialMaster, null, cache);
                    String dupKey = materialDuplicateKey(materialMaster);
                    if (!seenMaterialKeys.add(dupKey)) {
                        throw new IllegalArgumentException("Row " + excelRowNumber + ": 物料（分类、分项、品牌、频段、名称、型号）已存在或文件内重复");
                    }
                    String sortKey = buildSortKey(materialMaster, cache);
                    validMaterials.add(new ValidImportMaterial(excelRowNumber, rowData, materialMaster, sortKey));
                } catch (Exception e) {
                    errors.add(buildImportError(excelRowNumber, e.getMessage(), rowData));
                }
            }

            if (IMPORT_MODE_FAIL_FAST.equals(normalizedImportMode) && !errors.isEmpty()) {
                return buildImportResult(normalizedImportMode, totalRows, 0, skippedRows, errors);
            }

            validMaterials.sort(Comparator.comparing(ValidImportMaterial::sortKey)
                    .thenComparingInt(ValidImportMaterial::excelRowNumber));

            if (IMPORT_MODE_FAIL_FAST.equals(normalizedImportMode)) {
                importedRows = executeInSingleTransaction(() -> {
                    int count = 0;
                    for (ValidImportMaterial entry : validMaterials) {
                        persistCreatedMaterial(entry.material(), cache);
                        count++;
                    }
                    return count;
                });
            } else {
                for (ValidImportMaterial entry : validMaterials) {
                    try {
                        executeRowTransaction(() -> {
                            persistCreatedMaterial(entry.material(), cache);
                            return null;
                        });
                        importedRows++;
                    } catch (Exception e) {
                        errors.add(buildImportError(entry.excelRowNumber(), e.getMessage(), entry.rowData()));
                    }
                }
            }

            return buildImportResult(normalizedImportMode, totalRows, importedRows, skippedRows, errors);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Excel file: " + e.getMessage(), e);
        }
    }

    private void validateMaterial(MaterialMaster materialMaster, MaterialMaster existing) {
        validateMaterial(materialMaster, existing, null);
    }

    private void validateMaterial(MaterialMaster materialMaster, MaterialMaster existing, DictionaryCache cache) {
        ProductCategory category = resolveCategory(materialMaster.getCategoryId(), cache);
        ProductSubcategory subcategory = resolveSubcategory(materialMaster.getSubcategoryId(), cache);
        ProductBrand brand = resolveBrand(materialMaster.getBrandId(), cache);
        ProductBand band = resolveBand(materialMaster.getFrequency(), cache);

        if (existing == null) {
            if (!Boolean.TRUE.equals(category.getIsActive())) {
                throw new IllegalArgumentException("Category is disabled");
            }
            if (!Boolean.TRUE.equals(subcategory.getIsActive())) {
                throw new IllegalArgumentException("Subcategory is disabled");
            }
            if (!Boolean.TRUE.equals(brand.getIsActive())) {
                throw new IllegalArgumentException("Brand is disabled");
            }
            if (!Boolean.TRUE.equals(band.getIsActive())) {
                throw new IllegalArgumentException("Band is disabled");
            }
        } else {
            String normalizedFreq = trimToNull(materialMaster.getFrequency());
            String existingFreq = trimToNull(existing.getFrequency());
            boolean freqChanged = normalizedFreq == null ? existingFreq != null : !normalizedFreq.equalsIgnoreCase(existingFreq);
            if (!Objects.equals(existing.getCategoryId(), materialMaster.getCategoryId())
                    || !Objects.equals(existing.getSubcategoryId(), materialMaster.getSubcategoryId())
                    || !Objects.equals(existing.getBrandId(), materialMaster.getBrandId())
                    || freqChanged) {
                throw new IllegalArgumentException("物料分类与频段已固化在物料编码中，创建后不可修改");
            }
        }

        if (!StringUtils.hasText(materialMaster.getProductName())) {
            throw new IllegalArgumentException("Material name is required");
        }
        materialMaster.setProductName(materialMaster.getProductName().trim());
        if (!StringUtils.hasText(materialMaster.getUnit())) {
            throw new IllegalArgumentException("Material unit is required");
        }
        materialMaster.setUnit(materialMaster.getUnit().trim());
        if (materialMaster.getIsActive() == null) {
            throw new IllegalArgumentException("Material active flag is required");
        }
        materialMaster.setFrequency(band.getCode().trim().toUpperCase(Locale.ROOT));
        materialMaster.setProductModel(trimToNull(materialMaster.getProductModel()));
        materialMaster.setManufacturer(trimToNull(materialMaster.getManufacturer()));
        materialMaster.setSpecification(trimToNull(materialMaster.getSpecification()));
        materialMaster.setOtherNote(trimToNull(materialMaster.getOtherNote()));

        if (StringUtils.hasText(materialMaster.getMaterialCode())
                && !matchesConfiguredPattern(materialMaster.getMaterialCode(), category, subcategory, brand, band)) {
            throw new IllegalArgumentException("Material code format is invalid");
        }

        if (!isValidDictionaryCode(category.getCode(), 2, 2)) {
            throw new IllegalArgumentException("Category code is invalid");
        }
        if (!isValidDictionaryCode(subcategory.getCode(), 2, 2)) {
            throw new IllegalArgumentException("Subcategory code is invalid");
        }
        if (!isValidDictionaryCode(brand.getCode(), 2, 2)) {
            throw new IllegalArgumentException("Brand code is invalid");
        }
        if (!isValidDictionaryCode(band.getCode(), 1, 3)) {
            throw new IllegalArgumentException("Band code is invalid");
        }
    }

    private void fillGeneratedFieldsForCreate(MaterialMaster materialMaster) {
        fillGeneratedFieldsForCreate(materialMaster, null);
    }

    private void fillGeneratedFieldsForCreate(MaterialMaster materialMaster, DictionaryCache cache) {
        ProductCategory category = resolveCategory(materialMaster.getCategoryId(), cache);
        ProductSubcategory subcategory = resolveSubcategory(materialMaster.getSubcategoryId(), cache);
        ProductBrand brand = resolveBrand(materialMaster.getBrandId(), cache);
        ProductBand band = resolveBand(materialMaster.getFrequency(), cache);
        String pattern = resolveMaterialCodePattern();
        int sequenceLength = resolveSequenceLength();
        String sequenceScope = resolveSequenceScope();
        String lookupPrefix = resolveLookupPrefix(sequenceScope, category, subcategory, brand, band);
        String bandCode = "CATEGORY_SUBCATEGORY_BRAND_BAND".equals(sequenceScope) ? band.getCode() : null;
        int maxSequence = (int) Math.pow(10, sequenceLength) - 1;
        int nextSequence = codeSequenceService.nextValue(
                "material:" + sequenceScope + ":" + sequenceLength + ":" + lookupPrefix,
                () -> materialMasterMapper.selectMaxMaterialSequenceForUpdate(
                        category.getCode(), subcategory.getCode(), brand.getCode(), bandCode, sequenceLength),
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

    private int findHeaderRowIndex(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(row, formatter, evaluator);
            if (headerIndexMap.containsKey("name") && headerIndexMap.containsKey("categorycode")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
        if (headerRow == null) {
            return headerIndexMap;
        }

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

    private boolean isEmptyDataRow(Row row, Map<String, Integer> headerIndexMap, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        for (Integer columnIndex : headerIndexMap.values()) {
            if (columnIndex == null) {
                continue;
            }
            Cell cell = row.getCell(columnIndex);
            if (cell == null) {
                continue;
            }
            String value = formatter.formatCellValue(cell, evaluator).trim();
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> extractRowData(Row row, Map<String, Integer> headerIndexMap, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, String> rowData = new LinkedHashMap<>();
        headerIndexMap.forEach((key, columnIndex) -> {
            String value = row != null && row.getCell(columnIndex) != null
                    ? formatter.formatCellValue(row.getCell(columnIndex), evaluator).trim()
                    : "";
            rowData.put(key, value);
        });
        return rowData;
    }

    private MaterialMaster mapRowToMaterial(Row row, Map<String, Integer> headerIndexMap, int excelRowNumber, DictionaryCache cache, DataFormatter formatter, FormulaEvaluator evaluator) {
        MaterialMaster materialMaster = new MaterialMaster();
        materialMaster.setProductName(requiredCell(row, headerIndexMap, "name", "Name", excelRowNumber, formatter, evaluator));
        materialMaster.setCategoryId(findCategoryId(extractLeadingToken(requiredCell(row, headerIndexMap, "categorycode", "Category Code", excelRowNumber, formatter, evaluator)), excelRowNumber, cache));
        materialMaster.setSubcategoryId(findSubcategoryId(extractLeadingToken(requiredCell(row, headerIndexMap, "subcategorycode", "Subcategory Code", excelRowNumber, formatter, evaluator)), excelRowNumber, cache));
        materialMaster.setBrandId(findBrandId(extractLeadingToken(requiredCell(row, headerIndexMap, "brandcode", "Brand Code", excelRowNumber, formatter, evaluator)), excelRowNumber, cache));
        materialMaster.setFrequency(extractLeadingToken(requiredCell(row, headerIndexMap, "bandcode", "Band Code", excelRowNumber, formatter, evaluator)).toUpperCase(Locale.ROOT));
        materialMaster.setUnit(requiredCell(row, headerIndexMap, "unit", "Unit", excelRowNumber, formatter, evaluator));
        materialMaster.setProductModel(optionalCell(row, headerIndexMap, "model", formatter, evaluator));
        materialMaster.setSpecification(optionalCell(row, headerIndexMap, "specification", formatter, evaluator));
        materialMaster.setOtherNote(optionalCell(row, headerIndexMap, "othernote", formatter, evaluator));
        materialMaster.setManufacturer(optionalCell(row, headerIndexMap, "manufacturer", formatter, evaluator));
        String activeValue = optionalCell(row, headerIndexMap, "isactive", formatter, evaluator);
        materialMaster.setIsActive(!StringUtils.hasText(activeValue) || parseBoolean(activeValue, excelRowNumber));
        return materialMaster;
    }

    private String requiredCell(Row row, Map<String, Integer> headerIndexMap, String key, String fieldName, int excelRowNumber, DataFormatter formatter, FormulaEvaluator evaluator) {
        String value = optionalCell(row, headerIndexMap, key, formatter, evaluator);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Row " + excelRowNumber + ": " + fieldName + " is required");
        }
        return value.trim();
    }

    private String optionalCell(Row row, Map<String, Integer> headerIndexMap, String key, DataFormatter formatter, FormulaEvaluator evaluator) {
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

    private boolean isValidDictionaryCode(String code, int minLength, int maxLength) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        String trimmed = code.trim().toUpperCase(Locale.ROOT);
        return trimmed.length() >= minLength
                && trimmed.length() <= maxLength
                && ALPHANUMERIC_CODE_PATTERN.matcher(trimmed).matches();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void persistCreatedMaterial(MaterialMaster materialMaster, DictionaryCache cache) {
        if (!StringUtils.hasText(materialMaster.getUuid())) {
            materialMaster.setUuid(UUID.randomUUID().toString());
        }
        if (currentUserService != null) {
            materialMaster.setCreatedBy(currentUserService.requireCurrentUserId());
        }
        fillGeneratedFieldsForCreate(materialMaster, cache);
        materialMasterMapper.insert(materialMaster);
    }

    private int executeInSingleTransaction(java.util.function.IntSupplier action) {
        if (transactionManager == null) {
            return action.getAsInt();
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        Integer result = template.execute(status -> action.getAsInt());
        return result == null ? 0 : result;
    }

    private void executeRowTransaction(java.util.function.Supplier<Void> action) {
        if (transactionManager == null) {
            action.get();
            return;
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.execute(status -> action.get());
    }

    private DictionaryCache loadDictionaryCache() {
        Map<Long, ProductCategory> categoryById = new HashMap<>();
        Map<String, ProductCategory> categoryByCode = new HashMap<>();
        for (ProductCategory c : categoryMapper.selectAll()) {
            if (c != null) {
                if (c.getId() != null) categoryById.put(c.getId(), c);
                if (StringUtils.hasText(c.getCode())) categoryByCode.put(c.getCode().trim().toUpperCase(Locale.ROOT), c);
            }
        }
        Map<Long, ProductSubcategory> subcategoryById = new HashMap<>();
        Map<String, ProductSubcategory> subcategoryByCode = new HashMap<>();
        for (ProductSubcategory s : subcategoryMapper.selectAll()) {
            if (s != null) {
                if (s.getId() != null) subcategoryById.put(s.getId(), s);
                if (StringUtils.hasText(s.getCode())) subcategoryByCode.put(s.getCode().trim().toUpperCase(Locale.ROOT), s);
            }
        }
        Map<Long, ProductBrand> brandById = new HashMap<>();
        Map<String, ProductBrand> brandByCode = new HashMap<>();
        for (ProductBrand b : brandMapper.selectAll()) {
            if (b != null) {
                if (b.getId() != null) brandById.put(b.getId(), b);
                if (StringUtils.hasText(b.getCode())) brandByCode.put(b.getCode().trim().toUpperCase(Locale.ROOT), b);
            }
        }
        Map<String, ProductBand> bandByCode = new HashMap<>();
        for (ProductBand band : bandMapper.selectAll()) {
            if (band != null && StringUtils.hasText(band.getCode())) {
                bandByCode.put(band.getCode().trim().toUpperCase(Locale.ROOT), band);
            }
        }
        return new DictionaryCache(categoryById, categoryByCode, subcategoryById, subcategoryByCode, brandById, brandByCode, bandByCode);
    }

    private ProductCategory resolveCategory(Long id, DictionaryCache cache) {
        if (cache != null && id != null) {
            ProductCategory cached = cache.categoryById().get(id);
            if (cached != null) return cached;
        }
        return getCategory(id);
    }

    private ProductSubcategory resolveSubcategory(Long id, DictionaryCache cache) {
        if (cache != null && id != null) {
            ProductSubcategory cached = cache.subcategoryById().get(id);
            if (cached != null) return cached;
        }
        return getSubcategory(id);
    }

    private ProductBrand resolveBrand(Long id, DictionaryCache cache) {
        if (cache != null && id != null) {
            ProductBrand cached = cache.brandById().get(id);
            if (cached != null) return cached;
        }
        return getBrand(id);
    }

    private ProductBand resolveBand(String bandCode, DictionaryCache cache) {
        if (cache != null && StringUtils.hasText(bandCode)) {
            ProductBand cached = cache.bandByCode().get(bandCode.trim().toUpperCase(Locale.ROOT));
            if (cached != null) return cached;
        }
        return getBand(bandCode);
    }

    private String materialDuplicateKey(MaterialMaster item) {
        String freq = item.getFrequency() != null ? item.getFrequency().trim().toUpperCase(Locale.ROOT)
                : (item.getBandCode() != null ? item.getBandCode().trim().toUpperCase(Locale.ROOT) : "");
        String name = item.getProductName() == null ? "" : item.getProductName().trim().toLowerCase(Locale.ROOT);
        String model = item.getProductModel() == null ? "" : item.getProductModel().trim().toLowerCase(Locale.ROOT);
        return item.getCategoryId() + "|" + item.getSubcategoryId() + "|" + item.getBrandId()
                + "|" + freq + "|" + name + "|" + model;
    }

    private String buildSortKey(MaterialMaster materialMaster, DictionaryCache cache) {
        String c = resolveCategory(materialMaster.getCategoryId(), cache).getCode();
        String s = resolveSubcategory(materialMaster.getSubcategoryId(), cache).getCode();
        String b = resolveBrand(materialMaster.getBrandId(), cache).getCode();
        String f = materialMaster.getFrequency() == null ? "" : materialMaster.getFrequency();
        return c + "|" + s + "|" + b + "|" + f;
    }

    private record ValidImportMaterial(
            int excelRowNumber,
            Map<String, String> rowData,
            MaterialMaster material,
            String sortKey
    ) {
    }

    private record DictionaryCache(
            Map<Long, ProductCategory> categoryById,
            Map<String, ProductCategory> categoryByCode,
            Map<Long, ProductSubcategory> subcategoryById,
            Map<String, ProductSubcategory> subcategoryByCode,
            Map<Long, ProductBrand> brandById,
            Map<String, ProductBrand> brandByCode,
            Map<String, ProductBand> bandByCode
    ) {
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

    private Long findCategoryId(String code, int excelRowNumber, DictionaryCache cache) {
        ProductCategory matched = cache.categoryByCode().get(code.toUpperCase(Locale.ROOT));
        if (matched == null) {
            throw new IllegalArgumentException("Row " + excelRowNumber + ": category code does not exist");
        }
        return matched.getId();
    }

    private Long findSubcategoryId(String code, int excelRowNumber, DictionaryCache cache) {
        ProductSubcategory matched = cache.subcategoryByCode().get(code.toUpperCase(Locale.ROOT));
        if (matched == null) {
            throw new IllegalArgumentException("Row " + excelRowNumber + ": subcategory code does not exist");
        }
        return matched.getId();
    }

    private Long findBrandId(String code, int excelRowNumber, DictionaryCache cache) {
        ProductBrand matched = cache.brandByCode().get(code.toUpperCase(Locale.ROOT));
        if (matched == null) {
            throw new IllegalArgumentException("Row " + excelRowNumber + ": brand code does not exist");
        }
        return matched.getId();
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
