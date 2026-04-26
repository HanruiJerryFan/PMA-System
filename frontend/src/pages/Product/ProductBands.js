import React, { useEffect, useState } from "react";
import { Input, InputNumber, Switch } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { productAPI } from "../../api/modules";

export default function ProductBands() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchItems = async () => {
    setLoading(true);
    try {
      const response = await productAPI.getProductBands();
      setItems(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchItems();
  }, []);

  const handleCreate = async (values) => {
    await productAPI.createProductBand(values);
    await fetchItems();
  };

  const handleUpdate = async (id, values) => {
    await productAPI.updateProductBand(id, values);
    await fetchItems();
  };

  const handleDelete = async (id) => {
    await productAPI.deleteProductBand(id);
    await fetchItems();
  };

  return (
    <CRUDTable
      title="物料频段"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 120 },
        { title: "编码", dataIndex: "code", key: "code", width: 120 },
        { title: "说明", dataIndex: "description", key: "description", width: 260 },
        { title: "排序", dataIndex: "sortOrder", key: "sortOrder", width: 120 },
        { title: "启用状态", dataIndex: "isActive", key: "isActive", width: 100, render: (value) => (value ? "启用" : "停用") },
      ]}
      dataSource={items}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[
        { name: "code", label: "编码" },
        { name: "description", label: "说明" },
      ]}
      formFields={[
        { name: "code", label: "编码", rules: [{ required: true, message: "请输入编码" }, { max: 3, message: "编码长度最多为 3 位" }], component: <Input /> },
        { name: "description", label: "说明", rules: [{ required: true, message: "请输入说明" }], component: <Input /> },
        { name: "sortOrder", label: "排序", rules: [{ required: true, message: "请输入排序" }], component: <InputNumber min={1} style={{ width: "100%" }} /> },
        { name: "isActive", label: "启用状态", valuePropName: "checked", component: <Switch checkedChildren="启用" unCheckedChildren="停用" /> },
      ]}
      mapRecordToFormValues={(record) => ({ ...record, isActive: Boolean(record.isActive) })}
      transformValues={(values) => ({ ...values, isActive: Boolean(values.isActive) })}
    />
  );
}
