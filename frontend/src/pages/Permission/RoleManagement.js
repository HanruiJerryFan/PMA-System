import React, { useEffect, useState } from "react";
import { Input, Tag } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { permissionAPI } from "../../api/modules";

export default function RoleManagement() {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const response = await permissionAPI.getRoles();
      setRoles(response || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRoles();
  }, []);

  const handleCreate = async (values) => {
    await permissionAPI.createRole(values);
    await fetchRoles();
  };

  const handleUpdate = async (id, values) => {
    await permissionAPI.updateRole(id, values);
    await fetchRoles();
  };

  const handleDelete = async (id) => {
    await permissionAPI.deleteRole(id);
    await fetchRoles();
  };

  return (
    <CRUDTable
      title="角色管理"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        {
          title: "角色名称",
          dataIndex: "roleName",
          key: "roleName",
          width: 220,
          render: (value) => <Tag color="blue">{value}</Tag>,
        },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 180,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
      ]}
      dataSource={roles}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      formFields={[
        {
          name: "roleName",
          label: "角色名称",
          rules: [{ required: true, message: "请输入角色名称" }],
          component: <Input placeholder="请输入角色名称" />,
        },
      ]}
      searchFields={[{ name: "roleName", label: "角色名称" }]}
      rowKey="id"
    />
  );
}
