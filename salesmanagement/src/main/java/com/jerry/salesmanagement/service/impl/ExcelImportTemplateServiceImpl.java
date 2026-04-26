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
import com.jerry.salesmanagement.service.ExcelImportTemplateService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
public class ExcelImportTemplateServiceImpl implements ExcelImportTemplateService {

    private static final int FIRST_DATA_ROW_INDEX = 1;
    private static final int LAST_DATA_ROW_INDEX = 500;
    private static final String OPTIONS_SHEET_NAME = "下拉选项";

    private static final String[] MATERIAL_HEADERS = {
            "名称",
            "大类编码",
            "分项编码",
            "品牌编码",
            "频段编码",
            "单位",
            "型号",
            "规格参数",
            "厂家",
            "其他说明",
            "是否启用"
    };

    private static final String[] PROJECT_LIST_ITEM_HEADERS = {
            "物料编码",
            "物料名称",
            "型号",
            "品牌",
            "单位",
            "数量",
            "销售价格",
            "备注"
    };

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Autowired
    private ProductSubcategoryMapper subcategoryMapper;

    @Autowired
    private ProductBrandMapper brandMapper;

    @Autowired
    private ProductBandMapper bandMapper;

    @Autowired
    private MaterialMasterMapper materialMasterMapper;

    @Override
    public byte[] buildMaterialMasterTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet importSheet = workbook.createSheet("物料导入");
            Sheet optionsSheet = workbook.createSheet(OPTIONS_SHEET_NAME);

            writeHeader(importSheet, MATERIAL_HEADERS, createHeaderStyle(workbook));
            String categoryRange = writeOptions(workbook, optionsSheet, 0, "大类编码", activeCategoryOptions());
            String subcategoryRange = writeOptions(workbook, optionsSheet, 1, "分项编码", activeSubcategoryOptions());
            String brandRange = writeOptions(workbook, optionsSheet, 2, "品牌编码", activeBrandOptions());
            String bandRange = writeOptions(workbook, optionsSheet, 3, "频段编码", activeBandOptions());
            String activeRange = writeOptions(workbook, optionsSheet, 4, "是否启用", List.of("TRUE 启用", "FALSE 停用"));

            applyListValidation(importSheet, categoryRange, 1);
            applyListValidation(importSheet, subcategoryRange, 2);
            applyListValidation(importSheet, brandRange, 3);
            applyListValidation(importSheet, bandRange, 4);
            applyListValidation(importSheet, activeRange, 10);

