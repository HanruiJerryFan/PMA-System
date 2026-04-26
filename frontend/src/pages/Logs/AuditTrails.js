import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Form, Input, Space, Table, Tag } from "antd";
import { DownloadOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { getCurrentUser } from "../../api/auth";
import { logAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { downloadExcel } from "../../utils/exporters";

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString() : "-";
}

export default function AuditTrails() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({});
  const [currentUser, setCurrentUser] = useState(null);
  const [form] = Form.useForm();
  const canExport = hasAnyAuthority(currentUser, ["logs.view"]);

  const fetchRecords = useCallback(async (nextFilters = filters) => {
    setLoading(true);
    try {
      const params = {};
      if (nextFilters.userId) params.userId = nextFilters.userId;
      if (nextFilters.module) params.module = nextFilters.module;
      if (nextFilters.action) params.action = nextFilters.action;
      const response = await logAPI.getAuditTrails(Object.keys(params).length > 0 ? params : undefined);
      setRecords(Array.isArray(response?.data) ? response.data : []);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    fetchRecords({});
  }, [fetchRecords]);

  useEffect(() => {
    async function fetchCurrentUser() {
      try {
        setCurrentUser(await getCurrentUser());
      } catch (error) {
        setCurrentUser(null);
      }
    }

    fetchCurrentUser();
  }, []);

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

  const handleSearch = async (values) => {
    const nextFilters = {
      userId: values.userId ? Number(values.userId) : undefined,
      module: values.module?.trim() || undefined,
      action: values.action?.trim() || undefined,
    };
    setFilters(nextFilters);
    await fetchRecords(nextFilters);
  };

  const handleReset = async () => {
    form.resetFields();
    const nextFilters = {};
    setFilters(nextFilters);
    await fetchRecords(nextFilters);
  };

  const handleExportExcel = () => {
    downloadExcel(
      `审计日志-${new Date().toISOString().slice(0, 10)}.xls`,
      "审计日志",
      ["ID", "用户 ID", "用户名", "模块", "动作", "目标类型", "目标 ID", "详情", "发生时间"],
      records.map((item) => [
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
  };

  return (
    <Card
      title="审计日志"
      extra={(
        <Space>
          <Button icon={<DownloadOutlined />} onClick={handleExportExcel} disabled={!canExport || records.length === 0}>
            导出 Excel
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => fetchRecords()}>
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
        pagination={{ pageSize: 10, showSizeChanger: true }}
        scroll={{ x: 1700 }}
      />
    </Card>
  );
}
