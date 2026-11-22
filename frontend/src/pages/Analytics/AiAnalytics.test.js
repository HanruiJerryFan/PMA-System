import "@testing-library/jest-dom";
import { render, screen, waitFor } from "@testing-library/react";
import AiAnalytics from "./AiAnalytics";
import { aiAPI, projectAPI } from "../../api/modules";
import { useAuth } from "../../context/AuthContext";

const mockNavigate = jest.fn();

jest.mock(
  "react-router-dom",
  () => ({
    useNavigate: () => mockNavigate,
  }),
  { virtual: true }
);

jest.mock("../../api/modules", () => ({
  aiAPI: {
    getStatus: jest.fn(),
    generateBriefing: jest.fn(),
    ask: jest.fn(),
  },
  projectAPI: {
    getProjectOptions: jest.fn(),
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

describe("AiAnalytics", () => {
  test("renders rule-based status, authorized preset questions, and briefing risks", async () => {
    useAuth.mockReturnValue({
      currentUser: { id: 1, authorities: ["project.access"] },
    });
    aiAPI.getStatus.mockResolvedValue({
      data: {
        enabled: false,
        configured: false,
        mode: "RULE_BASED",
        authorizedScopes: ["PROJECT"],
      },
    });
    projectAPI.getProjectOptions.mockResolvedValue({
      data: [{ uuid: "p1", projectNumber: "AB-1001", projectName: "示范项目" }],
    });
    aiAPI.generateBriefing.mockResolvedValue({
      data: {
        mode: "RULE_BASED",
        fallbackUsed: false,
        authorizedScopes: ["PROJECT"],
        summary: "共 1 个项目超期。",
        highlights: ["项目范围共 1 个项目。"],
        risks: [
          {
            severity: "HIGH",
            category: "进度超期",
            title: "AB-1001 · 示范项目",
            detail: "已超期 5 天",
            targetRoute: "/project/dashboard?projectUuid=p1",
          },
        ],
        recommendations: ["优先处理超期项目。"],
        evidence: { authorizedScopes: ["PROJECT"] },
      },
    });

    render(<AiAnalytics />);

    await waitFor(() => {
      expect(screen.getByText("规则分析模式（无需外部 Key）")).toBeInTheDocument();
      expect(screen.getByText(/AB-1001 · 示范项目/)).toBeInTheDocument();
    });
    expect(screen.getByText("目前有哪些超期、滞留或质保临期项目需要优先处理？")).toBeInTheDocument();
    // Unauthorized finance preset must be filtered out
    expect(screen.queryByText(/90天以上应收账款是多少/)).not.toBeInTheDocument();
  });
});
