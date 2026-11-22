import React, { useCallback, useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Progress,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from "antd";
import {
  ArrowLeftOutlined,
  DownloadOutlined,
  EyeOutlined,
  ReconciliationOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { downloadExcel } from "../../utils/exporters";

const { Text, Title } = Typography;
const { Option } = Select;

const STATUS_META = {
  MATCHED: { label: "已齐套", color: "success" },
  PROCUREMENT_SHORTAGE: { label: "采购缺口", color: "error" },
  PENDING_SHIPMENT: { label: "待发货", color: "warning" },
  OVER_SHIPPED: { label: "超发预警", color: "magenta" },
  OVER_PROCURED: { label: "超采 / 富余", color: "cyan" },
  UNPLANNED: { label: "清单外采发", color: "purple" },
  NO_INVENTORY_PERMISSION: { label: "无库存权限", color: "default" },
};

function formatQty(value) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  return Number(value).toFixed(4).replace(/\.?0+$/, "");
}

function formatMoney(value) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  return Number(value).toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export default function ProjectMaterialReconciliation() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { currentUser } = useAuth();
  const selectedProjectUuid = searchParams.get("projectUuid") || "";

  const [loading, setLoading] = useState(false);
  const [overview, setOverview] = useState([]);
  const [detail, setDetail] = useState(null);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState(undefined);
  const canViewOverviewInventory = useMemo(
    () =>
      hasAnyAuthority(currentUser, ["inventory.access", "inventory.manage"]) &&
      !overview.some((row) => row.canViewInventory === false),
    [currentUser, overview]
  );

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const overviewRes = await projectAPI.getMaterialReconciliationSummary();
      const overviewRows = Array.isArray(overviewRes?.data)
        ? overviewRes.data
        : Array.isArray(overviewRes)
          ? overviewRes
          : [];
      setOverview(overviewRows);

      if (selectedProjectUuid) {
        const detailRes = await projectAPI.getMaterialReconciliation(selectedProjectUuid);
        setDetail(detailRes?.data ?? detailRes ?? null);
      } else {
        setDetail(null);
      }
    } catch (error) {
      setDetail(null);
      message.error(error?.message || "加载采发对账数据失败");
    } finally {
      setLoading(false);
    }
  }, [selectedProjectUuid]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredOverview = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) {
      return overview;
    }
    return overview.filter((item) =>
      [item.projectNumber, item.projectName, item.customerName].some((field) =>
        String(field || "").toLowerCase().includes(normalized)
      )
    );
  }, [keyword, overview]);

  const filteredItems = useMemo(() => {
    const items = detail?.items || [];
    const normalized = keyword.trim().toLowerCase();
    return items.filter((item) => {
      if (statusFilter && item.status !== statusFilter) {
        return false;
      }
      if (!normalized) {
        return true;
      }
      return [item.materialCode, item.itemName, item.model, item.brand].some((field) =>
        String(field || "").toLowerCase().includes(normalized)
      );
    });
  }, [detail?.items, keyword, statusFilter]);

  const overviewStats = useMemo(
    () =>
      overview.reduce(
        (acc, row) => {
          if (row.shortageLineCount > 0) acc.shortageProjects += 1;
          if (row.pendingShipmentLineCount > 0) acc.pendingShipmentProjects += 1;
          if (row.overShippedLineCount > 0) acc.overShippedProjects += 1;
          return acc;
        },
        { shortageProjects: 0, pendingShipmentProjects: 0, overShippedProjects: 0 }
      ),
    [overview]
  );

  const selectProject = (projectId) => {
    setKeyword("");
    setStatusFilter(undefined);
    if (projectId) {
      setSearchParams({ projectUuid: projectId });
    } else {
      setSearchParams({});
    }
  };

  const handleExportOverview = () => {
    downloadExcel(
      `material-reconciliation-overview-${dayjs().format("YYYY-MM-DD")}.xls`,
      "跨项目采发对账",
      ["项目编号", "项目名称", "客户", "物料行数", "采购缺口行数", "待发货行数", "超发行数", "计划销售额", "计划采购额", "实际供货额", "实际发货额", "发货履约率(%)"],
      filteredOverview.map((row) => [
        row.projectNumber || "",
        row.projectName || "",
        row.customerName || "",
        row.totalMaterialLines ?? 0,
        row.shortageLineCount ?? 0,
        row.pendingShipmentLineCount ?? 0,
        row.overShippedLineCount ?? 0,
        row.plannedSalesAmount ?? "",
        row.plannedProcurementAmount ?? "",
        row.actualSuppliedAmount ?? "",
        row.actualShippedAmount ?? "",
        row.fulfillmentRate ?? "",
      ])
    );
  };

  const handleExportDetail = () => {
    if (!detail) {
      return;
    }
    downloadExcel(
      `material-reconciliation-${detail.projectNumber || detail.projectId}-${dayjs().format("YYYY-MM-DD")}.xls`,
      "项目采发对账明细",
      ["物料代码", "物料名称", "型号", "品牌", "单位", "最终销售量", "最终采购量", "实际采购入库", "集采调入", "项目调出", "净供货量", "采购缺口", "已发货量", "待发货缺口", "未发库存", "对账状态"],
      filteredItems.map((item) => [
        item.materialCode || "",
        item.itemName || "",
        item.model || "",
        item.brand || "",
        item.unit || "",
        item.plannedSalesQty ?? 0,
        item.plannedProcurementQty ?? 0,
        item.purchasedQty ?? 0,
        item.transferInQty ?? 0,
        item.transferOutQty ?? 0,
        item.netSuppliedQty ?? 0,
        item.procurementGapQty ?? 0,
        item.shippedQty ?? 0,
        item.shipmentGapQty ?? 0,
        item.unshippedOnHandQty ?? 0,
        (STATUS_META[item.status] || { label: item.status }).label,
      ])
    );
  };

  const overviewColumns = [
    {
      title: "项目",
      key: "project",
      fixed: "left",
      width: 240,
      render: (_, record) => (
        <Button
          type="link"
          style={{ padding: 0, height: "auto", fontWeight: 600 }}
          onClick={() => selectProject(record.projectId)}
        >
          {record.projectNumber} · {record.projectName}
        </Button>
      ),
    },
    { title: "客户", dataIndex: "customerName", key: "customerName", width: 180, render: (v) => v || "-" },
    { title: "物料项", dataIndex: "totalMaterialLines", key: "totalMaterialLines", width: 90, align: "center" },
    {
      title: "采购缺口",
      dataIndex: "shortageLineCount",
      key: "shortageLineCount",
      width: 100,
      align: "center",
      sorter: (a, b) => (a.shortageLineCount || 0) - (b.shortageLineCount || 0),
      render: (count) => <Tag color={count > 0 ? "error" : "default"}>{count} 项</Tag>,
    },
    {
      title: "待发货",
      dataIndex: "pendingShipmentLineCount",
      key: "pendingShipmentLineCount",
      width: 100,
      align: "center",
      render: (count) => <Tag color={count > 0 ? "warning" : "default"}>{count} 项</Tag>,
    },
    {
      title: "超发预警",
      dataIndex: "overShippedLineCount",
      key: "overShippedLineCount",
      width: 100,
      align: "center",
      render: (count) => <Tag color={count > 0 ? "magenta" : "default"}>{count} 项</Tag>,
    },
    {
      title: "计划销售额",
      dataIndex: "plannedSalesAmount",
      key: "plannedSalesAmount",
      width: 130,
      align: "right",
      render: (v) => formatMoney(v),
    },
    {
      title: "计划采购额",
      dataIndex: "plannedProcurementAmount",
      key: "plannedProcurementAmount",
      width: 130,
      align: "right",
      render: (v) => formatMoney(v),
    },
    {
      title: "发货履约率",
      dataIndex: "fulfillmentRate",
      key: "fulfillmentRate",
      width: 140,
      render: (rate) =>
        rate != null ? (
          <Progress percent={Math.min(100, Math.max(0, Number(rate)))} size="small" format={() => `${Number(rate).toFixed(1)}%`} />
        ) : (
          "-"
        ),
    },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 170,
      render: (_, record) => (
        <Space>
          <Button size="small" type="primary" onClick={() => selectProject(record.projectId)}>
            物料对账
          </Button>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/project/dashboard?projectUuid=${record.projectId}`)}
          >
            总览
          </Button>
        </Space>
      ),
    },
  ];

  const detailColumns = [
    { title: "物料代码", dataIndex: "materialCode", key: "materialCode", fixed: "left", width: 140, render: (v) => v || "-" },
    { title: "货物名称", dataIndex: "itemName", key: "itemName", width: 180, render: (v) => v || "-" },
    { title: "型号", dataIndex: "model", key: "model", width: 140, render: (v) => v || "-" },
    { title: "品牌", dataIndex: "brand", key: "brand", width: 100, render: (v) => v || "-" },
    { title: "单位", dataIndex: "unit", key: "unit", width: 70, align: "center", render: (v) => v || "-" },
    { title: "最终销售量", dataIndex: "plannedSalesQty", key: "plannedSalesQty", width: 110, align: "right", render: formatQty },
    { title: "最终采购量", dataIndex: "plannedProcurementQty", key: "plannedProcurementQty", width: 110, align: "right", render: formatQty },
    { title: "采购入库", dataIndex: "purchasedQty", key: "purchasedQty", width: 100, align: "right", render: formatQty },
    { title: "集采调入", dataIndex: "transferInQty", key: "transferInQty", width: 100, align: "right", render: formatQty },
    { title: "项目调出", dataIndex: "transferOutQty", key: "transferOutQty", width: 100, align: "right", render: formatQty },
    { title: "净供货量", dataIndex: "netSuppliedQty", key: "netSuppliedQty", width: 100, align: "right", render: formatQty },
    {
      title: "采购缺口",
      dataIndex: "procurementGapQty",
      key: "procurementGapQty",
      width: 110,
      align: "right",
      render: (v) => <Text type={Number(v || 0) > 0 ? "danger" : undefined}>{formatQty(v)}</Text>,
    },
    { title: "已发货量", dataIndex: "shippedQty", key: "shippedQty", width: 100, align: "right", render: formatQty },
    {
      title: "待发缺口",
      dataIndex: "shipmentGapQty",
      key: "shipmentGapQty",
      width: 110,
      align: "right",
      render: (v) => (
        <Text type={Number(v || 0) < 0 ? "danger" : Number(v || 0) > 0 ? "warning" : undefined}>
          {formatQty(v)}
        </Text>
      ),
    },
    { title: "未发结余", dataIndex: "unshippedOnHandQty", key: "unshippedOnHandQty", width: 100, align: "right", render: formatQty },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      fixed: "right",
      width: 115,
      render: (status) => {
        const meta = STATUS_META[status] || { label: status, color: "default" };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space direction="vertical" size={4}>
              <Space>
                <ReconciliationOutlined style={{ fontSize: 20, color: "#1677ff" }} />
                <Title level={4} style={{ margin: 0 }}>
                  {detail ? `采发对账 · ${detail.projectNumber} ${detail.projectName}` : "项目清单与采发对账"}
                </Title>
              </Space>
              <Text type="secondary">
                自动对比项目“最终销售清单 / 最终采购清单”与实际出入库、集采划拨流水，识别采购缺口、待发货与超发异常。
              </Text>
            </Space>
          </Col>
          <Col>
            <Space wrap>
              <Select
                allowClear
                showSearch
                optionFilterProp="children"
                placeholder="切换项目查看明细（留空看全部）"
                style={{ width: 260 }}
                value={selectedProjectUuid || undefined}
                onChange={(value) => selectProject(value || "")}
              >
                {overview.map((item) => (
                  <Option key={item.projectId} value={item.projectId}>
                    {item.projectNumber} · {item.projectName}
                  </Option>
                ))}
              </Select>
              {detail ? (
                <Button icon={<ArrowLeftOutlined />} onClick={() => selectProject("")}>
                  返回项目汇总
                </Button>
              ) : null}
              <Button
                icon={<DownloadOutlined />}
                onClick={detail ? handleExportDetail : handleExportOverview}
                disabled={detail ? !filteredItems.length : !filteredOverview.length}
              >
                导出 Excel
              </Button>
              <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {detail ? (
        <>
          {!detail.canViewInventory ? (
            <Alert
              type="warning"
              showIcon
              message="当前账号缺少库存查看权限（inventory.access）"
              description="当前仅展示项目清单计划数量与金额；联系管理员分配库存查看权限后可对比实际采购入库、划拨与发货流水。"
            />
          ) : null}

          <Row gutter={[16, 16]}>
            <Col xs={12} sm={8} lg={4}>
              <Card size="small">
                <Statistic title="物料总行数" value={detail.totalMaterialLines || 0} />
              </Card>
            </Col>
            <Col xs={12} sm={8} lg={4}>
              <Card size="small">
                <Statistic
                  title="采购缺口行"
                  value={detail.shortageLineCount || 0}
                  valueStyle={{ color: (detail.shortageLineCount || 0) > 0 ? "#cf1322" : undefined }}
                />
              </Card>
            </Col>
            <Col xs={12} sm={8} lg={4}>
              <Card size="small">
                <Statistic
                  title="待发货行"
                  value={detail.pendingShipmentLineCount || 0}
                  valueStyle={{ color: (detail.pendingShipmentLineCount || 0) > 0 ? "#d46b08" : undefined }}
                />
              </Card>
            </Col>
            <Col xs={12} sm={8} lg={4}>
              <Card size="small">
                <Statistic title="计划销售额" value={Number(detail.plannedSalesAmount || 0)} precision={2} />
              </Card>
            </Col>
            <Col xs={12} sm={8} lg={4}>
              <Card size="small">
                <Statistic title="计划采购额" value={Number(detail.plannedProcurementAmount || 0)} precision={2} />
              </Card>
            </Col>
            <Col xs={12} sm={8} lg={4}>
              <Card size="small">
                <Statistic
                  title="发货履约率"
                  value={detail.fulfillmentRate != null ? Number(detail.fulfillmentRate) : "-"}
                  suffix={detail.fulfillmentRate != null ? "%" : ""}
                  precision={detail.fulfillmentRate != null ? 2 : 0}
                />
              </Card>
            </Col>
          </Row>

          <Card size="small">
            <Space wrap size={12}>
              <Input
                allowClear
                prefix={<SearchOutlined />}
                placeholder="搜索物料编码 / 名称 / 型号 / 品牌"
                style={{ width: 260 }}
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
              <Select
                allowClear
                placeholder="按对账状态筛选"
                style={{ width: 170 }}
                value={statusFilter}
                onChange={setStatusFilter}
              >
                {Object.entries(STATUS_META).map(([key, meta]) => (
                  <Option key={key} value={key}>
                    {meta.label}
                  </Option>
                ))}
              </Select>
              <Button onClick={() => navigate(`/project/dashboard?projectUuid=${detail.projectId}`)}>
                打开项目总览
              </Button>
            </Space>
          </Card>

          <Card>
            {loading ? (
              <div style={{ padding: 48, textAlign: "center" }}>
                <Spin size="large" />
              </div>
            ) : (
              <Table
                rowKey="materialKey"
                columns={detailColumns}
                dataSource={filteredItems}
                scroll={{ x: "max-content" }}
                pagination={{ defaultPageSize: 20, showSizeChanger: true }}
                locale={{ emptyText: "当前项目暂无清单或采发记录" }}
              />
            )}
          </Card>
        </>
      ) : (
        <>
          {!canViewOverviewInventory ? (
            <Alert
              type="warning"
              showIcon
              message="当前账号缺少库存查看权限（inventory.access）"
              description="当前仅展示项目清单计划数据；联系管理员分配库存查看权限后可对比实际采购入库、划拨与发货流水。"
            />
          ) : null}
          <Row gutter={[16, 16]}>
            <Col xs={12} sm={6}>
              <Card size="small">
                <Statistic title="项目总数" value={overview.length} />
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card size="small">
                <Statistic
                  title="存在采购缺口项目"
                  value={overviewStats.shortageProjects}
                  valueStyle={{ color: overviewStats.shortageProjects > 0 ? "#cf1322" : undefined }}
                />
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card size="small">
                <Statistic
                  title="待发货项目"
                  value={overviewStats.pendingShipmentProjects}
                  valueStyle={{ color: overviewStats.pendingShipmentProjects > 0 ? "#d46b08" : undefined }}
                />
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card size="small">
                <Statistic
                  title="超发预警项目"
                  value={overviewStats.overShippedProjects}
                  valueStyle={{ color: overviewStats.overShippedProjects > 0 ? "#c41d7f" : undefined }}
                />
              </Card>
            </Col>
          </Row>

          <Card size="small">
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索项目编号 / 项目名称 / 客户"
              style={{ width: 280 }}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </Card>

          <Card>
            {loading && !overview.length ? (
              <div style={{ padding: 48, textAlign: "center" }}>
                <Spin size="large" />
              </div>
            ) : filteredOverview.length === 0 ? (
              <Empty description="暂无对账数据" />
            ) : (
              <Table
                rowKey="projectId"
                columns={overviewColumns}
                dataSource={filteredOverview}
                scroll={{ x: "max-content" }}
                pagination={{ defaultPageSize: 15, showSizeChanger: true }}
              />
            )}
          </Card>
        </>
      )}
    </Space>
  );
}
