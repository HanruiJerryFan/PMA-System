import React, { useCallback, useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  Button,
  Card,
  Col,
  Empty,
  Input,
  Progress,
  Row,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from "antd";
import {
  BarChartOutlined,
  DownloadOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { financeAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";

const { Text, Title } = Typography;

function formatMoney(value) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  return Number(value).toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatPercent(value) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  return `${Number(value).toFixed(2)}%`;
}

function renderAgingCards(buckets = {}, accentColor = "#1677ff") {
  const total = Number(buckets.total || 0);
  const items = [
    { key: "0-30天", value: Number(buckets.bucket0To30 || 0), color: "#52c41a" },
    { key: "31-60天", value: Number(buckets.bucket31To60 || 0), color: "#1677ff" },
    { key: "61-90天", value: Number(buckets.bucket61To90 || 0), color: "#faad14" },
    { key: "90天以上", value: Number(buckets.bucketOver90 || 0), color: "#cf1322" },
  ];
  return (
    <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
      {items.map((item) => {
        const percent = total > 0 ? Math.round((item.value / total) * 100) : 0;
        return (
          <Col xs={12} sm={6} key={item.key}>
            <Card size="small">
              <Statistic
                title={item.key}
                value={item.value}
                precision={2}
                valueStyle={{ color: item.value > 0 ? item.color : accentColor }}
              />
              <Progress percent={percent} size="small" strokeColor={item.color} showInfo />
            </Card>
          </Col>
        );
      })}
    </Row>
  );
}

export default function FinanceAnalytics() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [overview, setOverview] = useState({
    canViewContracts: false,
    totalActualIncome: 0,
    totalActualExpense: 0,
    totalGrossProfit: 0,
    overallGrossMarginRate: null,
    nonProjectIncome: 0,
    nonProjectExpense: 0,
    totalReceivable: 0,
    totalPayable: 0,
    totalPendingInvoice: 0,
    totalMissingInvoice: 0,
    receivableAging: {},
    payableAging: {},
    projectProfits: [],
    receivableByCounterparty: [],
    payableByCounterparty: [],
  });

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const response = await financeAPI.getFinanceAnalyticsOverview();
      const payload = response?.data ?? response ?? {};
      setOverview({
        canViewContracts: Boolean(payload.canViewContracts),
        totalContractAmount: payload.totalContractAmount ?? 0,
        totalActualIncome: payload.totalActualIncome ?? 0,
        totalActualExpense: payload.totalActualExpense ?? 0,
        totalBookedIncome: payload.totalBookedIncome ?? 0,
        totalBookedExpense: payload.totalBookedExpense ?? 0,
        totalGrossProfit: payload.totalGrossProfit ?? 0,
        overallGrossMarginRate: payload.overallGrossMarginRate ?? null,
        nonProjectIncome: payload.nonProjectIncome ?? 0,
        nonProjectExpense: payload.nonProjectExpense ?? 0,
        nonProjectBookedExpense: payload.nonProjectBookedExpense ?? 0,
        totalReceivable: payload.totalReceivable ?? 0,
        totalPayable: payload.totalPayable ?? 0,
        totalPendingInvoice: payload.totalPendingInvoice ?? 0,
        totalMissingInvoice: payload.totalMissingInvoice ?? 0,
        receivableAging: payload.receivableAging || {},
        payableAging: payload.payableAging || {},
        projectProfits: Array.isArray(payload.projectProfits) ? payload.projectProfits : [],
        receivableByCounterparty: Array.isArray(payload.receivableByCounterparty) ? payload.receivableByCounterparty : [],
        payableByCounterparty: Array.isArray(payload.payableByCounterparty) ? payload.payableByCounterparty : [],
      });
    } catch (error) {
      message.error(error?.message || "加载财务分析数据失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredProfits = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return overview.projectProfits;
    return overview.projectProfits.filter((row) =>
      [row.projectNumber, row.projectName, row.customerName, row.currentStageName].some((field) =>
        String(field || "").toLowerCase().includes(normalized)
      )
    );
  }, [keyword, overview.projectProfits]);

  const filteredReceivables = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return overview.receivableByCounterparty;
    return overview.receivableByCounterparty.filter((row) =>
      String(row.customerName || "").toLowerCase().includes(normalized)
    );
  }, [keyword, overview.receivableByCounterparty]);

  const filteredPayables = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return overview.payableByCounterparty;
    return overview.payableByCounterparty.filter((row) =>
      String(row.customerName || "").toLowerCase().includes(normalized)
    );
  }, [keyword, overview.payableByCounterparty]);

  const exportProfitTable = () => {
    const headers = [
      "项目编号",
      "项目名称",
      "客户",
      "当前阶段",
      ...(overview.canViewContracts ? ["合同金额"] : []),
      "实际收入",
      "实际支出",
      "实际毛利",
      "毛利率(%)",
      "记账净额",
      "应收金额",
      "应付金额",
      "待开票",
      "缺票",
    ];
    downloadExcel(
      `project-profit-analysis-${dayjs().format("YYYY-MM-DD")}.xls`,
      "项目利润表",
      headers,
      filteredProfits.map((row) => [
        row.projectNumber || "",
        row.projectName || "",
        row.customerName || "",
        row.currentStageName || "",
        ...(overview.canViewContracts ? [row.contractAmount ?? 0] : []),
        row.actualIncomeAmount ?? 0,
        row.actualExpenseAmount ?? 0,
        row.grossProfit ?? 0,
        row.grossMarginRate ?? "",
        row.bookedNetProfit ?? 0,
        row.receivableAmount ?? 0,
        row.payableAmount ?? 0,
        row.pendingInvoiceAmount ?? 0,
        row.missingInvoiceAmount ?? 0,
      ])
    );
  };

  const exportAgingTable = (rows, label) => {
    downloadExcel(
      `${label}-${dayjs().format("YYYY-MM-DD")}.xls`,
      label,
      ["往来单位", "0-30天", "31-60天", "61-90天", "90天以上", "合计", "凭证数", "最早业务日期", "最大账龄(天)"],
      rows.map((row) => [
        row.customerName || "",
        row.bucket0To30 ?? 0,
        row.bucket31To60 ?? 0,
        row.bucket61To90 ?? 0,
        row.bucketOver90 ?? 0,
        row.total ?? 0,
        row.voucherCount ?? 0,
        row.oldestOccurredOn || "",
        row.maxAgingDays ?? 0,
      ])
    );
  };

  const profitColumns = useMemo(() => {
    const cols = [
      {
        title: "项目",
        key: "project",
        fixed: "left",
        width: 220,
        render: (_, record) => (
          <Space direction="vertical" size={0}>
            <Button
              type="link"
              style={{ padding: 0, height: "auto", fontWeight: 600 }}
              onClick={() => navigate(`/project/dashboard?projectUuid=${record.projectId}`)}
            >
              {record.projectNumber} · {record.projectName}
            </Button>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {record.customerName || "-"}
            </Text>
          </Space>
        ),
      },
      { title: "阶段", dataIndex: "currentStageName", key: "currentStageName", width: 120, render: (v) => <Tag>{v || "未开始"}</Tag> },
    ];
    if (overview.canViewContracts) {
      cols.push({
        title: "合同总额",
        dataIndex: "contractAmount",
        key: "contractAmount",
        width: 120,
        align: "right",
        sorter: (a, b) => Number(a.contractAmount || 0) - Number(b.contractAmount || 0),
        render: (v) => formatMoney(v),
      });
    }
    cols.push(
      {
        title: "实际收入",
        dataIndex: "actualIncomeAmount",
        key: "actualIncomeAmount",
        width: 120,
        align: "right",
        sorter: (a, b) => Number(a.actualIncomeAmount || 0) - Number(b.actualIncomeAmount || 0),
        render: (v) => formatMoney(v),
      },
      {
        title: "实际支出",
        dataIndex: "actualExpenseAmount",
        key: "actualExpenseAmount",
        width: 120,
        align: "right",
        sorter: (a, b) => Number(a.actualExpenseAmount || 0) - Number(b.actualExpenseAmount || 0),
        render: (v) => formatMoney(v),
      },
      {
        title: "实际毛利",
        dataIndex: "grossProfit",
        key: "grossProfit",
        width: 120,
        align: "right",
        sorter: (a, b) => Number(a.grossProfit || 0) - Number(b.grossProfit || 0),
        render: (v) => <Text strong type={Number(v || 0) < 0 ? "danger" : "success"}>{formatMoney(v)}</Text>,
      },
      {
        title: "毛利率",
        dataIndex: "grossMarginRate",
        key: "grossMarginRate",
        width: 100,
        align: "right",
        render: (v) => formatPercent(v),
      },
      {
        title: "记账净额",
        dataIndex: "bookedNetProfit",
        key: "bookedNetProfit",
        width: 120,
        align: "right",
        render: (v) => formatMoney(v),
      },
      {
        title: "应收",
        dataIndex: "receivableAmount",
        key: "receivableAmount",
        width: 110,
        align: "right",
        render: (v) => <Text type={Number(v || 0) > 0 ? "danger" : undefined}>{formatMoney(v)}</Text>,
      },
      {
        title: "应付",
        dataIndex: "payableAmount",
        key: "payableAmount",
        width: 110,
        align: "right",
        render: (v) => formatMoney(v),
      },
      {
        title: "待开票 / 缺票",
        key: "invoiceGaps",
        width: 150,
        align: "right",
        render: (_, record) => `${formatMoney(record.pendingInvoiceAmount)} / ${formatMoney(record.missingInvoiceAmount)}`,
      }
    );
    return cols;
  }, [navigate, overview.canViewContracts]);

  const buildAgingColumns = () => [
    { title: "往来单位", dataIndex: "customerName", key: "customerName", fixed: "left", width: 220 },
    { title: "0-30天", dataIndex: "bucket0To30", key: "bucket0To30", width: 120, align: "right", render: formatMoney },
    { title: "31-60天", dataIndex: "bucket31To60", key: "bucket31To60", width: 120, align: "right", render: formatMoney },
    { title: "61-90天", dataIndex: "bucket61To90", key: "bucket61To90", width: 120, align: "right", render: formatMoney },
    {
      title: "90天以上",
      dataIndex: "bucketOver90",
      key: "bucketOver90",
      width: 120,
      align: "right",
      render: (v) => <Text type={Number(v || 0) > 0 ? "danger" : undefined}>{formatMoney(v)}</Text>,
    },
    { title: "合计金额", dataIndex: "total", key: "total", width: 130, align: "right", render: (v) => <Text strong>{formatMoney(v)}</Text> },
    { title: "笔数", dataIndex: "voucherCount", key: "voucherCount", width: 80, align: "center" },
    { title: "最早日期", dataIndex: "oldestOccurredOn", key: "oldestOccurredOn", width: 120, render: (v) => v || "-" },
    {
      title: "最长账龄",
      dataIndex: "maxAgingDays",
      key: "maxAgingDays",
      width: 100,
      align: "center",
      render: (days) => <Tag color={days > 90 ? "error" : days > 60 ? "warning" : "default"}>{days} 天</Tag>,
    },
  ];

  const tabItems = [
    {
      key: "profit",
      label: `项目利润表 (${filteredProfits.length})`,
      children: (
        <Space direction="vertical" size={12} style={{ width: "100%" }}>
          <div style={{ display: "flex", justifyContent: "flex-end" }}>
            <Button icon={<DownloadOutlined />} onClick={exportProfitTable} disabled={!filteredProfits.length}>
              导出利润表 Excel
            </Button>
          </div>
          <Table
            rowKey="projectId"
            loading={loading}
            columns={profitColumns}
            dataSource={filteredProfits}
            scroll={{ x: "max-content" }}
            pagination={{ defaultPageSize: 15, showSizeChanger: true }}
          />
        </Space>
      ),
    },
    {
      key: "receivable",
      label: `应收账龄分析 (${filteredReceivables.length})`,
      children: (
        <>
          {renderAgingCards(overview.receivableAging, "#cf1322")}
          <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 12 }}>
            <Button
              icon={<DownloadOutlined />}
              onClick={() => exportAgingTable(filteredReceivables, "receivable-aging")}
              disabled={!filteredReceivables.length}
            >
              导出应收账龄 Excel
            </Button>
          </div>
          {filteredReceivables.length ? (
            <Table
              rowKey={(record) => `${record.customerId}-${record.customerName}`}
              loading={loading}
              columns={buildAgingColumns()}
              dataSource={filteredReceivables}
              scroll={{ x: "max-content" }}
            />
          ) : (
            <Empty description="暂无未结清应收账款" />
          )}
        </>
      ),
    },
    {
      key: "payable",
      label: `应付账龄分析 (${filteredPayables.length})`,
      children: (
        <>
          {renderAgingCards(overview.payableAging, "#d46b08")}
          <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 12 }}>
            <Button
              icon={<DownloadOutlined />}
              onClick={() => exportAgingTable(filteredPayables, "payable-aging")}
              disabled={!filteredPayables.length}
            >
              导出应付账龄 Excel
            </Button>
          </div>
          {filteredPayables.length ? (
            <Table
              rowKey={(record) => `${record.customerId}-${record.customerName}`}
              loading={loading}
              columns={buildAgingColumns()}
              dataSource={filteredPayables}
              scroll={{ x: "max-content" }}
            />
          ) : (
            <Empty description="暂无未结清应付账款" />
          )}
        </>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space direction="vertical" size={4}>
              <Space>
                <BarChartOutlined style={{ fontSize: 20, color: "#1677ff" }} />
                <Title level={4} style={{ margin: 0 }}>
                  项目利润与应收应付账龄分析
                </Title>
              </Space>
              <Text type="secondary">
                基于财务凭证按 BigDecimal 精确汇总项目收支毛利、项目调出冲减净额、非项目经营费用及应收应付账龄分布。
              </Text>
            </Space>
          </Col>
          <Col>
            <Space>
              <Input
                allowClear
                prefix={<SearchOutlined />}
                placeholder="搜索项目 / 客户 / 往来单位"
                style={{ width: 240 }}
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
              <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={[12, 12]}>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small">
            <Statistic title="累计实收" value={Number(overview.totalActualIncome || 0)} precision={2} valueStyle={{ color: "#3f8600" }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small">
            <Statistic title="累计实付" value={Number(overview.totalActualExpense || 0)} precision={2} />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small">
            <Statistic
              title="汇总毛利"
              value={Number(overview.totalGrossProfit || 0)}
              precision={2}
              suffix={overview.overallGrossMarginRate != null ? `(${Number(overview.overallGrossMarginRate).toFixed(1)}%)` : ""}
              valueStyle={{ color: Number(overview.totalGrossProfit || 0) < 0 ? "#cf1322" : "#1677ff" }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small">
            <Statistic title="应收余额" value={Number(overview.totalReceivable || 0)} precision={2} valueStyle={{ color: Number(overview.totalReceivable || 0) > 0 ? "#cf1322" : undefined }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small">
            <Statistic title="应付余额" value={Number(overview.totalPayable || 0)} precision={2} valueStyle={{ color: Number(overview.totalPayable || 0) > 0 ? "#d46b08" : undefined }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small">
            <Statistic title="非项目支出" value={Number(overview.nonProjectExpense || 0)} precision={2} />
          </Card>
        </Col>
      </Row>

      <Card>
        {loading && !overview.projectProfits.length ? (
          <div style={{ padding: 48, textAlign: "center" }}>
            <Spin size="large" />
          </div>
        ) : (
          <Tabs items={tabItems} />
        )}
      </Card>
    </Space>
  );
}
