import React, { useEffect, useState } from "react";
import { Button, Input, InputNumber, Popconfirm, Switch, Tag, message } from "antd";
import { ReloadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI } from "../../api/modules";

export default function CustomerActivityRules() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const fetchRules = async () => {
    setLoading(true);
    try {
      const response = await customerAPI.getCustomerActivityRules();
      setRules(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleCreate = async (values) => {
    await customerAPI.createCustomerActivityRule(values);
    await fetchRules();
  };

  const handleUpdate = async (id, values) => {
    await customerAPI.updateCustomerActivityRule(id, values);
    await fetchRules();
  };

  const handleDelete = async (id) => {
    await customerAPI.deleteCustomerActivityRule(id);
    await fetchRules();
  };

  const handleRefreshStatuses = async () => {
    setRefreshing(true);
    try {
      const response = await customerAPI.refreshCustomerActivityStatuses();
      message.success(`已刷新 ${response.data ?? 0} 个客户状态`);
      await fetchRules();
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <CRUDTable
      title="客户活跃规则"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        {
          title: "状态",
          dataIndex: "statusCode",
          key: "statusCode",
          width: 140,
          render: (value) => <Tag color="blue">{value}</Tag>,
        },
        { title: "最小静默天数", dataIndex: "minIdleDays", key: "minIdleDays", width: 140 },
        { title: "最大静默天数", dataIndex: "maxIdleDays", key: "maxIdleDays", width: 140, render: (value) => value ?? "不限制" },
        { title: "排序", dataIndex: "sortOrder", key: "sortOrder", width: 120 },
        {
          title: "启用状态",
          dataIndex: "isActive",
          key: "isActive",
          width: 120,
          render: (value) => (value ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>),
        },
      ]}
      dataSource={rules}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[{ name: "statusCode", label: "状态" }]}
      extraActions={
        <Popconfirm title="确认立即刷新全部客户活跃状态吗？" onConfirm={handleRefreshStatuses} okText="确定" cancelText="取消">
          <Button icon={<ReloadOutlined />} loading={refreshing}>
            刷新状态
          </Button>
        </Popconfirm>
      }
      formFields={[
        {
          name: "statusCode",
          label: "状态",
          rules: [{ required: true, message: "请输入状态编码" }],
          component: <Input placeholder="ACTIVE / NORMAL / INACTIVE / DORMANT" maxLength={32} />,
        },
        {
          name: "minIdleDays",
          label: "最小静默天数",
          rules: [{ required: true, message: "请输入最小静默天数" }],
          component: <InputNumber min={0} precision={0} style={{ width: "100%" }} />,
        },
        {
          name: "maxIdleDays",
          label: "最大静默天数",
          component: <InputNumber min={0} precision={0} style={{ width: "100%" }} placeholder="留空表示不限制" />,
        },
        {
          name: "sortOrder",
          label: "排序",
          rules: [{ required: true, message: "请输入排序值" }],
          component: <InputNumber min={0} precision={0} style={{ width: "100%" }} />,
        },
        {
          name: "isActive",
          label: "启用",
          valuePropName: "checked",
          rules: [{ required: true, message: "请选择启用状态" }],
          component: <Switch checkedChildren="启用" unCheckedChildren="停用" />,
        },
      ]}
      mapRecordToFormValues={(record) => ({
        ...record,
        isActive: Boolean(record.isActive),
      })}
      transformValues={(values) => ({
        ...values,
        statusCode: values.statusCode?.trim()?.toUpperCase(),
        minIdleDays: Number(values.minIdleDays),
        maxIdleDays: values.maxIdleDays === undefined || values.maxIdleDays === null || values.maxIdleDays === "" ? null : Number(values.maxIdleDays),
        sortOrder: Number(values.sortOrder),
        isActive: Boolean(values.isActive),
      })}
    />
  );
}
