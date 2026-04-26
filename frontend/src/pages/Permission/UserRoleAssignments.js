import React, { useEffect, useMemo, useState } from "react";
import { Button, Card, Select, Space, Table, Tag, Typography, message } from "antd";
import { permissionAPI } from "../../api/modules";

const { Option } = Select;
const { Title, Text } = Typography;

export default function UserRoleAssignments() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [assignedRoleIds, setAssignedRoleIds] = useState([]);
  const [draftRoleIds, setDraftRoleIds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const roleMap = useMemo(
    () => Object.fromEntries(roles.map((role) => [role.id, role])),
    [roles]
  );

  useEffect(() => {
    async function fetchOptions() {
      setLoading(true);
      try {
        const [userResponse, roleResponse] = await Promise.all([
          permissionAPI.getUserOptions(),
          permissionAPI.getRoles(),
        ]);
        const userData = userResponse || [];
        setUsers(userData);
        setRoles(roleResponse || []);
        if (userData.length > 0) {
          setSelectedUserId((current) => current ?? userData[0].id);
        }
      } finally {
        setLoading(false);
      }
    }

    fetchOptions();
  }, []);

  useEffect(() => {
    async function fetchAssignments() {
      if (!selectedUserId) {
        setAssignedRoleIds([]);
        setDraftRoleIds([]);
        return;
      }
      setLoading(true);
      try {
        const response = await permissionAPI.getUserRoles(selectedUserId);
        const roleIds = (response || []).map((item) => item.roleId);
        setAssignedRoleIds(roleIds);
        setDraftRoleIds(roleIds);
      } finally {
        setLoading(false);
      }
    }

    fetchAssignments();
  }, [selectedUserId]);

  const selectedUser = users.find((item) => item.id === selectedUserId);

  const assignmentRows = assignedRoleIds.map((roleId) => ({
    key: roleId,
    roleId,
    roleName: roleMap[roleId]?.roleName || `角色 ${roleId}`,
  }));

  const saveAssignments = async () => {
    if (!selectedUserId) {
      message.warning("请先选择用户");
      return;
    }

    const toAdd = draftRoleIds.filter((roleId) => !assignedRoleIds.includes(roleId));
    const toRemove = assignedRoleIds.filter((roleId) => !draftRoleIds.includes(roleId));

    setSaving(true);
    try {
      await Promise.all(toAdd.map((roleId) => permissionAPI.assignUserRole({ userId: selectedUserId, roleId })));
      await Promise.all(toRemove.map((roleId) => permissionAPI.removeUserRole(selectedUserId, roleId)));
      setAssignedRoleIds(draftRoleIds);
      message.success("用户角色已更新");
    } catch (error) {
      message.error(error?.message || "更新用户角色失败");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card loading={loading}>
        <Space direction="vertical" size={12} style={{ width: "100%" }}>
          <Title level={4} style={{ margin: 0 }}>
            用户角色分配
          </Title>
          <Text type="secondary">选择一个用户，调整角色后保存。新的角色权限会在下次登录时生效。</Text>
          <Space wrap>
            <Select
              style={{ width: 320 }}
              placeholder="请选择用户"
              value={selectedUserId}
              onChange={setSelectedUserId}
              showSearch
              optionFilterProp="children"
            >
              {users.map((user) => (
                <Option key={user.id} value={user.id}>
                  {(user.realName || user.username) + ` (${user.username})`}
                </Option>
              ))}
            </Select>
            <Select
              mode="multiple"
              style={{ minWidth: 420 }}
              placeholder="请选择角色"
              value={draftRoleIds}
              onChange={setDraftRoleIds}
              optionFilterProp="children"
            >
              {roles.map((role) => (
                <Option key={role.id} value={role.id}>
                  {role.roleName}
                </Option>
              ))}
            </Select>
            <Button type="primary" onClick={saveAssignments} loading={saving}>
              保存分配
            </Button>
          </Space>
          {selectedUser ? (
            <Space wrap>
              <Tag color="blue">{selectedUser.realName || selectedUser.username}</Tag>
              <Tag>{selectedUser.username}</Tag>
              <Tag color={selectedUser.status === "ACTIVE" ? "success" : "default"}>{selectedUser.status}</Tag>
            </Space>
          ) : null}
        </Space>
      </Card>

      <Card title="当前已分配角色">
        <Table
          rowKey="key"
          pagination={false}
          dataSource={assignmentRows}
          columns={[
            { title: "角色 ID", dataIndex: "roleId", key: "roleId", width: 120 },
            { title: "角色名称", dataIndex: "roleName", key: "roleName" },
          ]}
          locale={{ emptyText: "暂无已分配角色" }}
        />
      </Card>
    </Space>
  );
}
