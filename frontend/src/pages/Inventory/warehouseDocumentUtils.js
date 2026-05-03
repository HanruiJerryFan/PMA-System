import dayjs from "dayjs";

export const DOC_TYPES = {
  INBOUND_NOTE: {
    label: "入库单",
    tagColor: "green",
    numberLabel: "入库单编号",
    dateLabel: "入库日期",
    partyBlock: "发货公司及发货人",
    partyName: "发货公司",
    partyAddress: "发货地址",
    partyContact: "发货人及联系方式",
    placeholder: "例如：RKD202503001",
  },
  OUTBOUND_NOTE: {
    label: "发货单",
    tagColor: "blue",
    numberLabel: "发货单编号",
    dateLabel: "发货日期",
    partyBlock: "收货公司、收货地址及收货人",
    partyName: "收货公司",
    partyAddress: "收货地址",
    partyContact: "收货人及联系方式",
    placeholder: "例如：FHD202603006",
  },
};

export const BUSINESS_CATEGORIES = {
  PROJECT_PURCHASE: "项目采购",
  PROJECT_SALES: "项目销售",
  CENTRALIZED_PURCHASE: "集中采购",
  WAREHOUSE_TRANSFER_TO_PROJECT: "仓库调拨到项目",
  PROJECT_TRANSFER_TO_WAREHOUSE: "项目退回仓库",
};

export const CUSTOMER_TYPE_RULES = {
  INBOUND_NOTE: ["3", "4"],
  OUTBOUND_NOTE: ["2", "3", "4", "5"],
};

export const normalize = (response) => response?.data ?? response ?? [];
export const formatAmount = (value) => (value == null || value === "" ? "-" : Number(value).toFixed(2));
export const formatUnitPrice = (value) => (value == null || value === "" ? "-" : Number(value).toFixed(4));
export const docMeta = (docType) => DOC_TYPES[docType] || DOC_TYPES.INBOUND_NOTE;
export const categoryLabel = (value) => BUSINESS_CATEGORIES[value] || value || "-";
export const materialBrand = (item) => item?.brandName || item?.brand || item?.brandCode || item?.manufacturer || "-";

export function toChineseUppercaseRmb(value) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount) || amount === 0) {
    return "人民币零元整";
  }
  const digits = ["零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"];
  const smallUnits = ["", "拾", "佰", "仟"];
  const largeUnits = ["", "万", "亿", "万亿"];

  const formatInteger = (integerPart) => {
    if (integerPart === 0) {
      return "零";
    }
    let result = "";
    let sectionIndex = 0;
    let pendingZero = false;
    let num = integerPart;
    while (num > 0) {
      const section = num % 10000;
      if (section === 0) {
        if (result) {
          pendingZero = true;
        }
      } else {
        let sectionText = "";
        let sectionNum = section;
        let unitIndex = 0;
        let zeroInSection = false;
        while (sectionNum > 0) {
          const digit = sectionNum % 10;
          if (digit === 0) {
            if (sectionText && !zeroInSection) {
              sectionText = `零${sectionText}`;
              zeroInSection = true;
            }
          } else {
            sectionText = `${digits[digit]}${smallUnits[unitIndex]}${sectionText}`;
            zeroInSection = false;
          }
          unitIndex += 1;
          sectionNum = Math.floor(sectionNum / 10);
        }
        sectionText = sectionText.replace(/零+/g, "零").replace(/零$/g, "");
        if (pendingZero && result) {
          result = `零${result}`;
        }
        result = `${sectionText}${largeUnits[sectionIndex]}${result}`;
        pendingZero = section < 1000;
      }
      sectionIndex += 1;
      num = Math.floor(num / 10000);
    }
    return result
      .replace(/零+/g, "零")
      .replace(/零(万|亿)/g, "$1")
      .replace(/亿万/g, "亿")
      .replace(/零$/g, "");
  };

  const normalizedAmount = Math.round(amount * 100);
  const integerPart = Math.floor(normalizedAmount / 100);
  const fractionPart = normalizedAmount % 100;
  const jiao = Math.floor(fractionPart / 10);
  const fen = fractionPart % 10;

  let result = `人民币${formatInteger(integerPart)}元`;
  if (jiao === 0 && fen === 0) {
    return `${result}整`;
  }
  if (jiao > 0) {
    result += `${digits[jiao]}角`;
  } else if (fen > 0) {
    result += "零";
  }
  if (fen > 0) {
    result += `${digits[fen]}分`;
  }
  return result;
}

export function buildWarehouseDocumentFormValues(record, fallbackWarehouseId) {
  return {
    docNumber: record?.docNumber || undefined,
    docType: record?.docType || "INBOUND_NOTE",
    businessCategory: record?.businessCategory || "PROJECT_PURCHASE",
    projectId: record?.projectId || undefined,
    warehouseId: record?.warehouseId || fallbackWarehouseId || undefined,
    counterpartyCustomerId: record?.counterpartyCustomerId || undefined,
    counterpartyAddress: record?.counterpartyAddress || "",
    counterpartyContact: record?.counterpartyContact || "",
    contractNumber: record?.contractNumber || "",
    docDate: record?.docDate ? dayjs(record.docDate) : dayjs(),
    remark: record?.remark || "",
    items: record?.items?.length
      ? record.items.map((item) => ({
          materialId: item.materialId,
          displayName: item.displayName || item.materialName,
          displayModel: item.displayModel || item.model,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          remark: item.remark,
        }))
      : [
          {
            materialId: undefined,
            displayName: "",
            displayModel: "",
            quantity: undefined,
            unitPrice: undefined,
            remark: "",
          },
        ],
  };
}
