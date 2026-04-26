package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.PdfExportSupport;
import com.jerry.salesmanagement.pojo.PdfTableColumn;
import com.jerry.salesmanagement.pojo.PdfTableExportRequest;
import com.jerry.salesmanagement.pojo.PdfTableKeyValue;
import com.jerry.salesmanagement.pojo.PdfTableSection;
import com.jerry.salesmanagement.service.SystemConfigService;
import com.jerry.salesmanagement.service.TablePdfExportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TablePdfExportServiceImpl extends PdfExportSupport implements TablePdfExportService {

    private static final DateTimeFormatter GENERATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TablePdfExportServiceImpl(
            SystemConfigService systemConfigService,
            @Value("${app.pdf.font-path:}") String configuredFontPath
    ) {
        super(systemConfigService, configuredFontPath);
    }

    @Override
    public byte[] exportTable(PdfTableExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("PDF export request is required");
        }
        List<PdfTableColumn> columns = safeColumns(request.getColumns());
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("PDF export columns are required");
        }

        Rectangle pageSize = "PORTRAIT".equalsIgnoreCase(request.getOrientation())
                ? PageSize.A4
                : PageSize.A4.rotate();
        Document document = new Document(pageSize, 18f, 18f, 18f, 18f);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            String titleText = StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : "导出数据";
            Paragraph title = new Paragraph(titleText, boldFont(18f));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6f);
            document.add(title);

            String subtitleText = StringUtils.hasText(request.getSubtitle())
                    ? request.getSubtitle().trim()
                    : "生成时间：" + LocalDateTime.now().format(GENERATED_AT_FORMATTER);
            Paragraph subtitle = new Paragraph(subtitleText, font(9f));
            subtitle.setAlignment(Element.ALIGN_RIGHT);
            subtitle.setSpacingAfter(10f);
            document.add(subtitle);

            List<PdfTableKeyValue> metadata = combineKeyValues(request.getMetadata(), request.getSummaries());
            if (!metadata.isEmpty()) {
                document.add(buildKeyValueTable(metadata));
            }

            document.add(buildDataTable(columns, safeRows(request.getRows())));

            for (PdfTableSection section : safeSections(request.getExtraSections())) {
                List<PdfTableColumn> sectionColumns = safeColumns(section.getColumns());
                if (sectionColumns.isEmpty()) {
                    continue;
                }
                Paragraph sectionTitle = new Paragraph(safeText(section.getTitle()), boldFont(12f));
                sectionTitle.setSpacingBefore(14f);
                sectionTitle.setSpacingAfter(6f);
                document.add(sectionTitle);
                document.add(buildDataTable(sectionColumns, safeRows(section.getRows())));
            }

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("Failed to export table PDF: " + e.getMessage(), e);
        }
    }

    private PdfPTable buildKeyValueTable(List<PdfTableKeyValue> values) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1f, 1f, 1f, 1f});
        table.setSpacingAfter(10f);
        Font keyValueFont = font(9f);
        for (PdfTableKeyValue item : values) {
            String text = safeText(item.getLabel()) + "：" + safeText(item.getValue());
            PdfPCell cell = cell(text, keyValueFont, Element.ALIGN_LEFT);
            cell.setPadding(5f);
            table.addCell(cell);
        }
        int remainder = values.size() % 4;
        if (remainder > 0) {
            for (int i = remainder; i < 4; i++) {
                PdfPCell emptyCell = cell("", keyValueFont, Element.ALIGN_LEFT);
                emptyCell.setPadding(5f);
                table.addCell(emptyCell);
            }
        }
        return table;
    }

    private PdfPTable buildDataTable(List<PdfTableColumn> columns, List<List<Object>> rows) throws Exception {
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100f);
        table.setWidths(resolveWidths(columns));
        Font headerFont = boldFont(resolveHeaderFontSize(columns.size()));
        Font bodyFont = font(resolveBodyFontSize(columns.size()));
        float padding = columns.size() > 14 ? 3f : columns.size() > 10 ? 4f : 5f;

        for (PdfTableColumn column : columns) {
            PdfPCell cell = headerCell(safeText(column.getHeader()), headerFont);
            cell.setPadding(padding);
            table.addCell(cell);
        }

        if (rows == null || rows.isEmpty()) {
            PdfPCell empty = cell("暂无数据", bodyFont, Element.ALIGN_CENTER, columns.size(), 24f);
            empty.setPadding(padding);
            table.addCell(empty);
            return table;
        }

        for (List<Object> row : rows) {
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                Object value = row != null && columnIndex < row.size() ? row.get(columnIndex) : "";
                PdfPCell cell = cell(stringify(value), bodyFont, resolveAlignment(columns.get(columnIndex)), 1, 0f);
                cell.setPadding(padding);
                table.addCell(cell);
            }
        }
        return table;
    }

    private float[] resolveWidths(List<PdfTableColumn> columns) {
        float[] widths = new float[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            Float width = columns.get(i).getWidth();
            widths[i] = width != null && width > 0 ? width : 1f;
        }
        return widths;
    }

    private int resolveAlignment(PdfTableColumn column) {
        String align = column.getAlign();
        if ("right".equalsIgnoreCase(align)) {
            return Element.ALIGN_RIGHT;
        }
        if ("center".equalsIgnoreCase(align)) {
            return Element.ALIGN_CENTER;
        }
        return Element.ALIGN_LEFT;
    }

    private float resolveHeaderFontSize(int columnCount) {
        if (columnCount > 16) {
            return 6.2f;
        }
        if (columnCount > 10) {
            return 7.5f;
        }
        return 9f;
    }

    private float resolveBodyFontSize(int columnCount) {
        if (columnCount > 16) {
            return 5.8f;
        }
        if (columnCount > 10) {
            return 7f;
        }
        return 8.5f;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private List<PdfTableColumn> safeColumns(List<PdfTableColumn> columns) {
        return columns == null ? List.of() : columns;
    }

    private List<List<Object>> safeRows(List<List<Object>> rows) {
        return rows == null ? List.of() : rows;
    }

    private List<PdfTableSection> safeSections(List<PdfTableSection> sections) {
        return sections == null ? List.of() : sections;
    }

    private List<PdfTableKeyValue> combineKeyValues(List<PdfTableKeyValue> metadata, List<PdfTableKeyValue> summaries) {
        List<PdfTableKeyValue> combined = new ArrayList<>();
        if (metadata != null) {
            combined.addAll(metadata);
        }
        if (summaries != null) {
            combined.addAll(summaries);
        }
        return combined;
    }
}
