import "@testing-library/jest-dom";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import FinanceAnalytics from "./FinanceAnalytics";
import { financeAPI } from "../../api/modules";

const mockNavigate = jest.fn();

jest.mock(
  "react-router-dom",
  () => ({
    useNavigate: () => mockNavigate,
  }),
  { virtual: true }
);

jest.mock("../../api/modules", () => ({
  financeAPI: {
    getFinanceAnalyticsOverview: jest.fn(),
  },
}));

beforeAll(() => {
  window.matchMedia = (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
    dispatchEvent() {
      return false;
    },
  });
  global.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
});

describe("FinanceAnalytics", () => {
  test("renders profit KPIs, project P&L table, and receivable aging tab", async () => {
    financeAPI.getFinanceAnalyticsOverview.mockResolvedValue({
      data: {
        canViewContracts: true,
        totalContractAmount: 2000,
        totalActualIncome: 800,
        totalActualExpense: 600,
        totalGrossProfit: 200,
        overallGrossMarginRate: 25,
        nonProjectExpense: 100,
        totalReceivable: 700,
        totalPayable: 150,
        totalPendingInvoice: 0,
        totalMissingInvoice: 0,
        receivableAging: {
          bucket0To30: 400,
          bucket31To60: 0,
          bucket61To90: 0,
          bucketOver90: 300,
          total: 700,
        },
        payableAging: {
          bucket0To30: 0,
          bucket31To60: 150,
          bucket61To90: 0,
          bucketOver90: 0,
          total: 150,
        },
        projectProfits: [
          {
            projectId: "p1",
            projectNumber: "AB-3001",
            projectName: "利润测试项目",
            customerName: "客户A",
            currentStageName: "验收",
            contractAmount: 2000,
            actualIncomeAmount: 800,
            actualExpenseAmount: 600,
            grossProfit: 200,
            grossMarginRate: 25,
            bookedNetProfit: 200,
            receivableAmount: 700,
            payableAmount: 150,
            pendingInvoiceAmount: 0,
            missingInvoiceAmount: 0,
          },
        ],
        receivableByCounterparty: [
          {
            customerId: "c1",
            customerName: "客户A",
            bucket0To30: 400,
            bucket31To60: 0,
            bucket61To90: 0,
            bucketOver90: 300,
            total: 700,
            voucherCount: 2,
            oldestOccurredOn: "2026-03-01",
            maxAgingDays: 106,
          },
        ],
        payableByCounterparty: [],
      },
    });

    render(<FinanceAnalytics />);

    await waitFor(() => {
      expect(screen.getByText(/AB-3001 · 利润测试项目/)).toBeInTheDocument();
    });
    expect(screen.getByText("累计实收")).toBeInTheDocument();

    fireEvent.click(screen.getByText(/应收账龄分析/));
    await waitFor(() => {
      expect(screen.getByText("106 天")).toBeInTheDocument();
    });
  });
});
