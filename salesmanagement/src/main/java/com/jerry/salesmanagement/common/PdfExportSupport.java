package com.jerry.salesmanagement.common;

import com.jerry.salesmanagement.service.SystemConfigService;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import org.springframework.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public abstract class PdfExportSupport {

    private static final String PDF_FONT_PATH_CONFIG_KEY = "pdf.font.path";
    private static final String[] RMB_DIGITS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] RMB_SMALL_UNITS = {"", "拾", "佰", "仟"};
    private static final String[] RMB_LARGE_UNITS = {"", "万", "亿", "万亿"};
    private static final List<String> DEFAULT_FONT_CANDIDATES = List.of(
            "C:\\Windows\\Fonts\\msyh.ttc,0",
            "C:\\Windows\\Fonts\\msyh.ttf",
            "C:\\Windows\\Fonts\\simsun.ttc,0",
            "C:\\Windows\\Fonts\\simhei.ttf",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",
            "/usr/share/fonts/truetype/arphic/uming.ttc,0"
    );
    private static final DateTimeFormatter CHINESE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final ThreadLocal<DecimalFormat> MONEY_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("0.00");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    });

    private final SystemConfigService systemConfigService;
    private final String configuredFontPath;
    private volatile BaseFont cachedBaseFont;

    protected PdfExportSupport(SystemConfigService systemConfigService, String configuredFontPath) {
        this.systemConfigService = systemConfigService;
        this.configuredFontPath = configuredFontPath;
    }

    protected Font font(float size) {
        return new Font(resolveBaseFont(), size, Font.NORMAL);
    }

    protected Font boldFont(float size) {
        return new Font(resolveBaseFont(), size, Font.BOLD);
    }

    protected PdfPCell cell(String value, Font font, int horizontalAlignment) {
        return cell(value, font, horizontalAlignment, 1, 0f);
    }

    protected PdfPCell cell(String value, Font font, int horizontalAlignment, int colspan, float minimumHeight) {
        PdfPCell cell = new PdfPCell(new Phrase(safeText(value), font));
        cell.setHorizontalAlignment(horizontalAlignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setUseAscender(true);
        cell.setUseDescender(true);
        cell.setBorderWidth(1f);
        if (colspan > 1) {
            cell.setColspan(colspan);
        }
        if (minimumHeight > 0f) {
            cell.setMinimumHeight(minimumHeight);
        }
        return cell;
    }

    protected PdfPCell headerCell(String value, Font font) {
        PdfPCell cell = cell(value, font, Element.ALIGN_CENTER);
        cell.setBackgroundColor(new java.awt.Color(250, 250, 250));
        return cell;
    }

    protected PdfPCell borderlessCell(String value, Font font, int horizontalAlignment) {
        PdfPCell cell = cell(value, font, horizontalAlignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    protected String safeText(String value) {
        return value == null ? "" : value;
    }

    protected String formatMoney(Double value) {
        return value == null ? "" : MONEY_FORMAT.get().format(value);
    }

    protected String formatChineseDate(Date value) {
        if (value == null) {
            return "";
        }
        return value.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(CHINESE_DATE_FORMATTER);
    }

    protected String toChineseUppercaseRmb(Double value) {
        BigDecimal amount = BigDecimal.valueOf(value == null ? 0D : value).setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return "人民币零元整";
        }
        long normalizedAmount = amount.movePointRight(2).longValue();
        long integerPart = normalizedAmount / 100;
        int fractionPart = (int) (normalizedAmount % 100);
        int jiao = fractionPart / 10;
        int fen = fractionPart % 10;

        StringBuilder result = new StringBuilder("人民币").append(formatIntegerPart(integerPart)).append("元");
        if (jiao == 0 && fen == 0) {
            return result.append("整").toString();
        }
        if (jiao > 0) {
            result.append(RMB_DIGITS[jiao]).append("角");
        } else if (fen > 0) {
            result.append("零");
        }
        if (fen > 0) {
            result.append(RMB_DIGITS[fen]).append("分");
        }
        return result.toString();
    }

    private String formatIntegerPart(long integerPart) {
        if (integerPart == 0) {
            return "零";
        }
        StringBuilder result = new StringBuilder();
        int sectionIndex = 0;
        boolean pendingZero = false;
        long remaining = integerPart;
        while (remaining > 0) {
            int section = (int) (remaining % 10000);
            if (section == 0) {
                if (result.length() > 0) {
                    pendingZero = true;
                }
            } else {
                String sectionText = formatIntegerSection(section);
                if (pendingZero && result.length() > 0) {
                    result.insert(0, "零");
                }
                result.insert(0, RMB_LARGE_UNITS[sectionIndex]);
                result.insert(0, sectionText);
                pendingZero = section < 1000;
            }
            sectionIndex += 1;
            remaining = remaining / 10000;
        }
        return result.toString()
                .replaceAll("零+", "零")
                .replaceAll("零(万|亿)", "$1")
                .replace("亿万", "亿")
                .replaceAll("零$", "");
    }

    private String formatIntegerSection(int section) {
        StringBuilder sectionBuilder = new StringBuilder();
        boolean zeroInSection = false;
        int unitIndex = 0;
        int remaining = section;
        while (remaining > 0) {
            int digit = remaining % 10;
            if (digit == 0) {
                if (sectionBuilder.length() > 0 && !zeroInSection) {
                    sectionBuilder.insert(0, "零");
                    zeroInSection = true;
                }
            } else {
                sectionBuilder.insert(0, RMB_SMALL_UNITS[unitIndex]);
                sectionBuilder.insert(0, RMB_DIGITS[digit]);
                zeroInSection = false;
            }
            unitIndex += 1;
            remaining = remaining / 10;
        }
        return sectionBuilder.toString()
                .replaceAll("零+", "零")
                .replaceAll("零$", "");
    }

    private BaseFont resolveBaseFont() {
        BaseFont existing = cachedBaseFont;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (cachedBaseFont != null) {
                return cachedBaseFont;
            }
            String fontPath = resolvePdfFontPath();
            try {
                cachedBaseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return cachedBaseFont;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load PDF font from " + fontPath, e);
            }
        }
    }

    protected String resolvePdfFontPath() {
        String systemConfigValue = systemConfigService.getString(PDF_FONT_PATH_CONFIG_KEY, null);
        if (StringUtils.hasText(systemConfigValue) && fontFileExists(systemConfigValue)) {
            return systemConfigValue.trim();
        }
        if (StringUtils.hasText(configuredFontPath) && fontFileExists(configuredFontPath)) {
            return configuredFontPath.trim();
        }
        return DEFAULT_FONT_CANDIDATES.stream()
                .filter(this::fontFileExists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No usable PDF font found. Configure system_config[pdf.font.path] or app.pdf.font-path."
                ));
    }

    protected File resolvePdfFontFile() {
        return new File(resolvePdfFontPath().replaceFirst(",\\d+$", ""));
    }

    private boolean fontFileExists(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        String normalized = candidate.trim().replaceFirst(",\\d+$", "");
        return new File(normalized).exists();
    }
}