            finishWorkbook(workbook, importSheet, optionsSheet, MATERIAL_HEADERS.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build material import template", e);
        }
    }

    @Override
    public byte[] buildProjectListItemTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet importSheet = workbook.createSheet("项目清单明细导入");
            Sheet optionsSheet = workbook.createSheet(OPTIONS_SHEET_NAME);

            writeHeader(importSheet, PROJECT_LIST_ITEM_HEADERS, createHeaderStyle(workbook));
            String materialRange = writeOptions(workbook, optionsSheet, 0, "物料编码", activeMaterialOptions());

            applyListValidation(importSheet, materialRange, 0);
            finishWorkbook(workbook, importSheet, optionsSheet, PROJECT_LIST_ITEM_HEADERS.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build project list item import template", e);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeHeader(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(headers[columnIndex]);
            cell.setCellStyle(headerStyle);
        }
    }

    private String writeOptions(XSSFWorkbook workbook, Sheet sheet, int columnIndex, String title, List<String> values) {
        Row headerRow = getOrCreateRow(sheet, 0);
        headerRow.createCell(columnIndex).setCellValue(title);

        List<String> resolvedValues = values == null || values.isEmpty() ? List.of("") : values;
        for (int index = 0; index < resolvedValues.size(); index++) {
            getOrCreateRow(sheet, index + 1).createCell(columnIndex).setCellValue(resolvedValues.get(index));
        }

        String rangeName = "template_options_" + columnIndex;
        Name name = workbook.createName();
        name.setNameName(rangeName);
        String columnRef = CellReference.convertNumToColString(columnIndex);
        int lastRowNumber = resolvedValues.size() + 1;
        name.setRefersToFormula("'" + OPTIONS_SHEET_NAME + "'!$" + columnRef + "$2:$" + columnRef + "$" + lastRowNumber);
        return rangeName;
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row : sheet.createRow(rowIndex);
    }

    private void applyListValidation(Sheet sheet, String rangeName, int columnIndex) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(rangeName);
        CellRangeAddressList addressList = new CellRangeAddressList(
                FIRST_DATA_ROW_INDEX,
                LAST_DATA_ROW_INDEX,
                columnIndex,
                columnIndex
        );
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("无效选项", "请从下拉列表中选择一个有效值");
        sheet.addValidationData(validation);
    }

    private void finishWorkbook(XSSFWorkbook workbook, Sheet importSheet, Sheet optionsSheet, int headerCount) {
        importSheet.createFreezePane(0, 1);
        optionsSheet.createFreezePane(0, 1);
        for (int columnIndex = 0; columnIndex < headerCount; columnIndex++) {
            importSheet.setColumnWidth(columnIndex, 18 * 256);
        }
        for (int columnIndex = 0; columnIndex < 8; columnIndex++) {
            optionsSheet.setColumnWidth(columnIndex, 28 * 256);
        }
        workbook.setActiveSheet(0);
    }

    private List<String> activeCategoryOptions() {
        return categoryMapper.selectAll().stream()
                .filter(this::isActive)
                .map(item -> codeOption(item.getCode(), item.getName()))
                .toList();
    }

    private List<String> activeSubcategoryOptions() {
        return subcategoryMapper.selectAll().stream()
                .filter(this::isActive)
                .map(item -> codeOption(item.getCode(), item.getName()))
                .toList();
    }

    private List<String> activeBrandOptions() {
        return brandMapper.selectAll().stream()
                .filter(this::isActive)
                .map(item -> codeOption(item.getCode(), item.getName()))
                .toList();
    }

    private List<String> activeBandOptions() {
        return bandMapper.selectAll().stream()
                .filter(this::isActive)
                .map(item -> codeOption(item.getCode(), item.getDescription()))
                .toList();
    }

    private List<String> activeMaterialOptions() {
        return materialMasterMapper.selectAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .filter(item -> StringUtils.hasText(item.getMaterialCode()))
                .map(item -> codeOption(item.getMaterialCode(), materialOptionLabel(item)))
                .toList();
    }

    private boolean isActive(ProductCategory item) {
        return item != null && Boolean.TRUE.equals(item.getIsActive());
    }

    private boolean isActive(ProductSubcategory item) {
        return item != null && Boolean.TRUE.equals(item.getIsActive());
    }

    private boolean isActive(ProductBrand item) {
        return item != null && Boolean.TRUE.equals(item.getIsActive());
    }

    private boolean isActive(ProductBand item) {
        return item != null && Boolean.TRUE.equals(item.getIsActive());
    }

    private String codeOption(String code, String label) {
        String normalizedCode = StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : "";
        String normalizedLabel = StringUtils.hasText(label) ? label.trim() : "";
        return StringUtils.hasText(normalizedLabel) ? normalizedCode + " " + normalizedLabel : normalizedCode;
    }

    private String materialOptionLabel(MaterialMaster item) {
        StringBuilder label = new StringBuilder();
        if (StringUtils.hasText(item.getProductName())) {
            label.append(item.getProductName().trim());
        }
        if (StringUtils.hasText(item.getProductModel())) {
            appendPart(label, item.getProductModel().trim());
        }
        if (StringUtils.hasText(item.getUnit())) {
            appendPart(label, item.getUnit().trim());
        }
        return label.toString();
    }

    private void appendPart(StringBuilder builder, String value) {
        if (builder.length() > 0) {
            builder.append(" / ");
        }
        builder.append(value);
    }
}
