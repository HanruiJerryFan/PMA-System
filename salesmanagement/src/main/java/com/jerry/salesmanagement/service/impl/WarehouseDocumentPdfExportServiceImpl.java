package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.PdfExportSupport;
import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport;
import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport.DocumentPage;
import com.jerry.salesmanagement.common.WarehouseDocumentPrintSupport.DocumentPrintMeta;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.service.SystemConfigService;
import com.jerry.salesmanagement.service.WarehouseDocumentPdfExportService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

@Service
public class WarehouseDocumentPdfExportServiceImpl extends PdfExportSupport implements WarehouseDocumentPdfExportService {

    public WarehouseDocumentPdfExportServiceImpl(
            SystemConfigService systemConfigService,
            @Value("${app.pdf.font-path:}") String configuredFontPath
    ) {
        super(systemConfigService, configuredFontPath);
    }

    @Override
    public byte[] exportPdf(WarehouseDocument warehouseDocument, String projectName, boolean includePrice) {
        if (warehouseDocument == null) {
            throw new IllegalArgumentException("Warehouse document is required");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(resolveSongtiPdfFontFile(), "SalesSongtiFont");
            builder.withHtmlContent(buildHtml(warehouseDocument, projectName, includePrice), null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export warehouse document PDF: " + e.getMessage(), e);
        }
    }

    String buildHtml(WarehouseDocument warehouseDocument, String projectName) {
        return buildHtml(warehouseDocument, projectName, true);
    }

    String buildHtml(WarehouseDocument warehouseDocument, String projectName, boolean includePrice) {
        String companyName = WarehouseDocumentPrintSupport.resolveCompanyName(getSystemConfigService());
        DocumentPrintMeta meta = WarehouseDocumentPrintSupport.resolveMeta(warehouseDocument, companyName);
        List<DocumentPage> pages = WarehouseDocumentPrintSupport.paginate(warehouseDocument);

        StringBuilder pagesHtml = new StringBuilder();
        for (DocumentPage page : pages) {
            pagesHtml.append(buildPageHtml(warehouseDocument, projectName, meta, page, includePrice));
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page { size: 216mm 155mm; margin: 2mm 2.7mm 2mm 2.7mm; }
                    * { box-sizing: border-box; }
                    html, body {
                      margin: 0;
                      padding: 0;
                      color: #000;
                      font-family: 'SalesSongtiFont', SimSun, serif;
                    }
                    .doc-page { width: 210.6mm; margin: 0 auto; }
                    .doc-page.page-break { page-break-after: always; }
                    table.page-frame {
                      width: 210.6mm;
                      border-collapse: separate;
                      border-spacing: 0;
                      table-layout: fixed;
                      margin: 0 auto;
                    }
                    table.form-grid {
                      width: 198.69mm;
                      border-collapse: collapse;
                      table-layout: fixed;
                      margin: 0;
                    }
                    td.frame-cell {
                      border: none;
                      padding: 0;
                      vertical-align: top;
                    }
                    table.form-grid td, table.form-grid th {
                      padding: 0 2pt;
                      vertical-align: middle;
                      line-height: 1.15;
                      word-wrap: break-word;
                    }
                    th { font-weight: 400; }
                    .b-all { border: 0.75pt solid #000; }
                    .b-tbr { border-top: 0.75pt solid #000; border-right: 0.75pt solid #000; border-bottom: 0.75pt solid #000; border-left: none; }
                    .b-tbl { border-top: 0.75pt solid #000; border-bottom: 0.75pt solid #000; border-left: 0.75pt solid #000; border-right: none; }
                    .b-none { border: none; padding: 0; }
                    .r-1 { height: 30pt; }
                    .r-2 { height: 7pt; }
                    .r-3-5 { height: 17pt; }
                    .r-6 { height: 20pt; }
                    .r-item { height: 25pt; }
                    .r-17 { height: 25pt; }
                    .r-18 { height: 10pt; }
                    .r-19 { height: 25pt; }
                    .title-cell {
                      text-align: center;
                      vertical-align: middle;
                      font-size: 18pt;
                      font-weight: 700;
                      white-space: pre;
                      border-bottom: 2.25pt double #000;
                    }
                    .page-num-cell {
                      text-align: right;
                      vertical-align: bottom;
                      font-size: 9pt;
                      padding: 0 2pt 2pt 0;
                    }
                    .meta-label-bold {
                      text-align: center;
                      font-size: 9pt;
                      font-weight: 700;
                    }
                    .meta-project {
                      text-align: left;
                      font-size: 10pt;
                    }
                    .meta-party {
                      text-align: left;
                      font-size: 8.5pt;
                      line-height: 1.2;
                    }
                    .meta-small-center {
                      text-align: center;
                      font-size: 9pt;
                    }
                    td.frame-cell.copy-stub {
                      text-align: center;
                      vertical-align: middle;
                      font-size: 9pt;
                      line-height: 1.18;
                    }
                    .tbl-head {
                      text-align: center;
                      font-size: 9pt;
                      font-weight: 400;
                    }
                    .c-idx { text-align: center; font-size: 9pt; }
                    .c-name { text-align: left; font-size: 10pt; }
                    .c-model { text-align: left; font-size: 10pt; }
                    .c-brand { text-align: center; font-size: 9pt; }
                    .c-unit { text-align: center; font-size: 10pt; }
                    .c-qty { text-align: center; font-size: 10pt; }
                    .c-price { text-align: center; font-size: 10pt; }
                    .c-amt { text-align: right; font-size: 10pt; }
                    .c-remark { text-align: left; font-size: 8pt; }
                    .total-label {
                      text-align: right;
                      font-size: 11pt;
                      font-weight: 700;
                      padding-right: 2pt;
                    }
                    .total-upper {
                      text-align: left;
                      font-size: 11pt;
                      font-weight: 700;
                      padding-left: 2pt;
                    }
                    .total-num {
                      text-align: center;
                      font-size: 9pt;
                    }
                    .footer-cell {
                      text-align: left;
                      font-size: 11pt;
                      font-weight: 700;
                      white-space: pre;
                      padding: 0;
                    }
                    .clip-25 { max-height: 23.5pt; overflow: hidden; }
                    .clip-51 { max-height: 49pt; overflow: hidden; }
                    .clip-16 { max-height: 15.5pt; overflow: hidden; }
                    .clip-stub { max-height: 342pt; overflow: hidden; }
                  </style>
                </head>
                <body>
                  %s
                </body>
                </html>
                """.formatted(pagesHtml);
    }

    private String buildPageHtml(
            WarehouseDocument doc,
            String projectName,
            DocumentPrintMeta meta,
            DocumentPage page,
            boolean includePrice
    ) {
        StringBuilder colGroup11 = new StringBuilder("<colgroup>");
        for (int i = 0; i < WarehouseDocumentPrintSupport.COLUMN_COUNT - 1; i++) {
            colGroup11.append("<col style=\"width:")
                    .append(WarehouseDocumentPrintSupport.COLUMN_WIDTHS_MM[i])
                    .append("mm\" />");
        }
        colGroup11.append("</colgroup>");

        StringBuilder itemRows = new StringBuilder();
        List<WarehouseDocumentItem> rows = page.rows();
        for (int i = 0; i < rows.size(); i++) {
            itemRows.append(buildItemRow(rows.get(i), page.startLineNo() + i, includePrice));
        }

        BigDecimal displayTotal = page.lastPage()
                ? resolveTotalAmount(doc)
                : page.pageSubtotal();
        String totalLabel = page.lastPage() ? "合计：" : "本页小计：";
        String totalUppercaseText = includePrice ? toChineseUppercaseRmb(displayTotal) : "";
        String totalAmountText = includePrice ? "¥" + WarehouseDocumentPrintSupport.formatMoneyGrouped(displayTotal) : "";
        String pageIndicatorHtml = page.totalPages() > 1
                ? "<td colspan=\"3\" class=\"b-none page-num-cell\">第 %d 页 / 共 %d 页</td>"
                .formatted(page.pageNumber(), page.totalPages())
                : "<td colspan=\"3\" class=\"b-none\"></td>";

        String pageClass = page.lastPage() ? "doc-page" : "doc-page page-break";
        float copyStubWidthMm = WarehouseDocumentPrintSupport.COLUMN_WIDTHS_MM[WarehouseDocumentPrintSupport.COLUMN_COUNT - 1];

        return """
                <div class="%s">
                  <table class="page-frame">
                    <colgroup>
                      <col style="width:198.69mm" />
                      <col style="width:%smm" />
                    </colgroup>
                    <tr>
                      <td class="frame-cell">
                        <table class="form-grid">
                          %s
                          <tr class="r-1">
                            <td colspan="4" class="b-none"></td>
                            <td colspan="4" class="title-cell">%s</td>
                            %s
                          </tr>
                          <tr class="r-2">
                            <td colspan="11" class="b-none"></td>
                          </tr>
                        </table>
                      </td>
                      <td class="frame-cell"></td>
                    </tr>
                    <tr>
                      <td class="frame-cell">
                        <table class="form-grid">
                          %s
                          <tr class="r-3-5">
                            <td rowspan="3" class="b-all meta-label-bold"><div class="clip-51">%s</div></td>
                            <td rowspan="3" colspan="2" class="b-tbr meta-project"><div class="clip-51">%s</div></td>
                            <td rowspan="3" class="b-all meta-label-bold"><div class="clip-51">%s</div></td>
                            <td rowspan="3" colspan="4" class="b-tbr meta-party"><div class="clip-51">%s</div></td>
                            <td class="b-tbr meta-small-center"><div class="clip-16">%s</div></td>
                            <td colspan="2" class="b-tbr meta-small-center"><div class="clip-16">%s</div></td>
                          </tr>
                          <tr class="r-3-5">
                            <td class="b-tbr meta-small-center"><div class="clip-16">合同编号：</div></td>
                            <td colspan="2" class="b-tbr meta-small-center"><div class="clip-16">%s</div></td>
                          </tr>
                          <tr class="r-3-5">
                            <td class="b-tbr meta-small-center"><div class="clip-16">%s</div></td>
                            <td colspan="2" class="b-tbr meta-small-center"><div class="clip-16">%s</div></td>
                          </tr>
                          <tr class="r-6">
                            <th class="b-all tbl-head">序号</th>
                            <th colspan="2" class="b-tbr tbl-head">材料名称</th>
                            <th colspan="2" class="b-tbr tbl-head">型号及规格</th>
                            <th class="b-tbr tbl-head">品牌</th>
                            <th class="b-tbr tbl-head">单位</th>
                            <th class="b-tbr tbl-head">数量</th>
                            <th class="b-tbr tbl-head">单价</th>
                            <th class="b-tbr tbl-head">金额</th>
                            <th class="b-tbr tbl-head">备注</th>
                          </tr>
                          %s
                          <tr class="r-17">
                            <td colspan="2" class="b-tbl total-label">%s</td>
                            <td colspan="7" class="b-tbr total-upper">%s</td>
                            <td colspan="2" class="b-tbr total-num">%s</td>
                          </tr>
                        </table>
                      </td>
                      <td class="frame-cell copy-stub"><div class="clip-stub">%s</div></td>
                    </tr>
                    <tr>
                      <td class="frame-cell">
                        <table class="form-grid">
                          %s
                          <tr class="r-18">
                            <td colspan="11" class="b-none"></td>
                          </tr>
                          <tr class="r-19">
                            <td colspan="11" class="b-none footer-cell">%s</td>
                          </tr>
                        </table>
                      </td>
                      <td class="frame-cell"></td>
                    </tr>
                  </table>
                </div>
                """.formatted(
                pageClass,
                copyStubWidthMm,
                colGroup11,
                escapeHtml(meta.spacedTitle()),
                pageIndicatorHtml,
                colGroup11,
                formatMultilineHtmlPreservingEmpty("项目\n名称"),
                escapeHtml(WarehouseDocumentPrintSupport.safeTrim(projectName)),
                formatMultilineHtmlPreservingEmpty(meta.partyBlockLabel()),
                formatMultilineHtmlNonBlank(WarehouseDocumentPrintSupport.resolveCounterpartyText(doc)),
                escapeHtml(meta.numberLabel()),
                escapeHtml(WarehouseDocumentPrintSupport.safeTrim(doc.getDocNumber())),
                escapeHtml(WarehouseDocumentPrintSupport.safeTrim(doc.getContractNumber())),
                escapeHtml(meta.dateLabel()),
                escapeHtml(formatChineseDate(doc.getDocDate())),
                itemRows,
                escapeHtml(totalLabel),
                escapeHtml(totalUppercaseText),
                escapeHtml(totalAmountText),
                formatMultilineHtmlPreservingEmpty(meta.copyStubText()),
                colGroup11,
                escapeHtml(meta.footerText())
        );
    }

    private String buildItemRow(WarehouseDocumentItem item, int lineIndex, boolean includePrice) {
        String name = WarehouseDocumentPrintSupport.firstNonBlank(item.getDisplayName(), item.getMaterialName());
        String model = WarehouseDocumentPrintSupport.firstNonBlank(item.getDisplayModel(), item.getModel());
        BigDecimal amount = includePrice ? WarehouseDocumentPrintSupport.resolveItemAmount(item) : null;
        String unitPriceText = includePrice ? formatUnitPrice(item.getUnitPrice()) : "";
        String amountText = includePrice ? WarehouseDocumentPrintSupport.formatMoneyGrouped(amount) : "";
        return """
                <tr class="r-item">
                  <td class="b-all c-idx"><div class="clip-25">%d</div></td>
                  <td colspan="2" class="b-tbr c-name"><div class="clip-25">%s</div></td>
                  <td colspan="2" class="b-tbr c-model"><div class="clip-25">%s</div></td>
                  <td class="b-tbr c-brand"><div class="clip-25">%s</div></td>
                  <td class="b-tbr c-unit"><div class="clip-25">%s</div></td>
                  <td class="b-tbr c-qty"><div class="clip-25">%s</div></td>
                  <td class="b-tbr c-price"><div class="clip-25">%s</div></td>
                  <td class="b-tbr c-amt"><div class="clip-25">%s</div></td>
                  <td class="b-tbr c-remark"><div class="clip-25">%s</div></td>
                </tr>
                """.formatted(
                lineIndex,
                escapeHtml(name),
                escapeHtml(model),
                escapeHtml(WarehouseDocumentPrintSupport.safeTrim(item.getBrand())),
                escapeHtml(WarehouseDocumentPrintSupport.safeTrim(item.getUnit())),
                escapeHtml(trimTrailingZero(item.getQuantity())),
                escapeHtml(unitPriceText),
                escapeHtml(amountText),
                escapeHtml(WarehouseDocumentPrintSupport.safeTrim(item.getRemark()))
        );
    }

    private String trimTrailingZero(BigDecimal value) {
        return WarehouseDocumentPrintSupport.formatQuantity(value);
    }

    private String formatUnitPrice(BigDecimal value) {
        return WarehouseDocumentPrintSupport.formatUnitPrice(value);
    }

    private BigDecimal resolveTotalAmount(WarehouseDocument warehouseDocument) {
        return WarehouseDocumentPrintSupport.resolveTotalAmount(warehouseDocument);
    }

    private String formatMultilineHtmlNonBlank(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::escapeHtml)
                .reduce((left, right) -> left + "<br />" + right)
                .orElse("");
    }

    private String formatMultilineHtmlPreservingEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String[] lines = value.split("\\R", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append("<br />");
            }
            builder.append(escapeHtml(lines[i]));
        }
        return builder.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
