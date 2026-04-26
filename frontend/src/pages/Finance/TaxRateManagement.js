import React, { useEffect, useState } from "react";
import { Input, InputNumber, Switch, Tag } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { financeAPI } from "../../api/modules";

export default function TaxRateManagement() {
  const [taxRates, setTaxRates] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchTaxRates = async () => {
    setLoading(true);
    try {
      const response = await financeAPI.getTaxRateDicts();
      setTaxRates(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTaxRates();
  }, []);

  const handleCreate = async (values) => {
    await financeAPI.createTaxRateDict(values);
    await fetchTaxRates();
  };

  const handleUpdate = async (id, values) => {
    await financeAPI.updateTaxRateDict(id, values);
    await fetchTaxRates();
  };

  const handleDelete = async (id) => {
    await financeAPI.deleteTaxRateDict(id);
    await fetchTaxRates();
  };

  return (
    <CRUDTable
      title="税率字典"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        {
          title: "税率",
          dataIndex: "rate",
          key: "rate",
          width: 120,
          render: (value) => <Tag color="blue">{`${Number(value || 0) * 100}%`}</Tag>,
        },
        { title: "标签", dataIndex: "label", key: "label", width: 160 },
        { title: "排序", dataIndex: "sortOrder", key: "sortOrder", width: 120 },
        {
          title: "启用状态",
          dataIndex: "isActive",
          key: "isActive",
          width: 120,
          render: (value) => (value ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>),
        },
      ]}
      dataSource={taxRates}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[
        { name: "label", label: "标签" },
      ]}
      formFields={[
        {
          name: "rate",
          label: "税率",
          rules: [{ required: true, message: "请输入税率" }],
          component: <InputNumber min={0} max={1} step={0.01} precision={2} style={{ width: "100%" }} />,
        },
        {
          name: "label",
          label: "标签",
          rules: [{ required: true, message: "请输入标签" }],
          component: <Input placeholder="例如：13%" maxLength={32} />,
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
        rate: record.rate == null ? null : Number(record.rate),
        isActive: Boolean(record.isActive),
      })}
      transformValues={(values) => ({
        ...values,
        rate: values.rate == null ? null : Number(values.rate),
        label: values.label?.trim(),
        sortOrder: Number(values.sortOrder),
        isActive: Boolean(values.isActive),
      })}
    />
  );
}
