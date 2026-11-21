import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from "antd";
import {
  AuditOutlined,
  CheckCircleOutlined,
  EyeOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { approvalAPI, financeAPI, inventoryAPI, projectAPI } from "../../api/modules";

const { Text, Title } = Typography;

function formatAmount(value) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  return Number(value).toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function matchesKeyword(item, keyword) {
  if (!keyword) {
    return true;
  }
  const normalized = keyword.trim().toLowerCase();
  return [
    item.code,
    item.title,
    item.subTypeLabel,
    item.projectNumber,
    item.projectName,
    item.counterpartyName,
    item.entryUserName,
  ].some((field) => String(field || "").toLowerCase().includes(normalized));
}

export default function ApprovalWorkbench() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [auditingKey, setAuditingKey] = useState(null);
  const [keyword, setKeyword] = useState("");
  const [summary, setSummary] = useState({
    totalPendingCount: 0,
    projectListCount: 0,
    warehouseDocCount: 0,
    financeVoucherCount: 0,
    canAuditProjectLists: false,
    canAuditWarehouseDocs: false,
    canAuditFinanceVouchers: false,
    projectLists: [],
    warehouseDocs: [],
    financeVouchers: [],
  });

  const loadApprovals = useCallback(async () => {
    setLoading(true);
    try {
      const response = await approvalAPI.getPendingApprovals();
      const payload = response?.data ?? response ?? {};
      setSummary({
        totalPendingCount: payload.totalPendingCount || 0,
        projectListCount: payload.projectListCount || 0,
        warehouseDocCount: payload.warehouseDocCount || 0,
        financeVoucherCount: payload.financeVoucherCount || 0,
        canAuditProjectLists: Boolean(payload.canAuditProjectLists),
        canAuditWarehouseDocs: Boolean(payload.canAuditWarehouseDocs),
        canAuditFinanceVouchers: Boolean(payload.canAuditFinanceVouchers),
        projectLists: Array.isArray(payload.projectLists) ? payload.projectLists : [],
        warehouseDocs: Array.isArray(payload.warehouseDocs) ? payload.warehouseDocs : [],
        financeVouchers: Array.isArray(payload.financeVouchers) ? payload.financeVouchers : [],
      });
    } catch (error) {
      message.error(error?.message || "加载待审核列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadApprovals();
  }, [loadApprovals]);

  const handleAudit = useCallback(async (record) => {
    const key = `${record.module}:${record.id}`;
    setAuditingKey(key);
    try {
      if (record.module === "PROJECT_LIST") {
        await projectAPI.auditProjectList(record.id);
      } else if (record.module === "WAREHOUSE_DOC") {
        await inventoryAPI.auditWarehouseDoc(record.id);
      } else if (record.module === "FINANCE_VOUCHER") {
        await financeAPI.auditFinanceVoucher(record.id);
      }
      message.success(`已审核通过：${record.title || record.code}`);
      await loadApprovals();
    } catch (error) {
      message.error(error?.message || "审核操作失败");
    } finally {
      setAuditingKey(null);
    }
  }, [loadApprovals]);

  const buildColumns = useCallback((showAmount, titleLabel) => [
    {
      title: "单据编号 / 类型",
      key: "code",
      width: 190,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Tag color="blue">{record.subTypeLabel || record.code}</Tag>
          <Text strong>{record.code}</Text>
        </Space>
      ),
    },
    {
      title: titleLabel,
      dataIndex: "title",
      key: "title",
      width: 220,
      render: (value) => value || "-",
    },
    {
      title: "关联项目",
      key: "project",
      width: 220,
      render: (_, record) =>
        record.projectId ? (
          <Button
            type="link"
            style={{ padding: 0, height: "auto", whiteSpace: "normal", textAlign: "left" }}
            onClick={() => navigate(`/project/dashboard?projectUuid=${record.projectId}`)}
          >
            {record.projectNumber ? `${record.projectNumber} · ` : ""}
            {record.projectName || record.projectId}
          </Button>
        ) : (
          "-"
        ),
    },
    {
      title: "客户 / 往来方",
      dataIndex: "counterpartyName",
      key: "counterpartyName",
      width: 180,
      render: (value) => value || "-",
    },
    ...(showAmount
      ? [
          {
            title: "金额",
            dataIndex: "amount",
            key: "amount",
            width: 130,
            align: "right",
            render: (value) => formatAmount(value),
          },
        ]
      : []),
    { title: "业务日期", dataIndex: "bizDate", key: "bizDate", width: 120, render: (v) => v || "-" },
    { title: "录入人", dataIndex: "entryUserName", key: "entryUserName", width: 110, render: (v) => v || "-" },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 190,
      render: (_, record) => {
        const key = `${record.module}:${record.id}`;
        return (
          <Space>
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(record.detailRoute || "/home")}
            >
              查看
            </Button>
            <Popconfirm
              title={`确认审核通过“${record.title || record.code}”？`}
              okText="审核通过"
              cancelText="取消"
              onConfirm={() => handleAudit(record)}
            >
              <Button
                size="small"
                type="primary"
                icon={<CheckCircleOutlined />}
                loading={auditingKey === key}
              >
                审核通过
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ], [auditingKey, handleAudit, navigate]);

  const filteredProjectLists = useMemo(
    () => summary.projectLists.filter((item) => matchesKeyword(item, keyword)),
    [keyword, summary.projectLists]
  );
  const filteredWarehouseDocs = useMemo(
    () => summary.warehouseDocs.filter((item) => matchesKeyword(item, keyword)),
    [keyword, summary.warehouseDocs]
  );
  const filteredFinanceVouchers = useMemo(
    () => summary.financeVouchers.filter((item) => matchesKeyword(item, keyword)),
    [keyword, summary.financeVouchers]
  );

  const tabItems = useMemo(() => {
    const items = [];
    if (summary.canAuditProjectLists) {
      items.push({
        key: "PROJECT_LIST",
        label: (
          <Space size={6}>
            <span>项目清单</span>
            <Badge count={filteredProjectLists.length} showZero color="#1677ff" />
          </Space>
        ),
        children: (
          <Table
            rowKey="id"
            loading={loading}
            columns={buildColumns(false, "清单名称")}
            dataSource={filteredProjectLists}
            scroll={{ x: "max-content" }}
            locale={{ emptyText: "暂无待审核项目清单" }}
          />
        ),
      });
    }
    if (summary.canAuditWarehouseDocs) {
      items.push({
        key: "WAREHOUSE_DOC",
        label: (
          <Space size={6}>
            <span>出入库单</span>
            <Badge count={filteredWarehouseDocs.length} showZero color="#1677ff" />
          </Space>
        ),
        children: (
          <Space direction="vertical" size={12} style={{ width: "100%" }}>
            <Alert
              type="info"
              showIcon
              message="提示：划拨单自动生成的关联入库单已自动归并，审核原划拨单时将同步完成关联入库单审核。"
            />
            <Table
              rowKey="id"
              loading={loading}
              columns={buildColumns(true, "单据号")}
              dataSource={filteredWarehouseDocs}
              scroll={{ x: "max-content" }}
              locale={{ emptyText: "暂无待审核出入库单" }}
            />
          </Space>
        ),
      });
    }
    if (summary.canAuditFinanceVouchers) {
      items.push({
        key: "FINANCE_VOUCHER",
        label: (
          <Space size={6}>
            <span>财务凭证</span>
            <Badge count={filteredFinanceVouchers.length} showZero color="#1677ff" />
          </Space>
        ),
        children: (
          <Table
            rowKey="id"
            loading={loading}
            columns={buildColumns(true, "内容摘要")}
            dataSource={filteredFinanceVouchers}
            scroll={{ x: "max-content" }}
            locale={{ emptyText: "暂无待审核财务凭证" }}
          />
        ),
      });
    }
    return items;
  }, [
    buildColumns,
    filteredFinanceVouchers,
    filteredProjectLists,
    filteredWarehouseDocs,
    loading,
    summary.canAuditFinanceVouchers,
    summary.canAuditProjectLists,
    summary.canAuditWarehouseDocs,
  ]);

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space direction="vertical" size={4}>
              <Space>
                <AuditOutlined style={{ fontSize: 20, color: "#1677ff" }} />
                <Title level={4} style={{ margin: 0 }}>
                  统一待审核工作台
                </Title>
              </Space>
              <Text type="secondary">
                集中展示当前账号有权审核的项目清单、出入库单据与财务凭证，支持一键审核并跳转单据详情。
              </Text>
            </Space>
          </Col>
          <Col>
            <Space>
              <Input
                allowClear
                prefix={<SearchOutlined />}
                placeholder="搜索单据 / 项目 / 往来单位 / 录入人"
                style={{ width: 260 }}
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
              <Button icon={<ReloadOutlined />} onClick={loadApprovals} loading={loading}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic title="待审核总数" value={summary.totalPendingCount} valueStyle={{ color: "#1677ff" }} />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic
              title="待审项目清单"
              value={summary.canAuditProjectLists ? summary.projectListCount : "-"}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic
              title="待审出入库单"
              value={summary.canAuditWarehouseDocs ? summary.warehouseDocCount : "-"}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic
              title="待审财务凭证"
              value={summary.canAuditFinanceVouchers ? summary.financeVoucherCount : "-"}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        {tabItems.length ? (
          <Tabs items={tabItems} />
        ) : (
          <Empty description="当前账号未分配审核权限" />
        )}
      </Card>
    </Space>
  );
}
