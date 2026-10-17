import React, { act } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import WarehouseDocumentModal from "./WarehouseDocumentModal";

beforeAll(() => {
  window.matchMedia = window.matchMedia || (() => ({
    matches: false, addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {},
  }));
  window.ResizeObserver = class { observe() {} unobserve() {} disconnect() {} };
  const getComputedStyle = window.getComputedStyle;
  window.getComputedStyle = (element) => getComputedStyle(element);
});

const initialDocument = {
  docNumber: "FHD202610001", docType: "OUTBOUND_NOTE", businessCategory: "PROJECT_SALES",
  projectId: "p1", warehouseId: 1, counterpartyCustomerId: "c1", counterpartyName: "收货公司甲",
  counterpartyAddress: "旧地址", counterpartyContact: "旧联系人", docDate: "2026-10-17",
  items: [{ materialId: "m1", displayName: "设备", displayModel: "型号", quantity: 1, unitPrice: 100 }],
};

function renderEditor(document, onSubmit, customerTypeCode = "2") {
  return render(<ConfigProvider locale={zhCN}><WarehouseDocumentModal
    open initialDocument={document}
    defaultWarehouse={{ id: 1, warehouseName: "仓库" }} warehouses={[]}
    projects={[{ uuid: "p1", projectNumber: "P1", projectName: "项目甲" }]}
    materials={[{ uuid: "m1", materialCode: "M1", productName: "设备", productModel: "型号" }]}
    customerOptions={[
      { uuid: "c1", customerName: "收货公司甲", customerTypeCode, officeAddress: "旧地址" },
      { uuid: "c2", customerName: "收货公司乙", customerTypeCode, officeAddress: "乙地址" },
    ]}
    saving={false} onCancel={() => {}} onSubmit={onSubmit}
  /></ConfigProvider>);
}

test.each(["2", "5"])("receiver type %s remains selected when opening and reopening the editor", async (customerTypeCode) => {
  const onSubmit = jest.fn().mockResolvedValue(undefined);
  const editor = renderEditor(initialDocument, onSubmit, customerTypeCode);
  await waitFor(() => expect(screen.getByLabelText("收货公司").closest(".ant-select").textContent).toContain("收货公司甲"));
  fireEvent.mouseDown(screen.getByLabelText("收货公司"));
  const receiver = await screen.findByText("收货公司乙");
  await act(async () => { fireEvent.click(receiver); });
  await act(async () => {
    fireEvent.change(screen.getByLabelText("收货地址"), { target: { value: "新收货地址" } });
    fireEvent.change(screen.getByLabelText("收货人及联系方式"), { target: { value: "新联系人" } });
    fireEvent.click(screen.getByText(/确\s*定/, { selector: "button > span" }).closest("button"));
  });
  await waitFor(() => expect(onSubmit).toHaveBeenCalled());
  expect(onSubmit.mock.calls[0][0]).toMatchObject({
    counterpartyCustomerId: "c2", counterpartyAddress: "新收货地址", counterpartyContact: "新联系人", projectId: "p1",
  });
  const savedDocument = { ...initialDocument, ...onSubmit.mock.calls[0][0], counterpartyName: "收货公司乙" };
  editor.unmount();
  const reopenedSubmit = jest.fn().mockResolvedValue(undefined);
  renderEditor(savedDocument, reopenedSubmit, customerTypeCode);
  await waitFor(() => expect(screen.getByLabelText("收货公司").closest(".ant-select").textContent).toContain("收货公司乙"));
  expect(screen.getByLabelText("收货地址").value).toBe("新收货地址");
  expect(screen.getByLabelText("收货人及联系方式").value).toBe("新联系人");
  await act(async () => {
    fireEvent.click(screen.getByText(/确\s*定/, { selector: "button > span" }).closest("button"));
  });
  await waitFor(() => expect(reopenedSubmit).toHaveBeenCalled());
  expect(reopenedSubmit.mock.calls[0][0].counterpartyCustomerId).toBe("c2");
});

test("changing an outbound document to inbound still clears an incompatible receiver", async () => {
  renderEditor(initialDocument, jest.fn());
  await waitFor(() => expect(screen.getByLabelText("收货公司").closest(".ant-select").textContent).toContain("收货公司甲"));
  fireEvent.mouseDown(screen.getByLabelText("单据类型"));
  const inboundOption = await screen.findByText("入库单");
  await act(async () => { fireEvent.click(inboundOption); });
  await waitFor(() => expect(screen.getByLabelText("发货公司").closest(".ant-select").textContent).not.toContain("收货公司甲"));
});
