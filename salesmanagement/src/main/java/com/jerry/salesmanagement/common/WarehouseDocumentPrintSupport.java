package com.jerry.salesmanagement.common;

import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared layout constants, document-type metadata, pagination, and formatting helpers
 * matching the Aisino APD-E05 continuous-form warehouse document template.
 */
public final class WarehouseDocumentPrintSupport {

    public static final String COMPANY_NAME_CONFIG_KEY = "warehouse.document.company_name";
    public static final String DEFAULT_COMPANY_NAME = "敦力（南京）科技有限公司";

    public static final int ITEMS_PER_PAGE = 10;
    public static final int ROWS_PER_PAGE = 19;
    public static final int COLUMN_COUNT = 12;

    /**
     * Exact POI 1/256-char column widths derived from 发货单（表单格式）.xlsx
     * (4.625, 12.625, 6.625, 12.625, 6.625, 8.625, 4.625, 6.625, 10.625, 10.625, 9.625, 5.625).
     */
    public static final int[] EXCEL_COLUMN_WIDTHS = {
            1184, 3232, 1696, 3232, 1696, 2208, 1184, 1696, 2720, 2720, 2464, 1440
    };

    /**
     * Exact millimetre widths (sum = 210.60mm at SimSun 11pt MDW = 8px @ 96dpi).
     */
    public static final float[] COLUMN_WIDTHS_MM = {
            9.79f, 26.72f, 14.02f, 26.72f, 14.02f, 18.26f, 9.79f, 14.02f, 22.49f, 22.49f, 20.37f, 11.91f
    };

    /**
     * Exact row heights in points per 19-row page (sum = 418pt = 147.46mm).
     */
    public static final float[] ROW_HEIGHTS_PT = {
            30f, 7f, 17f, 17f, 17f, 20f,
            25f, 25f, 25f, 25f, 25f, 25f, 25f, 25f, 25f, 25f,
            25f, 10f, 25f
    };

    public static final double EXCEL_MARGIN_LEFT = 0.0784722222222222d;
    public static final double EXCEL_MARGIN_RIGHT = 0.0784722222222222d;
    public static final double EXCEL_MARGIN_TOP = 0.0784722222222222d;
    public static final double EXCEL_MARGIN_BOTTOM = 0.826388888888889d;
    public static final double EXCEL_MARGIN_HEADER = 0.0d;
    public static final double EXCEL_MARGIN_FOOTER = 0.314583333333333d;

