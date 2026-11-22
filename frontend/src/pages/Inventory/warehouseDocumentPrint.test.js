import {
  DEFAULT_COMPANY_NAME,
  PRINT_COL_WIDTHS_MM,
  buildWarehouseDocumentHtml,
  formatPrintAmount,
  formatPrintQuantity,
  formatPrintUnitPrice,
  paginateWarehouseDocument,
  resolvePrintDocMeta,
} from "./warehouseDocumentPrint";

describe("warehouseDocumentPrint", () => {
  const buildDoc = (overrides = {}, itemCount = 2) => ({
    docNumber: "FHD202610033",
    docType: "OUTBOUND_NOTE",
    contractNumber: "XS-2026-0088",
    projectName: "示范园区二期",
    projectLabel: "P-001 - 示范园区二期",
    counterpartyName: "测试客户公司",
    counterpartyAddress: "测试市示例路100号",
    counterpartyContact: "张三 13800000000",
    docDate: "2026-11-13",
    items: Array.from({ length: itemCount }, (_, idx) => ({
      displayName: `物料${idx + 1}`,
      displayModel: `SPEC-${idx + 1}`,
      brand: "测试品牌",
      unit: "套",
      quantity: idx + 1,
      unitPrice: 100,
      amount: (idx + 1) * 100,
      remark: "",
    })),
    totalAmount: (itemCount * (itemCount + 1) * 100) / 2,
    ...overrides,
  });

  it("renders compact needle-printer @page dimensions and 12 exact mm columns", () => {
    const html = buildWarehouseDocumentHtml(buildDoc());
    expect(html).toContain("@page { size: 216mm 155mm; margin: 2mm 2.7mm 2mm 2.7mm; }");
    PRINT_COL_WIDTHS_MM.forEach((width) => {
      expect(html).toContain(`<col style="width:${width}mm" />`);
    });
    expect(html).toContain("发    货    单");
    expect(html).toContain("示范园区二期");
    expect(html).not.toContain("P-001 - 示范园区二期");
    expect(html).toContain("XS-2026-0088");
    expect(html).toContain(DEFAULT_COMPANY_NAME);
    expect(html).toContain("（白）");
    expect(html).toContain("（红）");
    expect(html).toContain("（黄）");
    expect(html).not.toContain("第 1 页");
  });

  it("resolves doc metadata for inbound, transfer inbound, and transfer out", () => {
    const inbound = resolvePrintDocMeta({ docType: "INBOUND_NOTE" }, "示例公司A");
    expect(inbound.spacedTitle).toBe("入    库    单");
    expect(inbound.numberLabel).toBe("入库单编号");
    expect(inbound.footerText).toContain("收货单位：示例公司A");

    const transferInbound = resolvePrintDocMeta({
      docType: "INBOUND_NOTE",
      sourceRefType: "WAREHOUSE_TRANSFER",
    });
    expect(transferInbound.partyBlockLabel).toContain("调出仓库");
    expect(transferInbound.footerText).toContain("调入单位：");

    const jtx = resolvePrintDocMeta({ docType: "TRANSFER_JTX" });
    expect(jtx.spacedTitle).toBe("划  拨  单  JTX");
    expect(jtx.partyBlockLabel).toContain("调入仓库");
    expect(jtx.copyStubText).toContain("调");

    const xtj = resolvePrintDocMeta({ docType: "TRANSFER_XTJ" });
    expect(xtj.spacedTitle).toBe("划  拨  单  XTJ");
  });

  it("paginates items exceeding 10 rows with page subtotals and grand total", () => {
    const doc = buildDoc({}, 12);
    const pages = paginateWarehouseDocument(doc);
    expect(pages).toHaveLength(2);
    expect(pages[0].startLineNo).toBe(1);
    expect(pages[0].lastPage).toBe(false);
    expect(pages[0].pageSubtotal).toBe(5500);
    expect(pages[1].startLineNo).toBe(11);
    expect(pages[1].lastPage).toBe(true);
    expect(pages[1].pageSubtotal).toBe(2300);

    const html = buildWarehouseDocumentHtml(doc, { companyName: "自定义发货公司" });
    expect(html).toContain("第 1 页 / 共 2 页");
    expect(html).toContain("本页小计：");
    expect(html).toContain("第 2 页 / 共 2 页");
    expect(html).toContain("合计：");
    expect(html).toContain("自定义发货公司");
  });

  it("formats quantities, unit prices, and amounts without trailing noise", () => {
    expect(formatPrintQuantity("10.0000")).toBe("10");
    expect(formatPrintQuantity("2.5000")).toBe("2.5");
    expect(formatPrintQuantity(0)).toBe("0");
    expect(formatPrintQuantity(null)).toBe("");

    expect(formatPrintUnitPrice(100)).toBe("100.00");
    expect(formatPrintUnitPrice("12.3450")).toBe("12.345");
    expect(formatPrintUnitPrice("12.3456")).toBe("12.3456");
    expect(formatPrintUnitPrice(null)).toBe("");

    expect(formatPrintAmount(1234.5)).toBe("1,234.50");
    expect(formatPrintAmount(0)).toBe("0.00");
    expect(formatPrintAmount(null)).toBe("");
  });
});
