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
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({});
  const [currentUser, setCurrentUser] = useState(null);
  const [form] = Form.useForm();
  const canExport = hasAnyAuthority(currentUser, ["logs.view"]);

  const fetchRecords = useCallback(async (nextFilters = filters) => {
    setLoading(true);
    try {
      const params = nextFilters.userId ? { userId: nextFilters.userId } : undefined;
      const response = await logAPI.getLoginRecords(params);
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

  const handleSearch = async (values) => {
    const nextFilters = {
      userId: values.userId ? Number(values.userId) : undefined,
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
      `登录日志-${new Date().toISOString().slice(0, 10)}.xls`,
      "登录日志",
      ["ID", "用户 ID", "登录时间", "最后活跃时间", "退出时间", "持续时长", "退出原因", "登录 IP", "浏览器标识", "创建时间"],
      records.map((item) => [
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
  };

  return (
    <Card
      title="登录日志"
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
        scroll={{ x: 1600 }}
      />
    </Card>
  );
}
