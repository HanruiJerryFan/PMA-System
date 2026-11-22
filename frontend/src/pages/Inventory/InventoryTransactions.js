import React, { useCallback, useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import { Alert, Button, Select, Tag, message } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { inventoryAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";
import { BUSINESS_CATEGORIES } from "./warehouseDocumentUtils";

const { Option } = Select;

const BUSINESS_CATEGORY_OPTIONS = [
  { value: "PROJECT_PURCHASE", label: "项目采购" },
  { value: "PROJECT_SALES", label: "项目销售" },
  { value: "CENTRALIZED_PURCHASE", label: "集中采购" },
  { value: "WAREHOUSE_TRANSFER_TO_PROJECT", label: BUSINESS_CATEGORIES.WAREHOUSE_TRANSFER_TO_PROJECT },
  { value: "PROJECT_TRANSFER_TO_WAREHOUSE", label: BUSINESS_CATEGORIES.PROJECT_TRANSFER_TO_WAREHOUSE },
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

function normalizeTextFilter(value) {
  return typeof value === "string" ? value.trim() || undefined : undefined;
}

export default function InventoryTransactions() {
  const [transactions, setTransactions] = useState([]);
  const [total, setTotal] = useState(0);
  const [projects, setProjects] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [query, setQuery] = useState({ page: 1, pageSize: 10, filters: {}, reloadKey: 0 });

  const projectMap = useMemo(
    () =>
      Object.fromEntries(
        projects.map((item) => [item.uuid, `${item.projectNumber || ""} ${item.projectName || ""}`.trim()])
      ),
    [projects]
  );

  const warehouseMap = useMemo(
    () => Object.fromEntries(warehouses.map((item) => [String(item.id), [item.warehouseCode, item.warehouseName].filter(Boolean).join(" - ")])),
    [warehouses]
  );

  const decorateRows = useCallback(
    (rows) =>
      rows.map((item) => ({
        ...item,
        categoryLabel: getBusinessCategoryLabel(item.category),
        txnTypeLabel: getTxnTypeLabel(item.txnType),
        projectLabel: item.projectId ? projectMap[item.projectId] || item.projectId : "-",
        warehouseLabel: item.warehouseId != null ? warehouseMap[String(item.warehouseId)] || item.warehouseId : "-",
      })),
    [projectMap, warehouseMap]
  );

  const decoratedTransactions = useMemo(() => decorateRows(transactions), [decorateRows, transactions]);

  const fetchTransactionsPage = useCallback(async ({ page, pageSize, filters }) => {
    setLoading(true);
    try {
      const params = { page, pageSize, ...filters };
      const response = await inventoryAPI.getTransactionsPage(params);
      const pageData = response?.data || {};
      const nextRecords = Array.isArray(pageData.records) ? pageData.records : [];
      const nextTotal = Number(pageData.total ?? 0);
      if (nextRecords.length === 0 && page > 1 && nextTotal > 0) {
        const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize));
        setQuery((prev) => ({ ...prev, page: lastPage }));
        return;
      }
      setTransactions(nextRecords);
      setTotal(nextTotal);
    } catch (error) {
      message.error(error?.message || "加载库存流水失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTransactionsPage(query);
  }, [fetchTransactionsPage, query]);

  useEffect(() => {
    async function fetchOptions() {
      try {
        const [projectResponse, warehouseResponse] = await Promise.all([
          inventoryAPI.getWarehouseDocProjectOptions(),
          inventoryAPI.getWarehouses(),
        ]);
        setProjects(normalizeResponseData(projectResponse));
        setWarehouses(normalizeResponseData(warehouseResponse));
      } catch (error) {
        message.error(error?.message || "加载筛选项失败");
      }
    }

    fetchOptions();
  }, []);

  const matchesActiveFilters = useCallback((item, filters) => {
    if (filters.materialCode && !String(item.materialCode || "").toLowerCase().includes(filters.materialCode.toLowerCase())) {
      return false;
    }
    if (filters.materialName && !String(item.materialName || "").toLowerCase().includes(filters.materialName.toLowerCase())) {
      return false;
    }
    if (filters.category && item.category !== filters.category) {
      return false;
    }
    if (filters.projectId && item.projectId !== filters.projectId) {
      return false;
    }
    if (filters.warehouseId != null && Number(item.warehouseId) !== Number(filters.warehouseId)) {
      return false;
    }
    if (filters.sourceRefId && !String(item.sourceRefId || "").toLowerCase().includes(filters.sourceRefId.toLowerCase())) {
      return false;
    }
    return true;
  }, []);

  const exportProjectOutboundSummary = async () => {
    setExporting(true);
    try {
      const response = await inventoryAPI.getTransactions();
      const allRows = decorateRows(normalizeResponseData(response));
      const outboundRows = allRows.filter(
        (item) =>
          item.txnType === "OUT" &&
          item.projectId &&
          item.category !== "WAREHOUSE_TRANSFER_TO_PROJECT" &&
          item.category !== "PROJECT_TRANSFER_TO_WAREHOUSE" &&
          matchesActiveFilters(item, query.filters || {})
      );
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
          item.unitPrice != null ? Number(item.unitPrice).toFixed(4) : "",
          item.amount ?? "",
          item.sourceRefType || "",
          item.sourceRefId || "",
        ])
      );
    } catch (error) {
      message.error(error?.message || "导出项目物料出库汇总失败");
    } finally {
      setExporting(false);
    }
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
          render: (value) => (value != null ? Number(value).toFixed(4) : "-"),
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
      serverPagination={{
        current: query.page,
        pageSize: query.pageSize,
        total,
        onChange: (nextPage, nextPageSize) => setQuery((prev) => ({ ...prev, page: nextPage, pageSize: nextPageSize })),
        onSearch: (values) => {
          setQuery((prev) => ({
            ...prev,
            page: 1,
            filters: {
              materialCode: normalizeTextFilter(values.materialCode),
              materialName: normalizeTextFilter(values.materialName),
              category: values.category || undefined,
              projectId: values.projectId || undefined,
              warehouseId: values.warehouseId != null ? Number(values.warehouseId) : undefined,
              sourceRefId: normalizeTextFilter(values.sourceRefId),
            },
          }));
        },
        onReset: () => setQuery((prev) => ({ ...prev, page: 1, filters: {} })),
      }}
      extraActions={({ canExport }) => (
        <>
          <Alert
            type="info"
            showIcon
            message="库存流水由出入库单自动生成，当前页面仅提供查询和导出。"
            style={{ marginRight: 8 }}
          />
          <Button
            icon={<DownloadOutlined />}
            loading={exporting}
            onClick={exportProjectOutboundSummary}
            disabled={!canExport || total === 0}
          >
            导出项目物料出库汇总明细表
          </Button>
        </>
      )}
      searchFields={[
        { name: "materialCode", label: "物料编码" },
        { name: "materialName", label: "物料名称" },
        {
          name: "category",
          label: "业务类别",
          component: (
            <Select placeholder="请选择业务类别" allowClear style={{ width: 180 }}>
              {BUSINESS_CATEGORY_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "projectId",
          label: "项目",
          component: (
            <Select placeholder="请选择项目" allowClear showSearch optionFilterProp="children" style={{ width: 200 }}>
              {projects.map((item) => (
                <Option key={item.uuid} value={item.uuid}>
                  {`${item.projectNumber || ""} ${item.projectName || ""}`.trim()}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "warehouseId",
          label: "仓库",
          component: (
            <Select placeholder="请选择仓库" allowClear style={{ width: 180 }}>
              {warehouses.map((item) => (
                <Option key={item.id} value={item.id}>
                  {[item.warehouseCode, item.warehouseName].filter(Boolean).join(" - ")}
                </Option>
              ))}
            </Select>
          ),
        },
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
