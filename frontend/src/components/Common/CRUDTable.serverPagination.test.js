import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import CRUDTable from "./CRUDTable";

jest.mock(
  "react-router-dom",
  () => ({
    useLocation: () => ({ pathname: "/customer/info" }),
  }),
  { virtual: true }
);

jest.mock("../../api/auth", () => ({
  getCurrentUser: jest.fn(),
}));

beforeAll(() => {
  window.matchMedia = window.matchMedia || (() => ({
    matches: false,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
  }));
  window.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
});

function renderCrudTable(props) {
  return render(
    <ConfigProvider locale={zhCN}>
      <CRUDTable
        title="测试表格"
        columns={[{ title: "名称", dataIndex: "name", key: "name" }]}
        searchFields={[{ name: "name", label: "名称" }]}
        rowKey="id"
        showActions={false}
        {...props}
      />
    </ConfigProvider>
  );
}

describe("CRUDTable pagination modes", () => {
  test("delegates search, reset, and page change in serverPagination mode without client filtering", async () => {
    const onSearch = jest.fn();
    const onReset = jest.fn();
    const onChange = jest.fn();

    renderCrudTable({
      dataSource: [
        { id: 1, name: "Server Row 1" },
        { id: 2, name: "Server Row 2" },
      ],
      serverPagination: {
        current: 1,
        pageSize: 2,
        total: 4,
        onSearch,
        onReset,
        onChange,
      },
    });

    expect(screen.getByText("Server Row 1")).toBeInTheDocument();
    expect(screen.getByText("共 4 条")).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText("搜索名称"), { target: { value: "Alpha" } });
    fireEvent.click(screen.getByRole("button", { name: /搜\s*索/ }));

    await waitFor(() => {
      expect(onSearch).toHaveBeenCalledWith({ name: "Alpha" });
    });
    expect(screen.getByText("Server Row 1")).toBeInTheDocument();

    fireEvent.click(screen.getByTitle("2"));
    expect(onChange).toHaveBeenCalledWith(2, 2);

    fireEvent.click(screen.getByRole("button", { name: /重\s*置/ }));
    await waitFor(() => {
      expect(onReset).toHaveBeenCalledTimes(1);
    });
  });

  test("preserves default client-side filtering when serverPagination is omitted", async () => {
    renderCrudTable({
      dataSource: [
        { id: 1, name: "Alpha" },
        { id: 2, name: "Beta" },
      ],
    });

    fireEvent.change(screen.getByPlaceholderText("搜索名称"), { target: { value: "Alpha" } });
    fireEvent.click(screen.getByRole("button", { name: /搜\s*索/ }));

    await waitFor(() => {
      expect(screen.getByText("Alpha")).toBeInTheDocument();
      expect(screen.queryByText("Beta")).not.toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /重\s*置/ }));

    await waitFor(() => {
      expect(screen.getByText("Alpha")).toBeInTheDocument();
      expect(screen.getByText("Beta")).toBeInTheDocument();
    });
  });
});
