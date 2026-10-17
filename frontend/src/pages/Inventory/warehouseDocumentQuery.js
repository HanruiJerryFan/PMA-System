import dayjs from "dayjs";
import { sortSelectItems } from "../../utils/selectSorting";

const quantityUnits = (value) => Math.round(Number(value ?? 0) * 10000);
const amountCents = (item) => Math.round(Number(item.amount ?? (Number(item.unitPrice ?? 0) * Number(item.quantity ?? 0))) * 100);

export function queryWarehouseDocuments(documents, filters = {}) {
  const code = String(filters.materialCode ?? "").trim().toLowerCase();
  const [start, end] = filters.docDateRange || [];
  const startDate = start ? dayjs(start).startOf("day") : null;
  const endDate = end ? dayjs(end).endOf("day") : null;
  const matchedDocuments = [];
  const materials = new Map();
  const summary = { total: 0, inbound: 0, outbound: 0, inboundQuantity: 0, outboundQuantity: 0, inboundAmount: 0, outboundAmount: 0 };

  for (const doc of documents) {
    if (filters.projectId && doc.projectId !== filters.projectId) continue;
    if (filters.docType && doc.docType !== filters.docType) continue;
    if (startDate || endDate) {
      const date = doc.docDate ? dayjs(doc.docDate) : null;
      if (!date?.isValid() || (startDate && date.isBefore(startDate)) || (endDate && date.isAfter(endDate))) continue;
    }
    const items = (doc.items || []).filter((item) => !code || String(item.materialCode ?? "").toLowerCase().includes(code));
    if (code && !items.length) continue;

    const inbound = doc.docType === "INBOUND_NOTE";
    const direction = inbound ? "inbound" : "outbound";
    let quantity = 0;
    let amount = 0;
    for (const item of items) {
      const itemQuantity = quantityUnits(item.quantity);
      const itemAmount = amountCents(item);
      quantity += itemQuantity;
      amount += itemAmount;
      const key = JSON.stringify([item.materialCode || item.materialId || item.uuid, item.unit || ""]);
      if (!materials.has(key)) {
        materials.set(key, {
          key, materialCode: item.materialCode || "", materialName: item.materialName || item.displayName || "",
          model: item.model || item.displayModel || "", brand: item.brand || "", unit: item.unit || "",
          docNumbers: new Set(), inboundQuantity: 0, outboundQuantity: 0, inboundAmount: 0, outboundAmount: 0,
        });
      }
      const material = materials.get(key);
      material.docNumbers.add(doc.docNumber);
      material[`${direction}Quantity`] += itemQuantity;
      material[`${direction}Amount`] += itemAmount;
    }
    matchedDocuments.push({ ...doc, matchedItemCount: items.length, matchedQuantity: quantity / 10000, matchedAmount: amount / 100 });
    summary.total += 1;
    summary[direction] += 1;
    summary[`${direction}Quantity`] += quantity;
    summary[`${direction}Amount`] += amount;
  }

  return {
    documents: matchedDocuments,
    summary: {
      ...summary, inboundQuantity: summary.inboundQuantity / 10000,
      outboundQuantity: summary.outboundQuantity / 10000,
      inboundAmount: summary.inboundAmount / 100, outboundAmount: summary.outboundAmount / 100,
      amountDifference: (summary.outboundAmount - summary.inboundAmount) / 100,
    },
    materials: sortSelectItems([...materials.values()].map(({ docNumbers, ...item }) => ({
      ...item, documentCount: docNumbers.size,
      inboundQuantity: item.inboundQuantity / 10000, outboundQuantity: item.outboundQuantity / 10000,
      inboundAmount: item.inboundAmount / 100, outboundAmount: item.outboundAmount / 100,
    })), ["materialCode", "unit"]),
  };
}
