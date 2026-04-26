function escapeCsvValue(value) {
  if (value == null) {
    return "";
  }

  const stringValue = String(value).replace(/\r?\n|\r/g, " ");
  if (/[",]/.test(stringValue)) {
    return `"${stringValue.replace(/"/g, '""')}"`;
  }
  return stringValue;
}

function escapeXml(value) {
  if (value == null) {
    return "";
  }

  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

function sanitizeWorksheetName(name) {
  return String(name || "Sheet1")
    .replace(/[\\/*?:[\]]/g, " ")
    .trim()
    .slice(0, 31) || "Sheet1";
}

function inferCellType(value) {
  if (value == null || value === "") {
    return "String";
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return "Number";
  }
  if (typeof value === "boolean") {
    return "String";
  }
  return "String";
}

function buildWorksheetXml(sheetName, headers, rows) {
  const headerCells = headers
    .map(
      (header) =>
        `<Cell ss:StyleID="header"><Data ss:Type="String">${escapeXml(header)}</Data></Cell>`
    )
    .join("");

  const dataRows = rows
    .map((row) => {
      const cells = row
        .map((cell) => {
          const type = inferCellType(cell);
          const value =
            type === "Number" ? Number(cell) : escapeXml(cell == null ? "" : cell);
          return `<Cell><Data ss:Type="${type}">${value}</Data></Cell>`;
        })
        .join("");

      return `<Row>${cells}</Row>`;
    })
    .join("");

  return `
    <Worksheet ss:Name="${escapeXml(sanitizeWorksheetName(sheetName))}">
      <Table>
        <Row>${headerCells}</Row>
        ${dataRows}
      </Table>
    </Worksheet>
  `;
}

function triggerDownload(filename, content, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

export function downloadBlob(filename, blob, mimeType = "application/octet-stream") {
  const resolvedBlob = blob instanceof Blob ? blob : new Blob([blob], { type: mimeType });
  const url = window.URL.createObjectURL(resolvedBlob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

export function extractFilenameFromHeaders(headers, fallback = "download") {
  const disposition = headers?.["content-disposition"] || headers?.["Content-Disposition"];
  if (!disposition) {
    return fallback;
  }
  const utf8Match = disposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    const encodedValue = utf8Match[1].trim().replace(/^"|"$/g, "").replace(/\+/g, "%20");
    try {
      return decodeURIComponent(encodedValue);
    } catch (error) {
      return encodedValue;
    }
  }
  const plainMatch = disposition.match(/filename\s*=\s*("?)([^";]+)\1/i);
  if (plainMatch?.[2]) {
    return plainMatch[2].trim();
  }
  return fallback;
}

export async function resolveBlobErrorMessage(error, fallback = "下载失败") {
  const blob = error?.response?.data;
  if (blob instanceof Blob) {
    try {
      const text = await blob.text();
      if (text) {
        try {
          const parsed = JSON.parse(text);
          if (parsed?.message) {
            return parsed.message;
          }
        } catch (parseError) {
          return text;
        }
      }
    } catch (readError) {
      return fallback;
    }
  }
  return error?.message || fallback;
}

export async function downloadApiFile(requestPromise, fallbackFilename, fallbackMimeType = "application/octet-stream") {
  const response = await requestPromise;
  const filename = extractFilenameFromHeaders(response?.headers, fallbackFilename);
  const blob =
    response?.data instanceof Blob ? response.data : new Blob([response?.data], { type: fallbackMimeType });
  downloadBlob(filename, blob, fallbackMimeType);
  return response;
}

export function downloadCsv(filename, headers, rows) {
  const csvLines = [
    headers.map((header) => escapeCsvValue(header)).join(","),
    ...rows.map((row) => row.map((cell) => escapeCsvValue(cell)).join(",")),
  ];

  triggerDownload(
    filename,
    ["\uFEFF", csvLines.join("\r\n")],
    "text/csv;charset=utf-8;"
  );
}

export function downloadExcel(filename, sheetName, headers, rows) {
  const workbookXml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook
  xmlns="urn:schemas-microsoft-com:office:spreadsheet"
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:x="urn:schemas-microsoft-com:office:excel"
  xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
  xmlns:html="http://www.w3.org/TR/REC-html40">
  <Styles>
    <Style ss:ID="header">
      <Font ss:Bold="1"/>
      <Interior ss:Color="#D9EAF7" ss:Pattern="Solid"/>
    </Style>
  </Styles>
  ${buildWorksheetXml(sheetName, headers, rows)}
</Workbook>`;

  const normalizedFilename = filename.toLowerCase().endsWith(".xls")
    ? filename
    : `${filename.replace(/\.[^.]+$/, "")}.xls`;

  triggerDownload(
    normalizedFilename,
    ["\uFEFF", workbookXml],
    "application/vnd.ms-excel;charset=utf-8;"
  );
}

export function downloadHtmlExcel(filename, html) {
  const normalizedFilename = filename.toLowerCase().endsWith(".xls")
    ? filename
    : `${filename.replace(/\.[^.]+$/, "")}.xls`;

  triggerDownload(
    normalizedFilename,
    ["\uFEFF", html],
    "application/vnd.ms-excel;charset=utf-8;"
  );
}
