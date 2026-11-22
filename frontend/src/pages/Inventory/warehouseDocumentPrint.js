import dayjs from "dayjs";
import { toChineseUppercaseRmb } from "./warehouseDocumentUtils";

export const DEFAULT_COMPANY_NAME = "敦力（南京）科技有限公司";

export const ITEMS_PER_PAGE = 10;

export const PRINT_COL_WIDTHS_MM = [
  9.79, 26.72, 14.02, 26.72, 14.02, 18.26, 9.79, 14.02, 22.49, 22.49, 20.37, 11.91,
];

export const PRINT_COL_WIDTHS_PX = [
  37, 101, 53, 101, 53, 69, 37, 53, 85, 85, 77, 45,
];

export const PRINT_ROW_HEIGHTS_PT = [
  30, 7, 17, 17, 17, 20,
  25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
  25, 10, 25,
];

export function resolvePrintDocMeta(doc, companyName = DEFAULT_COMPANY_NAME) {
  const docType = doc?.docType;
  const sourceRefType = doc?.sourceRefType;
  const safeCompany = String(companyName || DEFAULT_COMPANY_NAME).trim() || DEFAULT_COMPANY_NAME;

  if (docType === "OUTBOUND_NOTE") {
    return {
      spacedTitle: "发    货    单",
      partyBlockLabel: "收货公司\n收货地址\n及收货人",
      numberLabel: "发货单编号",
      dateLabel: "发货日期：",
      copyStubText: "（白）\n\n收\n货\n单\n位\n\n（红）\n\n发\n货\n单\n位\n联\n\n（黄）\n\n记\n账\n联",
      footerText: `发货单位：${safeCompany}      收货人及电话：              签收日期：`,
    };
  }

  if (docType === "TRANSFER_JTX" || docType === "TRANSFER_XTJ") {
    return {
      spacedTitle: docType === "TRANSFER_JTX" ? "划  拨  单  JTX" : "划  拨  单  XTJ",
      partyBlockLabel: "调入仓库\n仓库地址\n及联系人",
      numberLabel: "划拨单编号",
      dateLabel: "划拨日期：",
      copyStubText: "（白）\n\n调\n出\n仓\n库\n\n（红）\n\n调\n入\n仓\n库\n\n（黄）\n\n记\n账\n联",
      footerText: `调出单位：${safeCompany}      接收人及电话：              签收日期：`,
    };
  }

  const transferInbound = sourceRefType === "WAREHOUSE_TRANSFER";
  return {
    spacedTitle: "入    库    单",
    partyBlockLabel: transferInbound ? "调出仓库\n仓库地址\n及联系人" : "发货公司\n发货地址\n及发货人",
    numberLabel: "入库单编号",
    dateLabel: "入库日期：",
    copyStubText: "（白）\n\n存\n根\n联\n\n（红）\n\n仓\n库\n联\n\n（黄）\n\n记\n账\n联",
    footerText: transferInbound
      ? `调入单位：${safeCompany}      验收人及电话：              签收日期：`
      : `收货单位：${safeCompany}      验收人及电话：              入库日期：`,
  };
}

export function resolvePrintItemAmount(item) {
  if (!item) {
    return null;
  }
  if (item.amount != null && item.amount !== "") {
    const parsed = Number(item.amount);
    return Number.isFinite(parsed) ? Math.round(parsed * 100) / 100 : null;
  }
  if (item.quantity != null && item.quantity !== "" && item.unitPrice != null && item.unitPrice !== "") {
    const product = Number(item.quantity) * Number(item.unitPrice);
    return Number.isFinite(product) ? Math.round(product * 100) / 100 : null;
  }
  return null;
}

export function resolvePrintDocTotalAmount(doc) {
  if (!doc) {
    return 0;
  }
  if (doc.totalAmount != null && doc.totalAmount !== "") {
    const parsed = Number(doc.totalAmount);
    if (Number.isFinite(parsed)) {
      return Math.round(parsed * 100) / 100;
    }
  }
  const items = Array.isArray(doc.items) ? doc.items : [];
  const total = items.reduce((sum, item) => sum + (resolvePrintItemAmount(item) || 0), 0);
  return Math.round(total * 100) / 100;
}

export function paginateWarehouseDocument(doc) {
  const items = Array.isArray(doc?.items) ? doc.items : [];
  const totalPages = Math.max(1, Math.ceil(items.length / ITEMS_PER_PAGE));
  const pages = [];

  for (let pageIndex = 0; pageIndex < totalPages; pageIndex += 1) {
    const startIdx = pageIndex * ITEMS_PER_PAGE;
    const endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.length);
    const slice = items.slice(startIdx, endIdx);
    const pageSubtotal = Math.round(
      slice.reduce((sum, item) => sum + (resolvePrintItemAmount(item) || 0), 0) * 100,
    ) / 100;
    const rows = [...slice];
    while (rows.length < ITEMS_PER_PAGE) {
      rows.push({});
    }
    pages.push({
      pageNumber: pageIndex + 1,
      totalPages,
      startLineNo: startIdx + 1,
      rows,
      pageSubtotal,
      lastPage: pageIndex === totalPages - 1,
    });
  }

  return pages;
}