    private static final ThreadLocal<DecimalFormat> GROUPED_MONEY_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("#,##0.00");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    });

    private WarehouseDocumentPrintSupport() {
    }

    public record DocumentPrintMeta(
            String spacedTitle,
            String partyBlockLabel,
            String numberLabel,
            String dateLabel,
            String copyStubText,
            String footerText
    ) {
    }

    public record DocumentPage(
            int pageNumber,
            int totalPages,
            int startLineNo,
            List<WarehouseDocumentItem> rows,
            BigDecimal pageSubtotal,
            boolean lastPage
    ) {
    }

    public static String resolveCompanyName(SystemConfigService systemConfigService) {
        if (systemConfigService != null) {
            String configured = systemConfigService.getString(COMPANY_NAME_CONFIG_KEY, null);
            if (StringUtils.hasText(configured)) {
                return configured.trim();
            }
        }
        return DEFAULT_COMPANY_NAME;
    }

    public static DocumentPrintMeta resolveMeta(WarehouseDocument doc, String companyName) {
        String docType = doc == null ? null : doc.getDocType();
        String sourceRefType = doc == null ? null : doc.getSourceRefType();
        String safeCompany = StringUtils.hasText(companyName) ? companyName.trim() : DEFAULT_COMPANY_NAME;

        if ("OUTBOUND_NOTE".equals(docType)) {
            return new DocumentPrintMeta(
                    "发    货    单",
                    "收货公司\n收货地址\n及收货人",
                    "发货单编号",
                    "发货日期：",
                    "（白）\n\n收\n货\n单\n位\n\n（红）\n\n发\n货\n单\n位\n联\n\n（黄）\n\n记\n账\n联",
                    "发货单位：" + safeCompany + "      收货人及电话：              签收日期："
            );
        }

        if ("TRANSFER_JTX".equals(docType) || "TRANSFER_XTJ".equals(docType)) {
            String title = "TRANSFER_JTX".equals(docType) ? "划  拨  单  JTX" : "划  拨  单  XTJ";
            return new DocumentPrintMeta(
                    title,
                    "调入仓库\n仓库地址\n及联系人",
                    "划拨单编号",
                    "划拨日期：",
                    "（白）\n\n调\n出\n仓\n库\n\n（红）\n\n调\n入\n仓\n库\n\n（黄）\n\n记\n账\n联",
                    "调出单位：" + safeCompany + "      接收人及电话：              签收日期："
            );
        }

        boolean transferInbound = WarehouseDocumentRules.TRANSFER_SOURCE.equals(sourceRefType);
        String partyLabel = transferInbound
                ? "调出仓库\n仓库地址\n及联系人"
                : "发货公司\n发货地址\n及发货人";
        String footer = transferInbound
                ? "调入单位：" + safeCompany + "      验收人及电话：              签收日期："
                : "收货单位：" + safeCompany + "      验收人及电话：              入库日期：";
        return new DocumentPrintMeta(
                "入    库    单",
                partyLabel,
                "入库单编号",
                "入库日期：",
                "（白）\n\n存\n根\n联\n\n（红）\n\n仓\n库\n联\n\n（黄）\n\n记\n账\n联",
                footer
        );
    }

    public static List<DocumentPage> paginate(WarehouseDocument doc) {
        List<WarehouseDocumentItem> items = doc != null && doc.getItems() != null
                ? doc.getItems()
                : List.of();
        int totalPages = Math.max(1, (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        List<DocumentPage> pages = new ArrayList<>(totalPages);

        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            int startIdx = pageIndex * ITEMS_PER_PAGE;
            int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.size());
            List<WarehouseDocumentItem> rows = new ArrayList<>(ITEMS_PER_PAGE);
            BigDecimal pageSubtotal = BigDecimal.ZERO;

            for (int i = startIdx; i < endIdx; i++) {
                WarehouseDocumentItem item = items.get(i);
                rows.add(item);
                BigDecimal amount = resolveItemAmount(item);
                if (amount != null) {
                    pageSubtotal = pageSubtotal.add(amount);
                }
            }
            while (rows.size() < ITEMS_PER_PAGE) {
                rows.add(new WarehouseDocumentItem());
            }

            pages.add(new DocumentPage(
                    pageIndex + 1,
                    totalPages,
                    startIdx + 1,
                    rows,
                    pageSubtotal.setScale(2, RoundingMode.HALF_UP),
                    pageIndex == totalPages - 1
            ));
        }
        return pages;
    }

    public static BigDecimal resolveItemAmount(WarehouseDocumentItem item) {
        if (item == null) {
            return null;
        }
        BigDecimal amount = item.getAmount();
        if (amount == null && item.getQuantity() != null && item.getUnitPrice() != null) {
            amount = item.getQuantity().multiply(item.getUnitPrice());
        }
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal resolveTotalAmount(WarehouseDocument doc) {
        if (doc == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (doc.getTotalAmount() != null) {
            return doc.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        if (doc.getItems() != null) {
            for (WarehouseDocumentItem item : doc.getItems()) {
                BigDecimal amount = resolveItemAmount(item);
                if (amount != null) {
                    total = total.add(amount);
                }
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public static String resolveCounterpartyText(WarehouseDocument doc) {
        if (doc == null) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        if (StringUtils.hasText(doc.getCounterpartyName())) {
            lines.add(doc.getCounterpartyName().trim());
        }
        if (StringUtils.hasText(doc.getCounterpartyAddress())) {
            lines.add(doc.getCounterpartyAddress().trim());
        }
        if (StringUtils.hasText(doc.getCounterpartyContact())) {
            lines.add(doc.getCounterpartyContact().trim());
        }
        return String.join("\n", lines);
    }

    public static String formatQuantity(BigDecimal value) {
        if (value == null) {
            return "";
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    public static String formatUnitPrice(BigDecimal value) {
        if (value == null) {
            return "";
        }
        BigDecimal rounded = value.setScale(4, RoundingMode.HALF_UP);
        BigDecimal stripped = rounded.stripTrailingZeros();
        if (stripped.compareTo(BigDecimal.ZERO) == 0) {
            return "0.00";
        }
        if (stripped.scale() < 2) {
            return stripped.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        }
        return stripped.toPlainString();
    }

    public static String formatMoneyGrouped(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return GROUPED_MONEY_FORMAT.get().format(value.setScale(2, RoundingMode.HALF_UP));
    }

    public static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second == null ? "" : second;
    }

    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
