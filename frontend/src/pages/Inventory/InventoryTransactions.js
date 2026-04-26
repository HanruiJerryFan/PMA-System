import React, { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import { Alert, Button, Tag } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { inventoryAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";

const BUSINESS_CATEGORY_OPTIONS = [
  { value: "PROJECT_PURCHASE", label: "项目采购" },
  { value: "PROJECT_SALES", label: "项目销售" },
  { value: "CENTRALIZED_PURCHASE", label: "集中采购" },
  { value: "WAREHOUSE_TRANSFER_TO_PROJECT", label: "仓库调拨到项目" },
  { value: "PROJECT_TRANSFER_TO_WAREHOUSE", label: "项目退回仓库" },
];

const TXN_TYPE_OPTIONS = [
  { value: "IN", label: "入库" },
  { value: "OUT", label: "出库" },
];

function normalizeResponseData(response) {
  return response?.data ?? response ?? [];
}

function getBusinessCategoryLabel(value) {
  return BUSINESS_CATEGORY_OPTIONS.find((item) => item.value === value)?.label || value || "-";
}

function getTxnTypeLabel(value) {
  return TXN_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || "-";
}

export default function InventoryTransactions() {
  const [transactions, setTransactions] = useState([]);
  const [projects, setProjects] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [loading, setLoading] = useState(false);

  const projectMap = useMemo(
    () =>
      Object.fromEntries(
        projects.map((item) => [item.uuid, `${item.projectNumber || ""} ${item.projectName || ""}`.trim()])
      ),
    [projects]
  );

  const warehouseMap = useMemo(
    () => Object.fromEntries(warehouses.map((item) => [String(item.id), item.warehouseName])),
    [warehouses]
  );

  const decoratedTransactions = useMemo(
    () =>
      transactions.map((item) => ({
        ...item,
        categoryLabel: getBusinessCategoryLabel(item.category),
        txnTypeLabel: getTxnTypeLabel(item.txnType),
        projectLabel: item.projectId ? projectMap[item.projectId] || item.projectId : "-",
        warehouseLabel: item.warehouseId != null ? warehouseMap[String(item.warehouseId)] || item.warehouseId : "-",
      })),
    [projectMap, transactions, warehouseMap]
  );

  const fetchData = async () => {
    setLoading(true);
    try {
      const [transactionResponse, projectResponse, warehouseResponse] = await Promise.all([
        inventoryAPI.getTransactions(),
        inventoryAPI.getWarehouseDocProjectOptions(),
        inventoryAPI.getWarehouses(),
      ]);

      setTransactions(normalizeResponseData(transactionResponse));
      setProjects(normalizeResponseData(projectResponse));
      setWarehouses(normalizeResponseData(warehouseResponse));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const exportProjectOutboundSummary = (rows = decoratedTransactions) => {
    const outboundRows = rows.filter((item) => item.txnType === "OUT" && item.projectId);
    downloadExcel(
      `project-material-outbound-summary-${dayjs().format("YYYY-MM-DD")}.xls`,
      "项目物料出库汇总明细表",
      ["项目", "物料编码", "物料名称", "型号", "品牌", "业务类别", "仓库", "业务时间", "数量", "单价", "金额", "来源类型", "来源 ID"],
      outboundRows.map((item) => [
        item.projectLabel || "",
        item.materialCode || "",
        item.materialName || "",
        item.model || "",
        item.brand || "",
        item.categoryLabel || "",
        item.warehouseLabel || "",
        item.txnTime ? dayjs(item.txnTime).format("YYYY-MM-DD HH:mm:ss") : "",
        item.quantity ?? "",
        item.unitPrice ?? "",
        item.amount ?? "",
        item.sourceRefType || "",
        item.sourceRefId || "",
      ])
    );
  };

  return (
    <CRUDTable
      title="库存流水"
      columns={[
        { title: "物料编码", dataIndex: "materialCode", key: "materialCode", width: 140 },
        { title: "物料名称", dataIndex: "materialName", key: "materialName", width: 220 },
        { title: "型号", dataIndex: "model", key: "model", width: 180 },
        { title: "品牌", dataIndex: "brand", key: "brand", width: 140 },
        {
          title: "业务类别",
          dataIndex: "categoryLabel",
          key: "categoryLabel",
          width: 180,
          render: (value) => <Tag color="blue">{value}</Tag>,
        },
        {
          title: "出入库类型",
          dataIndex: "txnTypeLabel",
          key: "txnTypeLabel",
          width: 120,
          render: (value, record) => <Tag color={record.txnType === "IN" ? "green" : "orange"}>{value}</Tag>,
        },
        { title: "项目", dataIndex: "projectLabel", key: "projectLabel", width: 220 },
        { title: "仓库", dataIndex: "warehouseLabel", key: "warehouseLabel", width: 180 },
        { title: "数量", dataIndex: "quantity", key: "quantity", width: 100 },
        {
          title: "单价",
          dataIndex: "unitPrice",
          key: "unitPrice",
          width: 110,
          render: (value) => (value != null ? Number(value).toFixed(2) : "-"),
        },
        {
          title: "金额",
          dataIndex: "amount",
          key: "amount",
          width: 120,
          render: (value) => (value != null ? Number(value).toFixed(2) : "-"),
        },
        {
          title: "业务时间",
          dataIndex: "txnTime",
          key: "txnTime",
          width: 180,
          render: (value) => (value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "-"),
        },
        { title: "来源类型", dataIndex: "sourceRefType", key: "sourceRefType", width: 140 },
        { title: "来源 ID", dataIndex: "sourceRefId", key: "sourceRefId", width: 180 },
      ]}
      dataSource={decoratedTransactions}
      loading={loading}
      rowKey="uuid"
      extraActions={({ filteredData, canExport }) => (
        <>
          <Alert
            type="info"
            showIcon
            message="库存流水由出入库单自动生成，当前页面仅提供查询和导出。"
            style={{ marginRight: 8 }}
          />
          <Button
            icon={<DownloadOutlined />}
            onClick={() => exportProjectOutboundSummary(filteredData)}
            disabled={!canExport || !filteredData.length}
          >
            导出项目物料出库汇总明细表
          </Button>
        </>
      )}
      searchFields={[
        { name: "materialCode", label: "物料编码" },
        { name: "materialName", label: "物料名称" },
        { name: "categoryLabel", label: "业务类别" },
        { name: "projectLabel", label: "项目" },
        { name: "warehouseLabel", label: "仓库" },
        { name: "sourceRefId", label: "单据编号" },
      ]}
      enableView={false}
      showActions={false}
      createAuthorities={["inventory.manage"]}
      updateAuthorities={["inventory.manage"]}
      deleteAuthorities={["inventory.manage"]}
    />
  );
}
