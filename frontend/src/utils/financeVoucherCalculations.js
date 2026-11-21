export const DIRECTION_OPTIONS = [
  { value: "RECEIVE", label: "收款" },
  { value: "PAY", label: "付款" },
];

export const INVOICE_STATUS_OPTIONS = [
  { value: "NOT_INVOICED", label: "未开票" },
  { value: "INVOICED", label: "已开票" },
];

export const INVOICE_TYPE_OPTIONS = [
  { value: "VAT_ORDINARY", label: "增值税普通发票" },
  { value: "VAT_SPECIAL", label: "增值税专用发票" },
];

export const FIXED_THIRTEEN_PERCENT_EXPENSE_SUBJECTS = ["SALARY", "TRAVEL"];

function hasProvidedValue(value) {
  return value !== undefined && value !== null && value !== "";
}

export function normalizeDirectionalAmounts(values = {}) {
  const direction = values.transactionDirection;
  const normalizedIncome =
    direction === "RECEIVE" && hasProvidedValue(values.actualIncomeAmount)
      ? Number(values.actualIncomeAmount)
      : null;
  const normalizedExpense =
    direction === "PAY" && hasProvidedValue(values.actualExpenseAmount)
      ? Number(values.actualExpenseAmount)
      : null;

  return {
    actualIncomeAmount: normalizedIncome,
    actualExpenseAmount: normalizedExpense,
  };
}

export function validateDirectionalAmounts(values = {}) {
  if (values.transactionDirection === "RECEIVE") {
    if (!hasProvidedValue(values.actualIncomeAmount)) {
      throw new Error("收款方向必须填写实际收入");
    }
    const income = Number(values.actualIncomeAmount);
    if (!Number.isFinite(income)) {
      throw new Error("实际收入必须是有效数字");
    }
    if (income < 0) {
      throw new Error("实际收入不能为负数");
    }
  }

  if (values.transactionDirection === "PAY") {
    if (!hasProvidedValue(values.actualExpenseAmount)) {
      throw new Error("付款方向必须填写实际支出");
    }
    const expense = Number(values.actualExpenseAmount);
    if (!Number.isFinite(expense)) {
      throw new Error("实际支出必须是有效数字");
    }
    if (expense < 0) {
      throw new Error("实际支出不能为负数");
    }
  }
}

export function calculateBookedAmountPreview(values = {}) {
  if (!hasProvidedValue(values.taxRate)) {
    return null;
  }
  const taxRate = Number(values.taxRate);
  if (!Number.isFinite(taxRate)) {
    return null;
  }

  if (values.transactionDirection === "RECEIVE") {
    if (!hasProvidedValue(values.actualIncomeAmount)) {
      return null;
    }
    const income = Number(values.actualIncomeAmount);
    if (!Number.isFinite(income) || income < 0) {
      return null;
    }
    if (income === 0) {
      return 0;
    }
    const denominator =
      taxRate === 0
        ? 1 - (values.invoiceStatus === "INVOICED" ? 0.115 : 0.18)
        : 1 - (0.13 - taxRate);
    if (!Number.isFinite(denominator) || denominator === 0) {
      return null;
    }
    return Number((income / denominator).toFixed(2));
  }

  if (values.transactionDirection === "PAY") {
    if (!hasProvidedValue(values.actualExpenseAmount)) {
      return null;
    }
    const expense = Number(values.actualExpenseAmount);
    if (!Number.isFinite(expense) || expense < 0) {
      return null;
    }
    if (expense === 0) {
      return 0;
    }

    let denominator;
    if (FIXED_THIRTEEN_PERCENT_EXPENSE_SUBJECTS.includes(values.level2SubjectCode)) {
      denominator = 1 - 0.13;
    } else if (taxRate === 0) {
      denominator = 1 - (values.invoiceStatus === "INVOICED" ? 0.115 : 0.18);
    } else {
      denominator = 1 - (0.13 - taxRate);
    }
    if (!Number.isFinite(denominator) || denominator === 0) {
      return null;
    }

    let bookedAmount = expense / denominator;
    if (values.level2SubjectCode === "PROJECT_TRANSFER_OUT") {
      bookedAmount = -bookedAmount;
    }
    return Number(bookedAmount.toFixed(2));
  }

  return null;
}

export function calculateReceivablePayableInfo(values = {}) {
  if (!["RECEIVE", "PAY"].includes(values?.transactionDirection)) {
    return { type: null, amount: null };
  }

  const actualAmount =
    values.transactionDirection === "RECEIVE"
      ? Number(values.actualIncomeAmount || 0)
      : Number(values.actualExpenseAmount || 0);

  if (!values.isCompleted) {
    if (!(actualAmount > 0)) {
      return { type: null, amount: null };
    }
    return {
      type: values.transactionDirection === "RECEIVE" ? "应收" : "应付",
      amount: Number(actualAmount.toFixed(2)),
    };
  }

  const invoiceAmount = !hasProvidedValue(values.invoiceAmount)
    ? null
    : Number(values.invoiceAmount);

  if (invoiceAmount == null) {
    return { type: null, amount: null };
  }

  if (values.transactionDirection === "RECEIVE") {
    if (actualAmount < invoiceAmount) {
      return {
        type: "应收",
        amount: Number((invoiceAmount - actualAmount).toFixed(2)),
      };
    }
    if (actualAmount > invoiceAmount) {
      return {
        type: "待开票",
        amount: Number((actualAmount - invoiceAmount).toFixed(2)),
      };
    }
  }

  if (values.transactionDirection === "PAY") {
    if (actualAmount < invoiceAmount) {
      return {
        type: "应付",
        amount: Number((invoiceAmount - actualAmount).toFixed(2)),
      };
    }
    if (actualAmount > invoiceAmount) {
      return {
        type: "缺票",
        amount: Number((actualAmount - invoiceAmount).toFixed(2)),
      };
    }
  }

  return { type: null, amount: null };
}

export function calculatePayableAmount(voucher = {}) {
  const info = calculateReceivablePayableInfo(voucher);
  return info.type === "应付" ? Number(info.amount || 0) : 0;
}
