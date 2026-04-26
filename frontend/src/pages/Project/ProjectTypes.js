import React, { useEffect, useState } from "react";
import { Input, Switch, Tag } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { projectAPI } from "../../api/modules";

export default function ProjectTypes() {
  const [projectTypes, setProjectTypes] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchProjectTypes = async () => {
    setLoading(true);
    try {
      const response = await projectAPI.getProjectTypes();
      setProjectTypes(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjectTypes();
  }, []);

  const handleCreate = async (values) => {
    await projectAPI.createProjectType(values);
    await fetchProjectTypes();
  };

  const handleUpdate = async (id, values) => {
    await projectAPI.updateProjectType(id, values);
    await fetchProjectTypes();
  };

  const handleDelete = async (id) => {
    await projectAPI.deleteProjectType(id);
    await fetchProjectTypes();
  };

  return (
    <CRUDTable
      title="项目类型"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        {
          title: "编码",
          dataIndex: "code",
          key: "code",
          width: 120,
          render: (value) => <Tag color="blue">{value}</Tag>,
        },
        { title: "名称", dataIndex: "name", key: "name", width: 220 },
        {
          title: "启用状态",
          dataIndex: "isActive",
          key: "isActive",
          width: 120,
          render: (value) => (value ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>),
        },
      ]}
      dataSource={projectTypes}
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
        {
          name: "code",
          label: "编码",
          rules: [{ required: true, message: "请输入编码" }],
          component: <Input placeholder="请输入编码" maxLength={32} />,
        },
        {
          name: "name",
          label: "名称",
          rules: [{ required: true, message: "请输入名称" }],
          component: <Input placeholder="请输入名称" maxLength={64} />,
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
        code: values.code?.trim(),
        name: values.name?.trim(),
        isActive: Boolean(values.isActive),
      })}
    />
  );
}
