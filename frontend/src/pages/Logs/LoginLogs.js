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

function formatDuration(seconds) {
  if (seconds == null || Number.isNaN(Number(seconds))) {
    return "-";
  }

  const totalSeconds = Math.max(0, Math.round(Number(seconds)));
  const minutes = Math.floor(totalSeconds / 60);
  const remainingSeconds = totalSeconds % 60;

  if (minutes === 0) {
    return `${remainingSeconds} 秒`;
  }
  if (remainingSeconds === 0) {
    return `${minutes} 分钟`;
  }
  return `${minutes} 分 ${remainingSeconds} 秒`;
}

function getLogoutReasonLabel(reason) {
  const labels = {
    MANUAL: "主动退出",
    TIMEOUT: "超时结束",
    BROWSER_CLOSED: "页面关闭",
    FORCED: "强制退出",
  };
  return labels[reason] || reason || "-";
}

export default function LoginLogs() {
  const [records, setRecords] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [query, setQuery] = useState({ page: 1, pageSize: 10, filters: {}, reloadKey: 0 });
  const { currentUser } = useAuth();
  const [form] = Form.useForm();
  const canExport = hasAnyAuthority(currentUser, ["logs.view"]);

  const fetchRecords = useCallback(async ({ page, pageSize, filters }) => {
    setLoading(true);
    try {
      const params = { page, pageSize };
      if (filters?.userId) params.userId = filters.userId;
      const response = await logAPI.getLoginRecordsPage(params);
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
      {
        title: "登录时间",
        dataIndex: "loginStartTime",
        key: "loginStartTime",
        width: 180,
        render: formatDateTime,
      },
      {
        title: "最后活跃时间",
        dataIndex: "lastSeenAt",
        key: "lastSeenAt",
        width: 180,
        render: formatDateTime,
      },
      {
        title: "退出时间",
        dataIndex: "loginEndTime",
        key: "loginEndTime",
        width: 180,
        render: formatDateTime,
      },
      {
        title: "持续时长",
        dataIndex: "durationSeconds",
        key: "durationSeconds",
        width: 140,
        render: formatDuration,
      },
      {
        title: "退出原因",
        dataIndex: "logoutReason",
        key: "logoutReason",
        width: 120,
        render: (value) => (value ? <Tag color="blue">{getLogoutReasonLabel(value)}</Tag> : "-"),
      },
      { title: "登录 IP", dataIndex: "loginIp", key: "loginIp", width: 140, render: (value) => value || "-" },
      { title: "浏览器标识", dataIndex: "userAgent", key: "userAgent", width: 320, ellipsis: true, render: (value) => value || "-" },
      {
        title: "创建时间",
        dataIndex: "createdAt",
        key: "createdAt",
        width: 180,
        render: formatDateTime,
      },
    ],
    []
  );

  const handleSearch = (values) => {
    const nextFilters = {
      userId: values.userId ? Number(values.userId) : undefined,
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
      const params = query.filters?.userId ? { userId: query.filters.userId } : undefined;
      const response = await logAPI.getLoginRecords(params);
      const exportRows = Array.isArray(response?.data) ? response.data : [];
      downloadExcel(
        `登录日志-${new Date().toISOString().slice(0, 10)}.xls`,
        "登录日志",
        ["ID", "用户 ID", "登录时间", "最后活跃时间", "退出时间", "持续时长", "退出原因", "登录 IP", "浏览器标识", "创建时间"],
        exportRows.map((item) => [
          item.id ?? "",
          item.userId ?? "",
          item.loginStartTime ? new Date(item.loginStartTime).toLocaleString() : "",
          item.lastSeenAt ? new Date(item.lastSeenAt).toLocaleString() : "",
          item.loginEndTime ? new Date(item.loginEndTime).toLocaleString() : "",
          formatDuration(item.durationSeconds),
          getLogoutReasonLabel(item.logoutReason),
          item.loginIp ?? "",
          item.userAgent ?? "",
          item.createdAt ? new Date(item.createdAt).toLocaleString() : "",
        ])
      );
    } finally {
      setExporting(false);
    }
  };

  return (
    <Card
      title="登录日志"
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
        scroll={{ x: 1600 }}
      />
    </Card>
  );
}
