import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ConfigProvider, Input } from "antd";
import zhCN from "antd/locale/zh_CN";
import CRUDTable from "./CRUDTable";
import { useAuth } from "../../context/AuthContext";

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

jest.mock("../../context/AuthContext", () => ({
  useAuth: jest.fn(),
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
  beforeEach(() => {
    useAuth.mockReturnValue({
      currentUser: { id: 1, authorities: ["customer.manage"] },
    });
  });

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

  test("blocks duplicate submit clicks while create request is pending", async () => {
    let resolveCreate;
    const onCreate = jest.fn(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        })
    );

    renderCrudTable({
      dataSource: [],
      onCreate,
      formFields: [
        {
          name: "name",
          label: "名称",
          rules: [{ required: true, message: "请输入名称" }],
          component: <Input placeholder="请输入名称" />,
        },
      ],
    });

    fireEvent.click(screen.getByRole("button", { name: /新\s*增/ }));
    const nameInput = await screen.findByPlaceholderText("请输入名称");
    fireEvent.change(nameInput, { target: { value: "新客户" } });

    const submitButton = screen.getByRole("button", { name: /创\s*建/ });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledTimes(1);
    });
    expect(submitButton).toBeDisabled();

    fireEvent.click(submitButton);
    expect(onCreate).toHaveBeenCalledTimes(1);

    resolveCreate({});
    await waitFor(() => {
      expect(screen.queryByPlaceholderText("请输入名称")).not.toBeInTheDocument();
    });
  });

  test("keeps client-side search filter applied when dataSource updates and clears on reset", async () => {
    const { rerender } = renderCrudTable({
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

    rerender(
      <ConfigProvider locale={zhCN}>
        <CRUDTable
          title="测试表格"
          columns={[{ title: "名称", dataIndex: "name", key: "name" }]}
          searchFields={[{ name: "name", label: "名称" }]}
          rowKey="id"
          showActions={false}
          dataSource={[
            { id: 1, name: "Alpha Updated" },
            { id: 2, name: "Beta Updated" },
          ]}
        />
      </ConfigProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("Alpha Updated")).toBeInTheDocument();
      expect(screen.queryByText("Beta Updated")).not.toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /重\s*置/ }));

    await waitFor(() => {
      expect(screen.getByText("Alpha Updated")).toBeInTheDocument();
      expect(screen.getByText("Beta Updated")).toBeInTheDocument();
    });
  });
});
