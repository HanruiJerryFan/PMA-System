package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.PdfExportSupport;
import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport;
import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport.DocumentPage;
import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport.DocumentPrintMeta;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.service.SystemConfigService;
import com.jerry.salesmanagement.service.WarehouseDocumentExcelExportService;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class WarehouseDocumentExcelExportServiceImpl implements WarehouseDocumentExcelExportService {

    private final SystemConfigService systemConfigService;

    public WarehouseDocumentExcelExportServiceImpl(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public byte[] exportExcel(WarehouseDocument warehouseDocument, String projectName, boolean includePrice) {
        if (warehouseDocument == null) {
            throw new IllegalArgumentException("Warehouse document is required");
        }
        String companyName = WarehouseDocumentPrintSupport.resolveCompanyName(systemConfigService);
        DocumentPrintMeta meta = WarehouseDocumentPrintSupport.resolveMeta(warehouseDocument, companyName);
        List<DocumentPage> pages = WarehouseDocumentPrintSupport.paginate(warehouseDocument);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Sheet1");
            configureSheetLayout(sheet);

            StylePalette styles = new StylePalette(workbook);
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                DocumentPage page = pages.get(pageIndex);
                int baseRow = pageIndex * WarehouseDocumentPrintSupport.ROWS_PER_PAGE;
                writePage(sheet, styles, warehouseDocument, projectName, meta, page, baseRow, includePrice);
                if (pageIndex < pages.size() - 1) {
                    sheet.setRowBreak(baseRow + WarehouseDocumentPrintSupport.ROWS_PER_PAGE - 1);
                }
            }

            int lastRowIndex = pages.size() * WarehouseDocumentPrintSupport.ROWS_PER_PAGE - 1;
            workbook.setPrintArea(0, 0, WarehouseDocumentPrintSupport.COLUMN_COUNT - 1, 0, lastRowIndex);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export warehouse document Excel: " + e.getMessage(), e);
        }
    }

    private void configureSheetLayout(XSSFSheet sheet) {
        sheet.setDefaultColumnWidth(9);
        sheet.setDefaultRowHeightInPoints(13.5f);
        for (int col = 0; col < WarehouseDocumentPrintSupport.COLUMN_COUNT; col++) {
            sheet.setColumnWidth(col, WarehouseDocumentPrintSupport.EXCEL_COLUMN_WIDTHS[col]);
        }

        sheet.setMargin(PageMargin.LEFT, WarehouseDocumentPrintSupport.EXCEL_MARGIN_LEFT);
        sheet.setMargin(PageMargin.RIGHT, WarehouseDocumentPrintSupport.EXCEL_MARGIN_RIGHT);
        sheet.setMargin(PageMargin.TOP, WarehouseDocumentPrintSupport.EXCEL_MARGIN_TOP);
        sheet.setMargin(PageMargin.BOTTOM, WarehouseDocumentPrintSupport.EXCEL_MARGIN_BOTTOM);
        sheet.setMargin(PageMargin.HEADER, WarehouseDocumentPrintSupport.EXCEL_MARGIN_HEADER);
        sheet.setMargin(PageMargin.FOOTER, WarehouseDocumentPrintSupport.EXCEL_MARGIN_FOOTER);
        sheet.setHorizontallyCenter(true);

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setPaperSize(PrintSetup.LEGAL_PAPERSIZE);
        printSetup.setLandscape(false);
        printSetup.setHResolution((short) 600);
    }

    private void writePage(
            XSSFSheet sheet,
            StylePalette styles,
            WarehouseDocument doc,
            String projectName,
            DocumentPrintMeta meta,
            DocumentPage page,
            int baseRow,
            boolean includePrice
    ) {
        XSSFRow[] rows = new XSSFRow[WarehouseDocumentPrintSupport.ROWS_PER_PAGE];
        for (int r = 0; r < WarehouseDocumentPrintSupport.ROWS_PER_PAGE; r++) {
            XSSFRow row = sheet.createRow(baseRow + r);
            row.setHeightInPoints(WarehouseDocumentPrintSupport.ROW_HEIGHTS_PT[r]);
            rows[r] = row;
        }

        // Row 1 (index 0): Title E1:H1 (+ optional page indicator I1:K1)
        styleRange(rows, 0, 0, 4, 7, styles.title);
        getOrCreateCell(rows[0], 4).setCellValue(meta.spacedTitle());
        addMerge(sheet, baseRow, 0, 0, 4, 7);

        if (page.totalPages() > 1) {
            styleRange(rows, 0, 0, 8, 10, styles.pageNumber);
            getOrCreateCell(rows[0], 8).setCellValue("第 " + page.pageNumber() + " 页 / 共 " + page.totalPages() + " 页");
            addMerge(sheet, baseRow, 0, 0, 8, 10);
        }

        // Rows 3-5 (indices 2..4): Metadata block + L3:L17 vertical copy legend
        styleRange(rows, 2, 4, 0, 0, styles.metaLabelBold);
        getOrCreateCell(rows[2], 0).setCellValue("项目\n名称");
        addMerge(sheet, baseRow, 2, 4, 0, 0);

        styleRange(rows, 2, 4, 1, 2, styles.metaProject);
        getOrCreateCell(rows[2], 1).setCellValue(WarehouseDocumentPrintSupport.safeTrim(projectName));
        addMerge(sheet, baseRow, 2, 4, 1, 2);

        styleRange(rows, 2, 4, 3, 3, styles.metaLabelBold);
        getOrCreateCell(rows[2], 3).setCellValue(meta.partyBlockLabel());
        addMerge(sheet, baseRow, 2, 4, 3, 3);

        styleRange(rows, 2, 4, 4, 7, styles.metaParty);
        getOrCreateCell(rows[2], 4).setCellValue(WarehouseDocumentPrintSupport.resolveCounterpartyText(doc));
        addMerge(sheet, baseRow, 2, 4, 4, 7);

        setTextCell(rows[2], 8, styles.metaCenter9, meta.numberLabel());
        styleRange(rows, 2, 2, 9, 10, styles.metaCenter9);
        getOrCreateCell(rows[2], 9).setCellValue(WarehouseDocumentPrintSupport.safeTrim(doc.getDocNumber()));
        addMerge(sheet, baseRow, 2, 2, 9, 10);

        setTextCell(rows[3], 8, styles.metaCenter9, "合同编号：");
        styleRange(rows, 3, 3, 9, 10, styles.metaCenter9);
        getOrCreateCell(rows[3], 9).setCellValue(WarehouseDocumentPrintSupport.safeTrim(doc.getContractNumber()));
        addMerge(sheet, baseRow, 3, 3, 9, 10);

        setTextCell(rows[4], 8, styles.metaCenter9, meta.dateLabel());
        styleRange(rows, 4, 4, 9, 10, styles.metaDate);
        if (doc.getDocDate() != null) {
            getOrCreateCell(rows[4], 9).setCellValue(doc.getDocDate());
        }
        addMerge(sheet, baseRow, 4, 4, 9, 10);

        // L3:L17 (rows 2..16, col 11): vertical copy stub
        styleRange(rows, 2, 16, 11, 11, styles.copyStub);
        getOrCreateCell(rows[2], 11).setCellValue(meta.copyStubText());
        addMerge(sheet, baseRow, 2, 16, 11, 11);

        // Row 6 (index 5): detail headers
        setTextCell(rows[5], 0, styles.tableHeader, "序号");
        styleRange(rows, 5, 5, 1, 2, styles.tableHeader);
        getOrCreateCell(rows[5], 1).setCellValue("材料名称");
        addMerge(sheet, baseRow, 5, 5, 1, 2);

        styleRange(rows, 5, 5, 3, 4, styles.tableHeader);
        getOrCreateCell(rows[5], 3).setCellValue("型号及规格");
        addMerge(sheet, baseRow, 5, 5, 3, 4);

        setTextCell(rows[5], 5, styles.tableHeader, "品牌");
        setTextCell(rows[5], 6, styles.tableHeader, "单位");
        setTextCell(rows[5], 7, styles.tableHeader, "数量");
        setTextCell(rows[5], 8, styles.tableHeader, "单价");
        setTextCell(rows[5], 9, styles.tableHeader, "金额");
        setTextCell(rows[5], 10, styles.tableHeader, "备注");

        // Rows 7-16 (indices 6..15): 10 item rows
        List<WarehouseDocumentItem> items = page.rows();
        for (int i = 0; i < WarehouseDocumentPrintSupport.ITEMS_PER_PAGE; i++) {
            XSSFRow row = rows[6 + i];
            WarehouseDocumentItem item = items.get(i);

            XSSFCell idxCell = getOrCreateCell(row, 0);
            idxCell.setCellStyle(styles.itemIndex);
            idxCell.setCellValue(page.startLineNo() + i);

            styleRange(rows, 6 + i, 6 + i, 1, 2, styles.itemText10);
            getOrCreateCell(row, 1).setCellValue(
                    WarehouseDocumentPrintSupport.firstNonBlank(item.getDisplayName(), item.getMaterialName()));
            addMerge(sheet, baseRow, 6 + i, 6 + i, 1, 2);

            styleRange(rows, 6 + i, 6 + i, 3, 4, styles.itemText10);
            getOrCreateCell(row, 3).setCellValue(
                    WarehouseDocumentPrintSupport.firstNonBlank(item.getDisplayModel(), item.getModel()));
            addMerge(sheet, baseRow, 6 + i, 6 + i, 3, 4);

            setTextCell(row, 5, styles.itemBrand, WarehouseDocumentPrintSupport.safeTrim(item.getBrand()));
            setTextCell(row, 6, styles.itemCenter10, WarehouseDocumentPrintSupport.safeTrim(item.getUnit()));

            XSSFCell qtyCell = getOrCreateCell(row, 7);
            qtyCell.setCellStyle(styles.itemCenter10);
            if (item.getQuantity() != null) {
                qtyCell.setCellValue(item.getQuantity().doubleValue());
            }

            XSSFCell priceCell = getOrCreateCell(row, 8);
            priceCell.setCellStyle(styles.itemUnitPrice);
            if (includePrice && item.getUnitPrice() != null) {
                priceCell.setCellValue(item.getUnitPrice().doubleValue());
            }

            XSSFCell amountCell = getOrCreateCell(row, 9);
            amountCell.setCellStyle(styles.itemAmount);
            BigDecimal amount = includePrice ? WarehouseDocumentPrintSupport.resolveItemAmount(item) : null;
            if (amount != null) {
                amountCell.setCellValue(amount.doubleValue());
            }

            setTextCell(row, 10, styles.itemRemark, WarehouseDocumentPrintSupport.safeTrim(item.getRemark()));
        }

        // Row 17 (index 16): Total / Page subtotal row
        BigDecimal displayTotal = page.lastPage()
                ? WarehouseDocumentPrintSupport.resolveTotalAmount(doc)
                : page.pageSubtotal();
        String totalLabel = page.lastPage() ? "合计：" : "本页小计：";

        styleRange(rows, 16, 16, 0, 1, styles.totalLabel);
        getOrCreateCell(rows[16], 0).setCellValue(totalLabel);
        addMerge(sheet, baseRow, 16, 16, 0, 1);

        styleRange(rows, 16, 16, 2, 8, styles.totalUppercase);
        if (includePrice) {
            getOrCreateCell(rows[16], 2).setCellValue(PdfExportSupport.toChineseUppercaseRmbStatic(displayTotal));
        }
        addMerge(sheet, baseRow, 16, 16, 2, 8);

        styleRange(rows, 16, 16, 9, 10, styles.totalAmount);
        if (includePrice) {
            getOrCreateCell(rows[16], 9).setCellValue(displayTotal.doubleValue());
        }
        addMerge(sheet, baseRow, 16, 16, 9, 10);

        // Row 19 (index 18): Footer A19:K19
        styleRange(rows, 18, 18, 0, 10, styles.footer);
        getOrCreateCell(rows[18], 0).setCellValue(meta.footerText());
        addMerge(sheet, baseRow, 18, 18, 0, 10);
    }

    private void addMerge(XSSFSheet sheet, int baseRow, int firstRowOffset, int lastRowOffset, int firstCol, int lastCol) {
        sheet.addMergedRegion(new CellRangeAddress(
                baseRow + firstRowOffset,
                baseRow + lastRowOffset,
                firstCol,
                lastCol
        ));
    }

    private void styleRange(XSSFRow[] rows, int firstRowOffset, int lastRowOffset, int firstCol, int lastCol, XSSFCellStyle style) {
        for (int r = firstRowOffset; r <= lastRowOffset; r++) {
            for (int c = firstCol; c <= lastCol; c++) {
                getOrCreateCell(rows[r], c).setCellStyle(style);
            }
        }
    }

    private void setTextCell(XSSFRow row, int col, XSSFCellStyle style, String value) {
        XSSFCell cell = getOrCreateCell(row, col);
        cell.setCellStyle(style);
        if (!value.isEmpty()) {
            cell.setCellValue(value);
        }
    }

    private XSSFCell getOrCreateCell(XSSFRow row, int col) {
        XSSFCell cell = row.getCell(col);
        return cell != null ? cell : row.createCell(col);
    }

    private static final class StylePalette {
        final XSSFCellStyle title;
        final XSSFCellStyle pageNumber;
        final XSSFCellStyle metaLabelBold;
        final XSSFCellStyle metaProject;
        final XSSFCellStyle metaParty;
        final XSSFCellStyle metaCenter9;
        final XSSFCellStyle metaDate;
        final XSSFCellStyle copyStub;
        final XSSFCellStyle tableHeader;
        final XSSFCellStyle itemIndex;
        final XSSFCellStyle itemText10;
        final XSSFCellStyle itemBrand;
        final XSSFCellStyle itemCenter10;
        final XSSFCellStyle itemUnitPrice;
        final XSSFCellStyle itemAmount;
        final XSSFCellStyle itemRemark;
        final XSSFCellStyle totalLabel;
        final XSSFCellStyle totalUppercase;
        final XSSFCellStyle totalAmount;
        final XSSFCellStyle footer;

        StylePalette(XSSFWorkbook workbook) {
            DataFormat dataFormat = workbook.createDataFormat();

            Font fontTitle = createSongtiFont(workbook, (short) 360, true);
            Font fontBold11 = createSongtiFont(workbook, (short) 220, true);
            Font fontBold9 = createSongtiFont(workbook, (short) 180, true);
            Font font10 = createSongtiFont(workbook, (short) 200, false);
            Font font9 = createSongtiFont(workbook, (short) 180, false);
            Font font85 = createSongtiFont(workbook, (short) 170, false);
            Font font8 = createSongtiFont(workbook, (short) 160, false);

            this.title = createStyle(workbook, fontTitle, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.title.setBorderBottom(BorderStyle.DOUBLE);

            this.pageNumber = createStyle(workbook, font9, HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM, false);

            this.metaLabelBold = createThinBoxStyle(workbook, fontBold9, HorizontalAlignment.CENTER, true);
            this.metaProject = createThinBoxStyle(workbook, font10, HorizontalAlignment.LEFT, true);
            this.metaParty = createThinBoxStyle(workbook, font85, HorizontalAlignment.LEFT, true);
            this.metaCenter9 = createThinBoxStyle(workbook, font9, HorizontalAlignment.CENTER, false);

            this.metaDate = createThinBoxStyle(workbook, font9, HorizontalAlignment.CENTER, false);
            this.metaDate.setDataFormat(dataFormat.getFormat("yyyy\"年\"m\"月\"d\"日\";@"));

            this.copyStub = createStyle(workbook, font9, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true);
            this.tableHeader = createThinBoxStyle(workbook, font9, HorizontalAlignment.CENTER, false);

            this.itemIndex = createThinBoxStyle(workbook, font9, HorizontalAlignment.CENTER, false);
            this.itemText10 = createThinBoxStyle(workbook, font10, HorizontalAlignment.LEFT, true);
            this.itemBrand = createThinBoxStyle(workbook, font9, HorizontalAlignment.CENTER, true);
            this.itemCenter10 = createThinBoxStyle(workbook, font10, HorizontalAlignment.CENTER, true);

            this.itemUnitPrice = createThinBoxStyle(workbook, font10, HorizontalAlignment.CENTER, false);
            this.itemUnitPrice.setDataFormat(dataFormat.getFormat("0.00##"));

            this.itemAmount = createThinBoxStyle(workbook, font10, HorizontalAlignment.RIGHT, false);
            this.itemAmount.setDataFormat(dataFormat.getFormat("#,##0.00"));

            this.itemRemark = createThinBoxStyle(workbook, font8, HorizontalAlignment.LEFT, true);

            this.totalLabel = createStyle(workbook, fontBold11, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, false);
            this.totalLabel.setBorderTop(BorderStyle.THIN);
            this.totalLabel.setBorderBottom(BorderStyle.THIN);
            this.totalLabel.setBorderLeft(BorderStyle.THIN);

            this.totalUppercase = createStyle(workbook, fontBold11, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false);
            this.totalUppercase.setBorderTop(BorderStyle.THIN);
            this.totalUppercase.setBorderBottom(BorderStyle.THIN);
            this.totalUppercase.setBorderRight(BorderStyle.THIN);

            this.totalAmount = createThinBoxStyle(workbook, font9, HorizontalAlignment.CENTER, false);
            this.totalAmount.setDataFormat(dataFormat.getFormat("\u00A5#,##0.00"));

            this.footer = createStyle(workbook, fontBold11, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false);
        }

        private static Font createSongtiFont(XSSFWorkbook workbook, short fontHeightTwips, boolean bold) {
            Font font = workbook.createFont();
            font.setFontName("宋体");
            font.setFontHeight(fontHeightTwips);
            font.setBold(bold);
            return font;
        }

        private static XSSFCellStyle createStyle(
                XSSFWorkbook workbook,
                Font font,
                HorizontalAlignment hAlign,
                VerticalAlignment vAlign,
                boolean wrapText
        ) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(hAlign);
            style.setVerticalAlignment(vAlign);
            style.setWrapText(wrapText);
            return style;
        }

        private static XSSFCellStyle createThinBoxStyle(
                XSSFWorkbook workbook,
                Font font,
                HorizontalAlignment hAlign,
                boolean wrapText
        ) {
            XSSFCellStyle style = createStyle(workbook, font, hAlign, VerticalAlignment.CENTER, wrapText);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
