import "@testing-library/jest-dom";
import { render, screen, waitFor } from "@testing-library/react";
import ProjectTrackingBoard from "./ProjectTrackingBoard";
import { projectAPI } from "../../api/modules";
import { useAuth } from "../../context/AuthContext";
import {
  filterTrackingCards,
  groupCardsIntoSwimlanes,
} from "../../utils/trackingBoardHelpers";

const mockNavigate = jest.fn();

jest.mock(
  "react-router-dom",
  () => ({
    useNavigate: () => mockNavigate,
  }),
  { virtual: true }
);

jest.mock("../../api/modules", () => ({
  projectAPI: {
    getTrackingBoard: jest.fn(),
    createProjectStatusHistory: jest.fn(),
  },
}));

jest.mock("../../context/AuthContext", () => ({
  useAuth: jest.fn(),
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
    dispatchEvent() { return false; },
  });
  global.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
});

describe("ProjectTrackingBoard helpers and component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useAuth.mockReturnValue({
      currentUser: { id: 1, authorities: ["project.access", "project.manage"] },
    });
  });

  test("groups cards into unstarted and active stage swimlanes and filters by keyword and risk", () => {
    const stages = [
      { id: 1, stageCode: "1.1", description: "发现" },
      { id: 2, stageCode: "3.3", description: "供货" },
    ];
    const items = [
      { uuid: "p1", projectNumber: "AB-0001", projectName: "Alpha", currentStageId: null, managerId: 1, riskTags: ["OVERDUE"] },
      { uuid: "p2", projectNumber: "AB-0002", projectName: "Beta", currentStageId: 2, managerId: 2, riskTags: ["STALLED"] },
    ];

    const lanes = groupCardsIntoSwimlanes(stages, items);
    expect(lanes).toHaveLength(3);
    expect(lanes[0].description).toBe("未开始");
    expect(lanes[0].cards).toHaveLength(1);
    expect(lanes[2].cards[0].uuid).toBe("p2");

    const filtered = filterTrackingCards(items, { keyword: "beta", riskTag: "STALLED" });
    expect(filtered).toHaveLength(1);
    expect(filtered[0].uuid).toBe("p2");
  });

  test("renders permission alert when finance and contracts are hidden and renders kanban card", async () => {
    projectAPI.getTrackingBoard.mockResolvedValue({
      data: {
        stages: [{ id: 1, stageCode: "1.1", description: "发现" }],
        stalledDaysThreshold: 30,
        permissions: {
          canViewContracts: false,
          canViewFinance: false,
          canViewInventory: false,
          canManageProject: true,
        },
        summary: {
          totalProjects: 1,
          activeProjects: 1,
          overdueCount: 1,
          stalledCount: 0,
          warrantyExpiringCount: 0,
          pendingAuditProjectCount: 1,
        },
        items: [
          {
            uuid: "p1",
            projectNumber: "AB-0001",
            projectName: "测试看板项目",
            customerName: "客户甲",
            managerName: "张三",
            currentStageId: 1,
            currentStageCode: "1.1",
            currentStageName: "发现",
            stageStayDays: 12,
            nextStageId: 2,
            nextStageName: "植入",
            warrantyStatus: "NONE",
            totalPendingAuditCount: 2,
            riskTags: ["OVERDUE", "PENDING_AUDIT"],
          },
        ],
      },
    });

    render(<ProjectTrackingBoard />);

    await waitFor(() => {
      expect(screen.getByText(/AB-0001 · 测试看板项目/)).toBeInTheDocument();
    });
    expect(screen.getByText("当前账号部分关联模块未授权")).toBeInTheDocument();
    expect(screen.getAllByText("计划超期").length).toBeGreaterThanOrEqual(2);
  });
});
