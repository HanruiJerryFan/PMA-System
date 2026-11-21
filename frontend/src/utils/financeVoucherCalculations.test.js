import {
  calculateBookedAmountPreview,
  calculatePayableAmount,
  calculateReceivablePayableInfo,
  normalizeDirectionalAmounts,
  validateDirectionalAmounts,
} from "./financeVoucherCalculations";

describe("financeVoucherCalculations", () => {
  test("validateDirectionalAmounts allows zero and positive amounts while rejecting empty or negative", () => {
    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "RECEIVE", actualIncomeAmount: 0 })
    ).not.toThrow();
    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "RECEIVE", actualIncomeAmount: 120.5 })
    ).not.toThrow();
    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "PAY", actualExpenseAmount: 0 })
    ).not.toThrow();
    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "PAY", actualExpenseAmount: 88 })
    ).not.toThrow();

    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "RECEIVE", actualIncomeAmount: null })
    ).toThrow("收款方向必须填写实际收入");
    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "RECEIVE", actualIncomeAmount: "" })
    ).toThrow("收款方向必须填写实际收入");
    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "RECEIVE", actualIncomeAmount: -0.01 })
    ).toThrow("实际收入不能为负数");

    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "PAY", actualExpenseAmount: undefined })
    ).toThrow("付款方向必须填写实际支出");
    expect(() =>
      validateDirectionalAmounts({ transactionDirection: "PAY", actualExpenseAmount: -1 })
    ).toThrow("实际支出不能为负数");
  });

  test("normalizeDirectionalAmounts keeps only the active direction amount", () => {
    expect(
      normalizeDirectionalAmounts({
        transactionDirection: "RECEIVE",
        actualIncomeAmount: "0",
        actualExpenseAmount: 100,
      })
    ).toEqual({
      actualIncomeAmount: 0,
      actualExpenseAmount: null,
    });

    expect(
      normalizeDirectionalAmounts({
        transactionDirection: "PAY",
        actualIncomeAmount: 50,
        actualExpenseAmount: "200.5",
      })
    ).toEqual({
      actualIncomeAmount: null,
      actualExpenseAmount: 200.5,
    });
  });

  test("calculateBookedAmountPreview matches backend tax rate, subject, zero, and missing rules", () => {
    expect(
      calculateBookedAmountPreview({
        transactionDirection: "PAY",
        actualExpenseAmount: 87,
        taxRate: 0,
        invoiceStatus: "NOT_INVOICED",
        level2SubjectCode: "SALARY",
      })
    ).toBe(100);

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "PAY",
        actualExpenseAmount: 87,
        taxRate: 0.06,
        invoiceStatus: "INVOICED",
        level2SubjectCode: "TRAVEL",
      })
    ).toBe(100);

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "PAY",
        actualExpenseAmount: 82,
        taxRate: 0,
        invoiceStatus: "NOT_INVOICED",
        level2SubjectCode: "OTHER",
      })
    ).toBe(100);

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "PAY",
        actualExpenseAmount: 88.5,
        taxRate: 0,
        invoiceStatus: "INVOICED",
        level2SubjectCode: "OTHER",
      })
    ).toBe(100);

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "RECEIVE",
        actualIncomeAmount: 93,
        taxRate: 0.06,
        invoiceStatus: "INVOICED",
      })
    ).toBe(100);

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "PAY",
        actualExpenseAmount: 100,
        taxRate: 0.13,
        invoiceStatus: "INVOICED",
        level2SubjectCode: "PROJECT_TRANSFER_OUT",
      })
    ).toBe(-100);

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "PAY",
        actualExpenseAmount: 0,
        taxRate: 0.13,
        invoiceStatus: "INVOICED",
        level2SubjectCode: "PROJECT_TRANSFER_OUT",
      })
    ).toBe(0);

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "RECEIVE",
        actualIncomeAmount: null,
        taxRate: 0.13,
      })
    ).toBeNull();

    expect(
      calculateBookedAmountPreview({
        transactionDirection: "RECEIVE",
        actualIncomeAmount: 100,
        taxRate: "",
      })
    ).toBeNull();
  });

  test("calculateReceivablePayableInfo and calculatePayableAmount handle incomplete and completed gaps", () => {
    expect(
      calculateReceivablePayableInfo({
        transactionDirection: "RECEIVE",
        actualIncomeAmount: 120,
        isCompleted: false,
      })
    ).toEqual({ type: "应收", amount: 120 });

    const incompletePay = {
      transactionDirection: "PAY",
      actualExpenseAmount: 80,
      isCompleted: false,
    };
    expect(calculateReceivablePayableInfo(incompletePay)).toEqual({ type: "应付", amount: 80 });
    expect(calculatePayableAmount(incompletePay)).toBe(80);

    expect(
      calculateReceivablePayableInfo({
        transactionDirection: "RECEIVE",
        actualIncomeAmount: 100,
        invoiceAmount: 150,
        isCompleted: true,
      })
    ).toEqual({ type: "应收", amount: 50 });

    expect(
      calculateReceivablePayableInfo({
        transactionDirection: "RECEIVE",
        actualIncomeAmount: 200,
        invoiceAmount: 150,
        isCompleted: true,
      })
    ).toEqual({ type: "待开票", amount: 50 });

    const completedUnderpaid = {
      transactionDirection: "PAY",
      actualExpenseAmount: 70,
      invoiceAmount: 100,
      isCompleted: true,
    };
    expect(calculateReceivablePayableInfo(completedUnderpaid)).toEqual({ type: "应付", amount: 30 });
    expect(calculatePayableAmount(completedUnderpaid)).toBe(30);

    const completedMissingInvoice = {
      transactionDirection: "PAY",
      actualExpenseAmount: 120,
      invoiceAmount: 100,
      isCompleted: true,
    };
    expect(calculateReceivablePayableInfo(completedMissingInvoice)).toEqual({ type: "缺票", amount: 20 });
    expect(calculatePayableAmount(completedMissingInvoice)).toBe(0);
  });
});
