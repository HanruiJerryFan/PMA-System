import React, { useEffect, useState } from "react";
import { Input, Switch, Tag } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI } from "../../api/modules";

export default function CustomerIndustries() {
  const [industries, setIndustries] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchIndustries = async () => {
    setLoading(true);
    try {
      const response = await customerAPI.getCustomerIndustryDicts();
      setIndustries(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIndustries();
  }, []);

  const handleCreate = async (values) => {
    await customerAPI.createCustomerIndustryDict(values);
    await fetchIndustries();
  };

  const handleUpdate = async (id, values) => {
    await customerAPI.updateCustomerIndustryDict(id, values);
    await fetchIndustries();
  };

  const handleDelete = async (id) => {
    await customerAPI.deleteCustomerIndustryDict(id);
    await fetchIndustries();
  };

  return (
    <CRUDTable
      title="行业字典"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        { title: "行业名称", dataIndex: "industryName", key: "industryName", width: 260 },
        {
          title: "启用状态",
          dataIndex: "isActive",
          key: "isActive",
          width: 120,
          render: (value) => (value ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>),
        },
      ]}
      dataSource={industries}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[{ name: "industryName", label: "行业名称" }]}
      formFields={[
        {
          name: "industryName",
          label: "行业名称",
          rules: [{ required: true, message: "请输入行业名称" }],
          component: <Input placeholder="请输入行业名称" />,
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
        isActive: Boolean(values.isActive),
      })}
    />
  );
}
