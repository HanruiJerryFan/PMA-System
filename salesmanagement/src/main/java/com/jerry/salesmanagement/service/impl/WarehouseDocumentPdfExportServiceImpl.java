package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.PdfExportSupport;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.service.SystemConfigService;
import com.jerry.salesmanagement.service.WarehouseDocumentPdfExportService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarehouseDocumentPdfExportServiceImpl extends PdfExportSupport implements WarehouseDocumentPdfExportService {

    private static final float[] DETAIL_WIDTHS = {6f, 25f, 12f, 12f, 7f, 9f, 10f, 10f, 9f};
    private static final float ROW_HEIGHT = 28f;

    public WarehouseDocumentPdfExportServiceImpl(
            SystemConfigService systemConfigService,
            @Value("${app.pdf.font-path:}") String configuredFontPath
    ) {
        super(systemConfigService, configuredFontPath);
    }

    @Override
    public byte[] exportPdf(WarehouseDocument warehouseDocument, String projectLabel) {
        if (warehouseDocument == null) {
            throw new IllegalArgumentException("Warehouse document is required");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(resolvePdfFontFile(), "SalesPdfFont");
            builder.withHtmlContent(buildHtml(warehouseDocument, projectLabel), null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export warehouse document PDF: " + e.getMessage(), e);
        }
    }

    private List<WarehouseDocumentItem> buildRows(WarehouseDocument warehouseDocument) {
        List<WarehouseDocumentItem> rows = new ArrayList<>();
        if (warehouseDocument.getItems() != null) {
            rows.addAll(warehouseDocument.getItems());
        }
        while (rows.size() < 10) {
            rows.add(new WarehouseDocumentItem());
        }
        return rows;
    }

    private String resolveDocLabel(String docType) {
        return "OUTBOUND_NOTE".equals(docType) ? "发货单" : "入库单";
    }

    private String resolveNumberLabel(String docType) {
        return "OUTBOUND_NOTE".equals(docType) ? "发货单编号" : "入库单编号";
    }

    private String resolveDateLabel(String docType) {
        return "OUTBOUND_NOTE".equals(docType) ? "发货日期" : "入库日期";
    }

    private String resolvePartyBlockLabel(String docType) {
        return "OUTBOUND_NOTE".equals(docType) ? "收货公司、收货地址及收货人" : "发货公司及发货人";
    }

    private String resolveCounterpartyText(WarehouseDocument warehouseDocument) {
        List<String> lines = new ArrayList<>();
        if (warehouseDocument.getCounterpartyName() != null && !warehouseDocument.getCounterpartyName().isBlank()) {
            lines.add(warehouseDocument.getCounterpartyName().trim());
        }
        if (warehouseDocument.getCounterpartyAddress() != null && !warehouseDocument.getCounterpartyAddress().isBlank()) {
            lines.add(warehouseDocument.getCounterpartyAddress().trim());
        }
        if (warehouseDocument.getCounterpartyContact() != null && !warehouseDocument.getCounterpartyContact().isBlank()) {
            lines.add(warehouseDocument.getCounterpartyContact().trim());
        }
        return String.join("\n", lines);
    }

    private String formatCounterpartyHtml(WarehouseDocument warehouseDocument) {
        return resolveCounterpartyText(warehouseDocument)
                .lines()
                .map(this::escapeHtml)
                .filter(line -> !line.isBlank())
                .reduce((left, right) -> left + "<br />" + right)
                .orElse("");
    }

    private String buildHtml(WarehouseDocument warehouseDocument, String projectLabel) {
        String docLabel = resolveDocLabel(warehouseDocument.getDocType());
        List<WarehouseDocumentItem> rows = buildRows(warehouseDocument);
        StringBuilder rowHtml = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            rowHtml.append(buildItemRow(rows.get(i), i));
        }
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page { size: A4 landscape; margin: 6mm; }
                    * { box-sizing: border-box; }
                    body { font-family: 'SalesPdfFont', SimSun, serif; margin: 0; padding: 0; color: #111; }
                    .doc-sheet { width: 100%%; }
                    .doc-title { text-align: center; font-size: 30px; letter-spacing: 8px; margin: 0 0 10px; }
                    table { width: 100%%; border-collapse: collapse; table-layout: fixed; }
                    .meta-table { margin-bottom: -2px; }
                    td, th { border: 2px solid #222; padding: 4px 5px; font-size: 12px; vertical-align: middle; word-break: keep-all; overflow-wrap: normal; }
                    th { background: #fafafa; font-weight: 700; white-space: nowrap; }
                    .label, .nowrap { white-space: nowrap; }
                    .wide-label { font-size: 11px; }
                    .project-cell { font-size: 10px; white-space: nowrap; }
                    .text-cell, .party-cell { white-space: normal; word-break: keep-all; overflow-wrap: break-word; }
                    .party-cell { line-height: 1.45; }
                    .center { text-align: center; }
                    .right { text-align: right; }
                    .strong { font-size: 15px; font-weight: 600; }
                    .model-cell { font-size: 11px; }
                  </style>
                </head>
                <body>
                  <div class="doc-sheet">
                    <div class="doc-title">%s</div>
                    <table class="meta-table">
                      %s
                      <tr>
                        <td class="label center"><strong>项目名称</strong></td>
                        <td class="text-cell project-cell">%s</td>
                        <td class="label center wide-label"><strong>%s</strong></td>
                        <td class="party-cell" colspan="2">%s</td>
                        <td class="label center"><strong>%s</strong></td>
                        <td class="center nowrap">%s</td>
                        <td class="label center"><strong>%s</strong></td>
                        <td class="center nowrap">%s</td>
                      </tr>
                    </table>
                    <table class="detail-table">
                      %s
                      <tr class="center">
                        <th>序号</th><th>材料名称</th><th>型号及规格</th><th>品牌</th><th>单位</th><th>数量</th><th>单价</th><th>金额</th><th>备注</th>
                      </tr>
                      %s
                      <tr>
                        <td colspan="7" class="center strong">合计：%s</td>
                        <td colspan="2" class="right strong">￥%s</td>
                      </tr>
                    </table>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(docLabel),
                columnGroupHtml(),
                escapeHtml(projectLabel),
                escapeHtml(resolvePartyBlockLabel(warehouseDocument.getDocType())),
                formatCounterpartyHtml(warehouseDocument),
                escapeHtml(resolveNumberLabel(warehouseDocument.getDocType())),
                escapeHtml(warehouseDocument.getDocNumber()),
                escapeHtml(resolveDateLabel(warehouseDocument.getDocType())),
                escapeHtml(formatChineseDate(warehouseDocument.getDocDate())),
                columnGroupHtml(),
                rowHtml,
                escapeHtml(toChineseUppercaseRmb(resolveTotalAmount(warehouseDocument))),
                escapeHtml(formatMoney(resolveTotalAmount(warehouseDocument)))
        );
    }

    private String columnGroupHtml() {
        StringBuilder builder = new StringBuilder("<colgroup>");
        for (float width : DETAIL_WIDTHS) {
            builder.append("<col style=\"width:").append(width).append("%\" />");
        }
        return builder.append("</colgroup>").toString();
    }

    private String buildItemRow(WarehouseDocumentItem item, int index) {
        return """
                <tr>
                  <td class="center nowrap">%d</td>
                  <td class="text-cell">%s</td>
                  <td class="text-cell model-cell">%s</td>
                  <td class="center text-cell">%s</td>
                  <td class="center nowrap">%s</td>
                  <td class="right nowrap">%s</td>
                  <td class="right nowrap">%s</td>
                  <td class="right nowrap">%s</td>
                  <td class="text-cell">%s</td>
                </tr>
                """.formatted(
                index + 1,
                escapeHtml(firstNonBlank(item.getDisplayName(), item.getMaterialName())),
                escapeHtml(firstNonBlank(item.getDisplayModel(), item.getModel())),
                escapeHtml(item.getBrand()),
                escapeHtml(item.getUnit()),
                escapeHtml(item.getQuantity() == null ? "" : trimTrailingZero(item.getQuantity())),
                item.getUnitPrice() == null ? "" : escapeHtml(formatUnitPrice(item.getUnitPrice())),
                escapeHtml(resolveAmountText(item)),
                escapeHtml(item.getRemark())
        );
    }

    private String resolveAmountText(WarehouseDocumentItem item) {
        Double amount = item.getAmount();
        if (amount == null && item.getQuantity() != null && item.getUnitPrice() != null) {
            amount = item.getQuantity() * item.getUnitPrice();
        }
        return amount == null ? "" : formatMoney(amount);
    }

    private Double resolveTotalAmount(WarehouseDocument warehouseDocument) {
        if (warehouseDocument.getTotalAmount() != null) {
            return warehouseDocument.getTotalAmount();
        }
        double total = 0D;
        if (warehouseDocument.getItems() != null) {
            for (WarehouseDocumentItem item : warehouseDocument.getItems()) {
                Double amount = item.getAmount();
                if (amount == null && item.getQuantity() != null && item.getUnitPrice() != null) {
                    amount = item.getQuantity() * item.getUnitPrice();
                }
                total += amount == null ? 0D : amount;
            }
        }
        return total;
    }

    private String trimTrailingZero(Double value) {
        if (value == null) {
            return "";
        }
        double normalized = value;
        if (Math.abs(normalized - Math.rint(normalized)) < 0.0000001D) {
            return String.valueOf((long) Math.rint(normalized));
        }
        return value.toString();
    }

    private String formatUnitPrice(Double value) {
        if (value == null) {
            return "";
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
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

    private String spacedTitle(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        label.codePoints().forEach(codePoint -> {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toChars(codePoint));
        });
        return builder.toString();
    }
}
