import React, { useEffect, useState } from "react";
import { Input } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { inventoryAPI } from "../../api/modules";

export default function Warehouses() {
  const [warehouses, setWarehouses] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchWarehouses = async () => {
    setLoading(true);
    try {
      const response = await inventoryAPI.getWarehouses();
      setWarehouses(response.data || response || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWarehouses();
  }, []);

  const handleCreate = async (values) => {
    await inventoryAPI.createWarehouse(values);
    await fetchWarehouses();
  };

  const handleUpdate = async (id, values) => {
    await inventoryAPI.updateWarehouse(id, values);
    await fetchWarehouses();
  };

  const handleDelete = async (id) => {
    await inventoryAPI.deleteWarehouse(id);
    await fetchWarehouses();
  };

  return (
    <CRUDTable
      title="仓库管理"
      columns={[
        { title: "仓库编码", dataIndex: "warehouseCode", key: "warehouseCode", width: 140 },
        { title: "仓库名称", dataIndex: "warehouseName", key: "warehouseName", width: 220 },
        { title: "地址", dataIndex: "warehouseAddress", key: "warehouseAddress", width: 240 },
        { title: "联系人", dataIndex: "contactName", key: "contactName", width: 140 },
        { title: "联系电话", dataIndex: "contactPhone", key: "contactPhone", width: 140 },
        { title: "备注", dataIndex: "remark", key: "remark", width: 220 },
      ]}
      dataSource={warehouses}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[
        { name: "warehouseCode", label: "仓库编码" },
        { name: "warehouseName", label: "仓库名称" },
      ]}
      formFields={[
        {
          name: "warehouseCode",
          label: "仓库编码",
          rules: [{ required: true, message: "请输入仓库编码" }],
          component: <Input placeholder="请输入仓库编码" />,
        },
        {
          name: "warehouseName",
          label: "仓库名称",
          rules: [{ required: true, message: "请输入仓库名称" }],
          component: <Input placeholder="请输入仓库名称" />,
        },
        { name: "warehouseAddress", label: "地址", component: <Input placeholder="请输入仓库地址" /> },
        { name: "contactName", label: "联系人", component: <Input placeholder="请输入联系人" /> },
        { name: "contactPhone", label: "联系电话", component: <Input placeholder="请输入联系电话" /> },
        { name: "remark", label: "备注", component: <Input.TextArea rows={3} placeholder="请输入备注" /> },
      ]}
    />
  );
}
