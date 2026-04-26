import React, { useEffect, useState } from "react";
import { Button, Input, Modal, Select, Tag, message } from "antd";
import { LockOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { getCurrentUser } from "../../api/auth";
import { permissionAPI } from "../../api/modules";

const { Option } = Select;

const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "启用", color: "success" },
  { value: "DISABLED", label: "禁用", color: "warning" },
  { value: "CLOSED", label: "关闭", color: "default" },
];

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await permissionAPI.getUsers();
      setUsers(response || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
    getCurrentUser().then(setCurrentUser).catch(() => setCurrentUser(null));
  }, []);

  const handleCreate = async (values) => {
    await permissionAPI.createUser(values);
    await fetchUsers();
  };

  const handleUpdate = async (id, values) => {
    await permissionAPI.updateUser(id, values);
    await fetchUsers();
  };

  const handleDelete = async (id) => {
    await permissionAPI.deleteUser(id);
    await fetchUsers();
  };

  const handleResetPassword = async (record) => {
    if (!currentUser?.id) {
      message.error("当前登录用户信息未就绪，请稍后再试");
      return;
    }
    try {
      const result = await permissionAPI.resetUserPassword(record.id, currentUser?.id);
      const temporaryPassword = result?.temporaryPassword || result?.data?.temporaryPassword;
      Modal.success({
        title: "临时密码已生成",
        content: (
          <div>
            <p>用户：{record.realName || record.username}</p>
            <p>临时密码：<strong>{temporaryPassword || "-"}</strong></p>
            <p>该用户下次登录后将被强制修改密码。</p>
          </div>
        ),
      });
      await fetchUsers();
    } catch (error) {
      message.error(error?.message || "重置密码失败");
    }
  };

  const renderStatus = (value) => {
    const option = STATUS_OPTIONS.find((item) => item.value === value);
    return <Tag color={option?.color || "default"}>{option?.label || value || "-"}</Tag>;
  };

  const renderForcePasswordChange = (value) =>
    value ? <Tag color="processing">需改密</Tag> : <Tag color="default">正常</Tag>;

  return (
    <CRUDTable
      title="用户管理"
      columns={[
        {
          title: "重置密码",
          key: "resetPassword",
          width: 120,
          render: (_, record) => (
            <Button
              type="link"
              icon={<LockOutlined />}
              onClick={() => handleResetPassword(record)}
            >
              重置
            </Button>
          ),
        },
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        { title: "用户名", dataIndex: "username", key: "username", width: 180 },
        { title: "真实姓名", dataIndex: "realName", key: "realName", width: 180 },
        { title: "状态", dataIndex: "status", key: "status", width: 120, render: renderStatus },
        {
          title: "首次登录改密",
          dataIndex: "forcePasswordChange",
          key: "forcePasswordChange",
          width: 140,
          render: renderForcePasswordChange,
        },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 180,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
        {
          title: "更新时间",
          dataIndex: "updateTime",
          key: "updateTime",
          width: 180,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
      ]}
      dataSource={users}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      formFields={[
        {
          name: "username",
          label: "用户名",
          rules: [{ required: true, message: "请输入用户名" }],
          component: <Input placeholder="请输入用户名" />,
        },
        {
          name: "password",
          label: "密码",
          rules: ({ editingRecord }) =>
            editingRecord ? [] : [{ required: true, message: "请输入密码" }],
          component: ({ editingRecord }) => (
            <Input.Password placeholder={editingRecord ? "留空表示不修改密码" : "请输入密码"} autoComplete="new-password" />
          ),
        },
        {
          name: "realName",
          label: "真实姓名",
          rules: [{ required: true, message: "请输入真实姓名" }],
          component: <Input placeholder="请输入真实姓名" />,
        },
        {
          name: "status",
          label: "状态",
          rules: [{ required: true, message: "请选择状态" }],
          component: (
            <Select placeholder="请选择状态">
              {STATUS_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          ),
        },
      ]}
      searchFields={[
        { name: "username", label: "用户名" },
        { name: "realName", label: "真实姓名" },
        { name: "status", label: "状态" },
      ]}
      mapRecordToFormValues={(record) => ({
        ...record,
        password: undefined,
      })}
      rowKey="id"
      transformValues={(values, editingRecord) => {
        if (!editingRecord && !values.password) {
          throw new Error("新增用户必须填写密码");
        }
        const payload = {
          ...values,
          status: values.status || "ACTIVE",
          forcePasswordChange: editingRecord?.forcePasswordChange ?? false,
        };
        if (editingRecord && !values.password) {
          delete payload.password;
        }
        return payload;
      }}
    />
  );
}
