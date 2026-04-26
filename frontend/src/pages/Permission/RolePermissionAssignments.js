import React, { useEffect, useMemo, useState } from "react";
import { Button, Card, Select, Space, Table, Tag, Typography, message } from "antd";
import { permissionAPI } from "../../api/modules";

const { Option } = Select;
const { Title, Text } = Typography;

export default function RolePermissionAssignments() {
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [selectedRoleId, setSelectedRoleId] = useState(null);
  const [assignedPermissionIds, setAssignedPermissionIds] = useState([]);
  const [draftPermissionIds, setDraftPermissionIds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const permissionMap = useMemo(
    () => Object.fromEntries(permissions.map((permission) => [permission.id, permission])),
    [permissions]
  );

  useEffect(() => {
    async function fetchOptions() {
      setLoading(true);
      try {
        const [roleResponse, permissionResponse] = await Promise.all([
          permissionAPI.getRoles(),
          permissionAPI.getPermissions(),
        ]);
        const roleData = roleResponse || [];
        setRoles(roleData);
        setPermissions(permissionResponse || []);
        if (roleData.length > 0) {
          setSelectedRoleId((current) => current ?? roleData[0].id);
        }
      } finally {
        setLoading(false);
      }
    }

    fetchOptions();
  }, []);

  useEffect(() => {
    async function fetchAssignments() {
      if (!selectedRoleId) {
        setAssignedPermissionIds([]);
        setDraftPermissionIds([]);
        return;
      }
      setLoading(true);
      try {
        const response = await permissionAPI.getRolePermissions(selectedRoleId);
        const permissionIds = (response || []).map((item) => item.permissionId);
        setAssignedPermissionIds(permissionIds);
        setDraftPermissionIds(permissionIds);
      } finally {
        setLoading(false);
      }
    }

    fetchAssignments();
  }, [selectedRoleId]);

  const selectedRole = roles.find((item) => item.id === selectedRoleId);

  const assignmentRows = assignedPermissionIds.map((permissionId) => ({
    key: permissionId,
    permissionId,
    permissionName: permissionMap[permissionId]?.permissionName || `权限 ${permissionId}`,
  }));

  const saveAssignments = async () => {
    if (!selectedRoleId) {
      message.warning("请先选择角色");
      return;
    }

    const toAdd = draftPermissionIds.filter((permissionId) => !assignedPermissionIds.includes(permissionId));
    const toRemove = assignedPermissionIds.filter((permissionId) => !draftPermissionIds.includes(permissionId));

    setSaving(true);
    try {
      await Promise.all(
        toAdd.map((permissionId) => permissionAPI.assignRolePermission({ roleId: selectedRoleId, permissionId }))
      );
      await Promise.all(
        toRemove.map((permissionId) => permissionAPI.removeRolePermission(selectedRoleId, permissionId))
      );
      setAssignedPermissionIds(draftPermissionIds);
      message.success("角色权限已更新");
    } catch (error) {
      message.error(error?.message || "更新角色权限失败");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card loading={loading}>
        <Space direction="vertical" size={12} style={{ width: "100%" }}>
          <Title level={4} style={{ margin: 0 }}>
            角色权限分配
          </Title>
          <Text type="secondary">选择一个角色，勾选权限项后保存。权限编码会在登录时装载到当前账号。</Text>
          <Space wrap>
            <Select
              style={{ width: 320 }}
              placeholder="请选择角色"
              value={selectedRoleId}
              onChange={setSelectedRoleId}
              showSearch
              optionFilterProp="children"
            >
              {roles.map((role) => (
                <Option key={role.id} value={role.id}>
                  {role.roleName}
                </Option>
              ))}
            </Select>
            <Select
              mode="multiple"
              style={{ minWidth: 460 }}
              placeholder="请选择权限项"
              value={draftPermissionIds}
              onChange={setDraftPermissionIds}
              optionFilterProp="children"
            >
              {permissions.map((permission) => (
                <Option key={permission.id} value={permission.id}>
                  {permission.permissionName}
                </Option>
              ))}
            </Select>
            <Button type="primary" onClick={saveAssignments} loading={saving}>
              保存分配
            </Button>
          </Space>
          {selectedRole ? <Tag color="blue">{selectedRole.roleName}</Tag> : null}
        </Space>
      </Card>

      <Card title="当前已分配权限">
        <Table
          rowKey="key"
          pagination={false}
          dataSource={assignmentRows}
          columns={[
            { title: "权限 ID", dataIndex: "permissionId", key: "permissionId", width: 140 },
            { title: "权限名称", dataIndex: "permissionName", key: "permissionName" },
          ]}
          locale={{ emptyText: "暂无已分配权限" }}
        />
      </Card>
    </Space>
  );
}
