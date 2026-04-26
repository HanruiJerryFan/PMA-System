import React, { useEffect, useState } from "react";
import { Input, InputNumber, Switch } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { productAPI } from "../../api/modules";

export default function ProductCategories() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const response = await productAPI.getProductCategories();
      setCategories(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const handleCreate = async (values) => {
    await productAPI.createProductCategory(values);
    await fetchCategories();
  };

  const handleUpdate = async (id, values) => {
    await productAPI.updateProductCategory(id, values);
    await fetchCategories();
  };

  const handleDelete = async (id) => {
    await productAPI.deleteProductCategory(id);
    await fetchCategories();
  };

  return (
    <CRUDTable
      title="物料大类"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 120 },
        { title: "编码", dataIndex: "code", key: "code", width: 120 },
        { title: "名称", dataIndex: "name", key: "name", width: 220 },
        { title: "排序", dataIndex: "sortOrder", key: "sortOrder", width: 120 },
        { title: "启用状态", dataIndex: "isActive", key: "isActive", width: 100, render: (value) => (value ? "启用" : "停用") },
      ]}
      dataSource={categories}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[
        { name: "code", label: "编码" },
        { name: "name", label: "名称" },
      ]}
      formFields={[
        { name: "code", label: "编码", rules: [{ required: true, message: "请输入编码" }, { len: 2, message: "编码长度必须为 2 位" }], component: <Input /> },
        { name: "name", label: "名称", rules: [{ required: true, message: "请输入名称" }], component: <Input /> },
        { name: "sortOrder", label: "排序", rules: [{ required: true, message: "请输入排序" }], component: <InputNumber min={1} style={{ width: "100%" }} /> },
        { name: "isActive", label: "启用状态", valuePropName: "checked", component: <Switch checkedChildren="启用" unCheckedChildren="停用" /> },
      ]}
      mapRecordToFormValues={(record) => ({ ...record, isActive: Boolean(record.isActive) })}
      transformValues={(values) => ({ ...values, isActive: Boolean(values.isActive) })}
    />
  );
}
