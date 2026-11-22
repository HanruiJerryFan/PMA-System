import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Form, Input, Space, Table, Tag } from "antd";
import { DownloadOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { useAuth } from "../../context/AuthContext";
import { logAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { downloadExcel } from "../../utils/exporters";

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString() : "-";
}

export default function AuditTrails() {
  const [records, setRecords] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [query, setQuery] = useState({ page: 1, pageSize: 10, filters: {}, reloadKey: 0 });
  const { currentUser } = useAuth();
  const [form] = Form.useForm();
  const canExport = hasAnyAuthority(currentUser, ["logs.view", "logs.manage"]);

  const fetchRecords = useCallback(async ({ page, pageSize, filters }) => {
    setLoading(true);
    try {
      const params = { page, pageSize };
      if (filters?.userId) params.userId = filters.userId;
      if (filters?.module) params.module = filters.module;
      if (filters?.action) params.action = filters.action;
      const response = await logAPI.getAuditTrailsPage(params);
      const pageData = response?.data || {};
      const nextRecords = Array.isArray(pageData.records) ? pageData.records : [];
      const nextTotal = Number(pageData.total ?? 0);
      if (nextRecords.length === 0 && page > 1 && nextTotal > 0) {
        const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize));
        setQuery((prev) => ({ ...prev, page: lastPage }));
        return;
      }
      setRecords(nextRecords);
      setTotal(nextTotal);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRecords(query);
  }, [fetchRecords, query]);

  const columns = useMemo(
    () => [
      { title: "ID", dataIndex: "id", key: "id", width: 80 },
      { title: "用户 ID", dataIndex: "userId", key: "userId", width: 100 },
      { title: "用户名", dataIndex: "username", key: "username", width: 140, render: (value) => value || "-" },
      { title: "模块", dataIndex: "module", key: "module", width: 160, render: (value) => <Tag color="blue">{value || "-"}</Tag> },
      { title: "动作", dataIndex: "action", key: "action", width: 120, render: (value) => <Tag color="green">{value || "-"}</Tag> },
      { title: "目标类型", dataIndex: "targetType", key: "targetType", width: 160, render: (value) => value || "-" },
      { title: "目标 ID", dataIndex: "targetId", key: "targetId", width: 200, render: (value) => value || "-" },
      { title: "详情", dataIndex: "detail", key: "detail", width: 320, render: (value) => value || "-" },
      { title: "发生时间", dataIndex: "occurredAt", key: "occurredAt", width: 180, render: formatDateTime },
    ],
    []
  );

  const handleSearch = (values) => {
    const nextFilters = {
      userId: values.userId ? Number(values.userId) : undefined,
      module: values.module?.trim() || undefined,
      action: values.action?.trim() || undefined,
    };
    setQuery((prev) => ({ ...prev, page: 1, filters: nextFilters }));
  };

  const handleReset = () => {
    form.resetFields();
    setQuery((prev) => ({ ...prev, page: 1, filters: {} }));
  };

  const handleExportExcel = async () => {
    setExporting(true);
    try {
      const params = {};
      if (query.filters?.userId) params.userId = query.filters.userId;
      if (query.filters?.module) params.module = query.filters.module;
      if (query.filters?.action) params.action = query.filters.action;
      const response = await logAPI.getAuditTrails(Object.keys(params).length > 0 ? params : undefined);
      const exportRows = Array.isArray(response?.data) ? response.data : [];
      downloadExcel(
        `审计日志-${new Date().toISOString().slice(0, 10)}.xls`,
        "审计日志",
        ["ID", "用户 ID", "用户名", "模块", "动作", "目标类型", "目标 ID", "详情", "发生时间"],
        exportRows.map((item) => [
          item.id ?? "",
          item.userId ?? "",
          item.username || "",
          item.module || "",
          item.action || "",
          item.targetType || "",
          item.targetId || "",
          item.detail || "",
          item.occurredAt ? new Date(item.occurredAt).toLocaleString() : "",
        ])
      );
    } finally {
      setExporting(false);
    }
  };

  return (
    <Card
      title="审计日志"
      extra={(
        <Space>
          <Button icon={<DownloadOutlined />} loading={exporting} onClick={handleExportExcel} disabled={!canExport || total === 0}>
            导出 Excel
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => setQuery((prev) => ({ ...prev, reloadKey: prev.reloadKey + 1 }))}>
            刷新
          </Button>
        </Space>
      )}
    >
      <Form form={form} layout="inline" onFinish={handleSearch} style={{ marginBottom: 16 }}>
        <Form.Item name="userId" label="用户 ID">
          <Input placeholder="输入用户 ID" allowClear />
        </Form.Item>
        <Form.Item name="module" label="模块">
          <Input placeholder="输入模块名" allowClear />
        </Form.Item>
        <Form.Item name="action" label="动作">
          <Input placeholder="输入动作名" allowClear />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              搜索
            </Button>
            <Button onClick={handleReset}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={records}
        loading={loading}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (count) => `共 ${count} 条`,
          onChange: (nextPage, nextPageSize) => setQuery((prev) => ({ ...prev, page: nextPage, pageSize: nextPageSize })),
        }}
        scroll={{ x: 1700 }}
      />
    </Card>
  );
}
