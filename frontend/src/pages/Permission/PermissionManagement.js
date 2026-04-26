import React, { useEffect, useState } from "react";
import { Input, Tag } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { permissionAPI } from "../../api/modules";

export default function PermissionManagement() {
  const [permissions, setPermissions] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchPermissions = async () => {
    setLoading(true);
    try {
      const response = await permissionAPI.getPermissions();
      setPermissions(response || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPermissions();
  }, []);

  const handleCreate = async (values) => {
    await permissionAPI.createPermission(values);
    await fetchPermissions();
  };

  const handleUpdate = async (id, values) => {
    await permissionAPI.updatePermission(id, values);
    await fetchPermissions();
  };

  const handleDelete = async (id) => {
    await permissionAPI.deletePermission(id);
    await fetchPermissions();
  };

  return (
    <CRUDTable
      title="权限项管理"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        {
          title: "权限编码",
          dataIndex: "permissionCode",
          key: "permissionCode",
          width: 260,
          render: (value) => <Tag color="blue">{value}</Tag>,
        },
        {
          title: "权限名称",
          dataIndex: "permissionName",
          key: "permissionName",
          width: 240,
          render: (value) => <Tag color="green">{value}</Tag>,
        },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 180,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
      ]}
      dataSource={permissions}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      formFields={[
        {
          name: "permissionCode",
          label: "权限编码",
          rules: [
            { required: true, message: "请输入权限编码" },
            { pattern: /^[a-z][a-z0-9._-]*$/, message: "仅支持小写字母、数字、点、横线和下划线" },
          ],
          component: <Input placeholder="例如：finance.access" />,
        },
        {
          name: "permissionName",
          label: "权限名称",
          rules: [{ required: true, message: "请输入权限名称" }],
          component: <Input placeholder="例如：财务模块访问" />,
        },
      ]}
      searchFields={[
        { name: "permissionCode", label: "权限编码" },
        { name: "permissionName", label: "权限名称" },
      ]}
      rowKey="id"
    />
  );
}