export function formatPrintQuantity(value) {
  if (value == null || value === "") {
    return "";
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return "";
  }
  if (numeric === 0) {
    return "0";
  }
  return numeric.toFixed(4).replace(/\.?0+$/, "");
}

export function formatPrintUnitPrice(value) {
  if (value == null || value === "") {
    return "";
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return "";
  }
  return numeric.toFixed(4).replace(/(\.\d{2,}?)0+$/, "$1");
}

export function formatPrintAmount(value) {
  if (value == null || value === "") {
    return "";
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return "";
  }
  const rounded = Math.round(numeric * 100) / 100;
  const normalized = Object.is(rounded, -0) ? 0 : rounded;
  return normalized.toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export function formatPrintDate(docDate) {
  return docDate ? dayjs(docDate).format("YYYY年M月D日") : "";
}

export function formatCounterpartyLines(doc) {
  return [doc?.counterpartyName, doc?.counterpartyAddress, doc?.counterpartyContact]
    .map((value) => String(value ?? "").trim())
    .filter(Boolean);
}

export function buildWarehouseDocumentHtml(doc, { autoPrint = false, companyName = DEFAULT_COMPANY_NAME } = {}) {
  const meta = resolvePrintDocMeta(doc, companyName);
  const pages = paginateWarehouseDocument(doc);
  const colGroupHtml = `<colgroup>${PRINT_COL_WIDTHS_MM
    .map((width) => `<col style="width:${width}mm" />`)
    .join("")}</colgroup>`;
  const docTotal = resolvePrintDocTotalAmount(doc);

  const pagesHtml = pages
    .map((page) => {
      const displayTotal = page.lastPage ? docTotal : page.pageSubtotal;
      const totalLabel = page.lastPage ? "合计：" : "本页小计：";
      const pageClass = page.lastPage ? "doc-page" : "doc-page page-break";
      const pageIndicatorHtml =
        page.totalPages > 1
          ? `<td colspan="3" class="b-none page-num-cell">第 ${page.pageNumber} 页 / 共 ${page.totalPages} 页</td><td class="b-none"></td>`
          : `<td colspan="4" class="b-none"></td>`;
      const itemRowsHtml = page.rows
        .map((item, idx) => {
          const lineNo = page.startLineNo + idx;
          const amount = resolvePrintItemAmount(item);
          return `<tr class="r-item">
            <td class="b-all c-idx"><div class="clip-25">${lineNo}</div></td>
            <td colspan="2" class="b-tbr c-name"><div class="clip-25">${escapeHtml(String(item.displayName || item.materialName || "").trim())}</div></td>
            <td colspan="2" class="b-tbr c-model"><div class="clip-25">${escapeHtml(String(item.displayModel || item.model || "").trim())}</div></td>
            <td class="b-tbr c-brand"><div class="clip-25">${escapeHtml(String(item.brand || "").trim())}</div></td>
            <td class="b-tbr c-unit"><div class="clip-25">${escapeHtml(String(item.unit || "").trim())}</div></td>
            <td class="b-tbr c-qty"><div class="clip-25">${escapeHtml(formatPrintQuantity(item.quantity))}</div></td>
            <td class="b-tbr c-price"><div class="clip-25">${escapeHtml(formatPrintUnitPrice(item.unitPrice))}</div></td>
            <td class="b-tbr c-amt"><div class="clip-25">${escapeHtml(formatPrintAmount(amount))}</div></td>
            <td class="b-tbr c-remark"><div class="clip-25">${escapeHtml(String(item.remark || "").trim())}</div></td>
          </tr>`;
        })
        .join("");

      return `<div class="${pageClass}">
        <table class="form-grid">
          ${colGroupHtml}
          <tr class="r-1">
            <td colspan="4" class="b-none"></td>
            <td colspan="4" class="title-cell">${escapeHtml(meta.spacedTitle)}</td>
            ${pageIndicatorHtml}
          </tr>
          <tr class="r-2"><td colspan="12" class="b-none"></td></tr>
          <tr class="r-3-5">
            <td rowspan="3" class="b-all meta-label-bold"><div class="clip-51">${formatMultilineHtml("项目\n名称")}</div></td>
            <td rowspan="3" colspan="2" class="b-tbr meta-project"><div class="clip-51">${escapeHtml(String(doc?.projectName || "").trim())}</div></td>
            <td rowspan="3" class="b-all meta-label-bold"><div class="clip-51">${formatMultilineHtml(meta.partyBlockLabel)}</div></td>
            <td rowspan="3" colspan="4" class="b-tbr meta-party"><div class="clip-51">${formatCounterpartyLines(doc).map(escapeHtml).join("<br />")}</div></td>
            <td class="b-tbr meta-small-center"><div class="clip-16">${escapeHtml(meta.numberLabel)}</div></td>
            <td colspan="2" class="b-tbr meta-small-center"><div class="clip-16">${escapeHtml(String(doc?.docNumber || "").trim())}</div></td>
            <td rowspan="15" class="b-none copy-stub"><div class="clip-stub">${formatMultilineHtml(meta.copyStubText)}</div></td>
          </tr>
          <tr class="r-3-5">
            <td class="b-tbr meta-small-center"><div class="clip-16">合同编号：</div></td>
            <td colspan="2" class="b-tbr meta-small-center"><div class="clip-16">${escapeHtml(String(doc?.contractNumber || "").trim())}</div></td>
          </tr>
          <tr class="r-3-5">
            <td class="b-tbr meta-small-center"><div class="clip-16">${escapeHtml(meta.dateLabel)}</div></td>
            <td colspan="2" class="b-tbr meta-small-center"><div class="clip-16">${escapeHtml(formatPrintDate(doc?.docDate))}</div></td>
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
          ${itemRowsHtml}
          <tr class="r-17">
            <td colspan="2" class="b-tbl total-label">${escapeHtml(totalLabel)}</td>
            <td colspan="7" class="b-tbr total-upper">${escapeHtml(toChineseUppercaseRmb(displayTotal))}</td>
            <td colspan="2" class="b-tbr total-num">¥${escapeHtml(formatPrintAmount(displayTotal))}</td>
          </tr>
          <tr class="r-18"><td colspan="12" class="b-none"></td></tr>
          <tr class="r-19"><td colspan="11" class="b-none footer-cell">${escapeHtml(meta.footerText)}</td><td class="b-none"></td></tr>
        </table>
      </div>`;
    })
    .join("");

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(meta.spacedTitle.replace(/\s+/g, ""))}</title>
  <style>
    @page { size: 216mm 155mm; margin: 2mm 2.7mm 2mm 2.7mm; }
    * { box-sizing: border-box; }
    html, body {
      margin: 0;
      padding: 0;
      color: #000;
      font-family: "SimSun", "NSimSun", serif;
      background: #fff;
    }
    .doc-page { width: 210.6mm; margin: 0 auto; }
    .doc-page.page-break { page-break-after: always; }
    table.form-grid {
      width: 210.6mm;
      border-collapse: collapse;
      table-layout: fixed;
      margin: 0 auto;
    }
    td, th {
      padding: 0 2pt;
      vertical-align: middle;
      font-weight: 400;
      line-height: 1.15;
      word-break: break-all;
      overflow-wrap: break-word;
    }
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
    .meta-label-bold { text-align: center; font-size: 9pt; font-weight: 700; }
    .meta-project { text-align: left; font-size: 10pt; }
    .meta-party { text-align: left; font-size: 8.5pt; line-height: 1.2; }
    .meta-small-center { text-align: center; font-size: 9pt; }
    .copy-stub { text-align: center; vertical-align: middle; font-size: 9pt; line-height: 1.18; padding: 0; }
    .tbl-head { text-align: center; font-size: 9pt; font-weight: 400; }
    .c-idx { text-align: center; font-size: 9pt; }
    .c-name { text-align: left; font-size: 10pt; }
    .c-model { text-align: left; font-size: 10pt; }
    .c-brand { text-align: center; font-size: 9pt; }
    .c-unit { text-align: center; font-size: 10pt; }
    .c-qty { text-align: center; font-size: 10pt; }
    .c-price { text-align: center; font-size: 10pt; }
    .c-amt { text-align: right; font-size: 10pt; }
    .c-remark { text-align: left; font-size: 8pt; }
    .total-label { text-align: right; font-size: 11pt; font-weight: 700; padding-right: 2pt; }
    .total-upper { text-align: left; font-size: 11pt; font-weight: 700; padding-left: 2pt; }
    .total-num { text-align: center; font-size: 9pt; }
    .footer-cell { text-align: left; font-size: 11pt; font-weight: 700; white-space: pre; padding: 0; }
    .clip-25 { max-height: 23.5pt; overflow: hidden; }
    .clip-51 { max-height: 49pt; overflow: hidden; }
    .clip-16 { max-height: 15.5pt; overflow: hidden; }
    .clip-stub { max-height: 342pt; overflow: hidden; }
  </style>
</head>
<body>${pagesHtml}${autoPrint ? "<script>window.onload=function(){window.print();}</script>" : ""}</body>
</html>`;
}

function formatMultilineHtml(value) {
  return String(value ?? "")
    .split(/\r?\n/)
    .map(escapeHtml)
    .join("<br />");
}

function escapeHtml(value) {
  if (value == null) {
    return "";
  }
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
