package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class WarehouseDocumentExportTests {

    private SystemConfigService systemConfigService;
    private WarehouseDocumentPdfExportServiceImpl pdfService;
    private WarehouseDocumentExcelExportServiceImpl excelService;

    @BeforeEach
    void setUp() {
        systemConfigService = Mockito.mock(SystemConfigService.class);
        when(systemConfigService.getString(eq("pdf.font.path"), isNull())).thenReturn(null);
        when(systemConfigService.getString(eq(WarehouseDocumentPrintSupport.COMPANY_NAME_CONFIG_KEY), isNull()))
                .thenReturn(null);
        pdfService = new WarehouseDocumentPdfExportServiceImpl(systemConfigService, "");
        excelService = new WarehouseDocumentExcelExportServiceImpl(systemConfigService);
    }

    @Test
    void outboundPdfSinglePageMatchesTemplateGeometry() throws Exception {
        WarehouseDocument doc = buildSampleDoc("OUTBOUND_NOTE", "FHD202610001", 2);

        byte[] pdf = pdfService.exportPdf(doc, "测试示范园区项目");
        assertTrue(pdf.length > 1000);

        try (PDDocument pd = PDDocument.load(pdf)) {
            assertEquals(1, pd.getNumberOfPages(), "10 items or fewer must fit cleanly on a single page");
            PDRectangle box = pd.getPage(0).getMediaBox();
            assertEquals(216f * 72f / 25.4f, box.getWidth(), 2.0f);
            assertEquals(155f * 72f / 25.4f, box.getHeight(), 2.0f);

            String text = new PDFTextStripper().getText(pd);
            assertTrue(text.contains("测试示范园区项目"));
            assertTrue(text.contains("示例收货单位"));
            assertTrue(text.contains("FHD202610001"));
            assertTrue(text.contains("XS-2026-0088"));
            assertTrue(text.contains("合计："));
            assertTrue(text.contains("人民币"));
            assertTrue(text.contains(WarehouseDocumentPrintSupport.DEFAULT_COMPANY_NAME));
            assertTrue(text.contains("（白）"));
            assertTrue(text.contains("（红）"));
            assertTrue(text.contains("（黄）"));
            assertTrue(text.contains("10"));
            assertFalse(text.contains("第 1 页"), "Single page document must not render page indicator");
        }
    }

    @Test
    void outboundPdfPaginatesOverTenItems() throws Exception {
        WarehouseDocument doc = buildSampleDoc("OUTBOUND_NOTE", "FHD202610012", 12);

        byte[] pdf = pdfService.exportPdf(doc, "多页示范项目");
        try (PDDocument pd = PDDocument.load(pdf)) {
            assertEquals(2, pd.getNumberOfPages());

            PDFTextStripper p1Stripper = new PDFTextStripper();
            p1Stripper.setStartPage(1);
            p1Stripper.setEndPage(1);
            String page1Text = p1Stripper.getText(pd);
            assertTrue(page1Text.contains("第 1 页 / 共 2 页"));
            assertTrue(page1Text.contains("本页小计："));
            assertTrue(page1Text.contains("物料10"));
            assertFalse(page1Text.contains("物料11"));

            PDFTextStripper p2Stripper = new PDFTextStripper();
            p2Stripper.setStartPage(2);
            p2Stripper.setEndPage(2);
            String page2Text = p2Stripper.getText(pd);
            assertTrue(page2Text.contains("第 2 页 / 共 2 页"));
            assertTrue(page2Text.contains("合计："));
            assertTrue(page2Text.contains("物料11"));
            assertTrue(page2Text.contains("物料12"));
            assertTrue(page2Text.contains("20"));
        }
    }

    @Test
    void supportsInboundTransferAndCustomCompanyConfig() throws Exception {
        when(systemConfigService.getString(eq(WarehouseDocumentPrintSupport.COMPANY_NAME_CONFIG_KEY), isNull()))
                .thenReturn("自定义测试供应链有限公司");

        WarehouseDocument inbound = buildSampleDoc("INBOUND_NOTE", "RKD202610001", 1);
        byte[] inboundPdf = pdfService.exportPdf(inbound, "入库测试项目");
        try (PDDocument pd = PDDocument.load(inboundPdf)) {
            assertEquals(1, pd.getNumberOfPages());
            String text = new PDFTextStripper().getText(pd);
            assertTrue(text.contains("入库单编号"));
            assertTrue(text.contains("存"));
            assertTrue(text.contains("收货单位：自定义测试供应链有限公司"));
        }

        WarehouseDocument transferInbound = buildSampleDoc("INBOUND_NOTE", "RKD202610002", 1);
        transferInbound.setSourceRefType("WAREHOUSE_TRANSFER");
        byte[] transferInboundPdf = pdfService.exportPdf(transferInbound, "划拨测试项目");
        try (PDDocument pd = PDDocument.load(transferInboundPdf)) {
            assertEquals(1, pd.getNumberOfPages());
            String text = new PDFTextStripper().getText(pd);
            assertTrue(text.contains("调出仓库"));
            assertTrue(text.contains("调入单位：自定义测试供应链有限公司"));
        }

        WarehouseDocument transfer = buildSampleDoc("TRANSFER_JTX", "HBJ202610001", 1);
        byte[] transferPdf = pdfService.exportPdf(transfer, "划拨测试项目");
        try (PDDocument pd = PDDocument.load(transferPdf)) {
            assertEquals(1, pd.getNumberOfPages());
            String text = new PDFTextStripper().getText(pd);
            assertTrue(text.contains("JTX"));
            assertTrue(text.contains("划拨单编号"));
            assertTrue(text.contains("调出单位：自定义测试供应链有限公司"));
        }
    }

    @Test
    void outboundExcelSinglePageMatchesTemplateExactSpec() throws Exception {
        WarehouseDocument doc = buildSampleDoc("OUTBOUND_NOTE", "FHD202610033", 2);

        byte[] xlsx = excelService.exportExcel(doc, "南京示范二期");
        assertTrue(xlsx.length > 1000);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals("Sheet1", sheet.getSheetName());

            for (int c = 0; c < WarehouseDocumentPrintSupport.COLUMN_COUNT; c++) {
                assertEquals(
                        WarehouseDocumentPrintSupport.EXCEL_COLUMN_WIDTHS[c],
                        sheet.getColumnWidth(c),
                        "Column " + c + " width mismatch"
                );
            }

            assertEquals(18, sheet.getLastRowNum());
            for (int r = 0; r < WarehouseDocumentPrintSupport.ROWS_PER_PAGE; r++) {
                XSSFRow row = sheet.getRow(r);
                assertNotNull(row, "Row " + r + " must exist");
                assertEquals(WarehouseDocumentPrintSupport.ROW_HEIGHTS_PT[r], row.getHeightInPoints(), 0.01f);
            }

            PrintSetup printSetup = sheet.getPrintSetup();
            assertEquals(PrintSetup.LEGAL_PAPERSIZE, printSetup.getPaperSize());
            assertFalse(printSetup.getLandscape());
            assertEquals(600, printSetup.getHResolution());
            assertTrue(sheet.getHorizontallyCenter());

            assertEquals(WarehouseDocumentPrintSupport.EXCEL_MARGIN_LEFT, sheet.getMargin(PageMargin.LEFT), 1e-6);
            assertEquals(WarehouseDocumentPrintSupport.EXCEL_MARGIN_RIGHT, sheet.getMargin(PageMargin.RIGHT), 1e-6);
            assertEquals(WarehouseDocumentPrintSupport.EXCEL_MARGIN_TOP, sheet.getMargin(PageMargin.TOP), 1e-6);
            assertEquals(WarehouseDocumentPrintSupport.EXCEL_MARGIN_BOTTOM, sheet.getMargin(PageMargin.BOTTOM), 1e-6);
            assertEquals(WarehouseDocumentPrintSupport.EXCEL_MARGIN_HEADER, sheet.getMargin(PageMargin.HEADER), 1e-6);
            assertEquals(WarehouseDocumentPrintSupport.EXCEL_MARGIN_FOOTER, sheet.getMargin(PageMargin.FOOTER), 1e-6);

            assertEquals("Sheet1!$A$1:$L$19", wb.getPrintArea(0));
            assertEquals(35, sheet.getNumMergedRegions());
            Set<String> merges = sheet.getMergedRegions().stream()
                    .map(CellRangeAddress::formatAsString)
                    .collect(Collectors.toSet());
            assertTrue(merges.containsAll(List.of(
                    "E1:H1", "A3:A5", "B3:C5", "D3:D5", "E3:H5",
                    "J3:K3", "J4:K4", "J5:K5", "L3:L17",
                    "B6:C6", "D6:E6", "A17:B17", "C17:I17", "J17:K17", "A19:K19"
            )));

            assertEquals("发    货    单", sheet.getRow(0).getCell(4).getStringCellValue());
            assertEquals(BorderStyle.DOUBLE, sheet.getRow(0).getCell(4).getCellStyle().getBorderBottom());
            assertEquals("南京示范二期", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("FHD202610033", sheet.getRow(2).getCell(9).getStringCellValue());
            assertEquals("XS-2026-0088", sheet.getRow(3).getCell(9).getStringCellValue());
            assertNotNull(sheet.getRow(4).getCell(9).getDateCellValue());
            assertEquals("序号", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("合计：", sheet.getRow(16).getCell(0).getStringCellValue());
            assertEquals(BorderStyle.NONE, sheet.getRow(16).getCell(1).getCellStyle().getBorderRight());
            assertEquals(BorderStyle.NONE, sheet.getRow(16).getCell(2).getCellStyle().getBorderLeft());
            assertEquals(BorderStyle.NONE, sheet.getRow(2).getCell(11).getCellStyle().getBorderLeft());
            assertEquals(1100.0, sheet.getRow(16).getCell(9).getNumericCellValue(), 0.001);
            assertTrue(sheet.getRow(18).getCell(0).getStringCellValue().contains(WarehouseDocumentPrintSupport.DEFAULT_COMPANY_NAME));
        }
    }

    @Test
    void outboundExcelMultiPageSetsRowBreakAndSubtotal() throws Exception {
        WarehouseDocument doc = buildSampleDoc("OUTBOUND_NOTE", "FHD202610099", 12);

        byte[] xlsx = excelService.exportExcel(doc, "多页导出项目");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals(37, sheet.getLastRowNum());
            assertEquals("Sheet1!$A$1:$L$38", wb.getPrintArea(0));
            assertArrayEquals(new int[]{18}, sheet.getRowBreaks());

            assertEquals("第 1 页 / 共 2 页", sheet.getRow(0).getCell(8).getStringCellValue());
            assertEquals("本页小计：", sheet.getRow(16).getCell(0).getStringCellValue());
            assertEquals("第 2 页 / 共 2 页", sheet.getRow(19).getCell(8).getStringCellValue());
            assertEquals("合计：", sheet.getRow(35).getCell(0).getStringCellValue());
            assertEquals(12d, sheet.getRow(26).getCell(0).getNumericCellValue());
            assertEquals(20d, sheet.getRow(34).getCell(0).getNumericCellValue());
            Cell docTotalCell = sheet.getRow(35).getCell(9);
            assertEquals(doc.getTotalAmount().doubleValue(), docTotalCell.getNumericCellValue(), 0.001);
        }
    }

    private WarehouseDocument buildSampleDoc(String docType, String docNumber, int itemCount) {
        WarehouseDocument doc = new WarehouseDocument();
        doc.setDocType(docType);
        doc.setDocNumber(docNumber);
        doc.setContractNumber("XS-2026-0088");
        doc.setDocDate(Date.valueOf(LocalDate.of(2026, 11, 13)));
        doc.setCounterpartyName("示例收货单位");
        doc.setCounterpartyAddress("测试市高新区示例大道100号");
        doc.setCounterpartyContact("测试联系人 13800000001");

        List<WarehouseDocumentItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i <= itemCount; i++) {
            WarehouseDocumentItem item = new WarehouseDocumentItem();
            item.setLineNo(i);
            item.setDisplayName("物料" + i);
            item.setDisplayModel("MOD-" + i);
            item.setBrand("测试品牌");
            item.setUnit("台");
            BigDecimal qty = new BigDecimal(i);
            BigDecimal price = new BigDecimal("100.00");
            BigDecimal amt = qty.multiply(price);
            if (i <= 2 && itemCount == 2) {
                qty = i == 1 ? new BigDecimal("2") : new BigDecimal("9");
                amt = qty.multiply(price);
            }
            item.setQuantity(qty);
            item.setUnitPrice(price);
            item.setAmount(amt);
            item.setRemark("备注" + i);
            total = total.add(amt);
            items.add(item);
        }
        doc.setItems(items);
        doc.setTotalAmount(total);
        return doc;
    }
}
