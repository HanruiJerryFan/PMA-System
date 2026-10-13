import dayjs from "dayjs";
import { queryWarehouseDocuments } from "./warehouseDocumentQuery";

const docs = [
  {
    docNumber: "IN1", projectId: "p1", docType: "INBOUND_NOTE", docDate: "2026-10-01T00:00:00",
    totalAmount: 1000,
    items: [
      { materialCode: "M1", materialName: "设备", unit: "台", quantity: 2, amount: 200 },
      { materialCode: "M2", unit: "个", quantity: 8, amount: 800 },
    ],
  },
  {
    docNumber: "OUT1", projectId: "p1", docType: "OUTBOUND_NOTE", docDate: "2026-10-12T23:59:59",
    items: [{ materialCode: "M1", unit: "台", quantity: 1, amount: 150 }],
  },
  {
    docNumber: "IN2", projectId: "p2", docType: "INBOUND_NOTE", docDate: "2026-10-05T12:00:00",
    items: [{ materialCode: "M1", unit: "台", quantity: 5, amount: 500 }],
  },
  {
    docNumber: "IN3", projectId: "p1", docType: "INBOUND_NOTE", docDate: "2026-10-13T00:00:00",
    items: [{ materialCode: "M1", unit: "台", quantity: 3, amount: 300 }],
  },
];

test("combines project, direction, dates and material code without including unrelated lines", () => {
  const result = queryWarehouseDocuments(docs, {
    projectId: "p1", docType: "INBOUND_NOTE", materialCode: " m1 ",
    docDateRange: [dayjs("2026-10-01"), dayjs("2026-10-12")],
  });
  expect(result.documents.map((doc) => doc.docNumber)).toEqual(["IN1"]);
  expect(result.summary).toEqual({ total: 1, inbound: 1, outbound: 0, inboundQuantity: 2, outboundQuantity: 0, amount: 200 });
  expect(result.documents[0]).toMatchObject({ matchedQuantity: 2, matchedAmount: 200, totalAmount: 1000 });
  expect(result.documents[0].items).toBe(docs[0].items);
  expect(result.materials).toHaveLength(1);
  expect(result.materials[0]).toMatchObject({ materialCode: "M1", inboundQuantity: 2, inboundAmount: 200 });
});

test("includes both full boundary days and excludes missing or invalid dates when filtering", () => {
  const result = queryWarehouseDocuments([...docs,
    { ...docs[0], docNumber: "undated", docDate: null },
    { ...docs[0], docNumber: "invalid", docDate: "not-a-date" },
  ], { projectId: "p1", docDateRange: [dayjs("2026-10-01"), dayjs("2026-10-12")] });
  expect(result.documents.map((doc) => doc.docNumber)).toEqual(["IN1", "OUT1"]);
  expect(result.summary).toMatchObject({ total: 2, inbound: 1, outbound: 1, amount: 1150 });
});

test("outbound filtering and partial codes update document and material totals together", () => {
  const result = queryWarehouseDocuments(docs, { docType: "OUTBOUND_NOTE", materialCode: "m" });
  expect(result.summary).toMatchObject({ total: 1, inbound: 0, outbound: 1, inboundQuantity: 0, outboundQuantity: 1, amount: 150 });
  expect(result.materials[0]).toMatchObject({ documentCount: 1, inboundAmount: 0, outboundAmount: 150 });
});

test("counts each document once per material and keeps different units separate", () => {
  const result = queryWarehouseDocuments([{
    ...docs[0], items: [
      { materialCode: "M10", unit: "台", quantity: 1, amount: 10 },
      { materialCode: "M10", unit: "台", quantity: 2, amount: 20 },
      { materialCode: "M10", unit: "箱", quantity: 1, amount: 40 },
      { materialCode: "M2", unit: "台", quantity: 1, amount: 5 },
    ],
  }]);
  expect(result.materials.map((item) => item.materialCode)).toEqual(["M2", "M10", "M10"]);
  expect(result.materials.find((item) => item.materialCode === "M10" && item.unit === "台"))
    .toMatchObject({ documentCount: 1, inboundQuantity: 3, inboundAmount: 30 });
});

test("sums decimal quantities and money without binary floating point noise", () => {
  const result = queryWarehouseDocuments([{
    ...docs[0], items: [
      { materialCode: "M1", quantity: 0.1, unitPrice: 1 },
      { materialCode: "M1", quantity: 0.2, amount: 0.2 },
      { materialCode: "M1", quantity: 0.0001, amount: 0 },
    ],
  }]);
  expect(result.summary.inboundQuantity).toBe(0.3001);
  expect(result.summary.amount).toBe(0.3);
  expect(result.materials[0].inboundAmount).toBe(0.3);
});

test("clearing filters restores all documents and empty results have zero totals", () => {
  expect(queryWarehouseDocuments(docs, { projectId: "missing" }).summary)
    .toEqual({ total: 0, inbound: 0, outbound: 0, inboundQuantity: 0, outboundQuantity: 0, amount: 0 });
  const result = queryWarehouseDocuments(docs, {});
  expect(result.documents).toHaveLength(4);
  expect(result.summary.amount).toBe(1950);
  expect(result.summary.total).toBe(4);
  expect(docs[0]).not.toHaveProperty("matchedAmount");
});
