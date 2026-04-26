import React, { useEffect, useState } from "react";
import { Input, Switch, Tag } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { projectAPI } from "../../api/modules";

export default function StageManagement() {
  const [stages, setStages] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchStages = async () => {
    setLoading(true);
    try {
      const response = await projectAPI.getStages();
      setStages(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStages();
  }, []);

  const handleCreate = async (values) => {
    await projectAPI.createStage(values);
    await fetchStages();
  };

  const handleUpdate = async (id, values) => {
    await projectAPI.updateStage(id, values);
    await fetchStages();
  };

  const handleDelete = async (id) => {
    await projectAPI.deleteStage(id);
    await fetchStages();
  };

  return (
    <CRUDTable
      title="项目阶段"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        { title: "阶段编码", dataIndex: "stageCode", key: "stageCode", width: 120 },
        { title: "阶段说明", dataIndex: "description", key: "description", width: 260 },
        { title: "排序", dataIndex: "sortOrder", key: "sortOrder", width: 120 },
        {
          title: "启用状态",
          dataIndex: "isActive",
          key: "isActive",
          width: 120,
          render: (value) => (value ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>),
        },
      ]}
      dataSource={stages}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[
        { name: "stageCode", label: "阶段编码" },
        { name: "description", label: "阶段说明" },
      ]}
      formFields={[
        {
          name: "stageCode",
          label: "阶段编码",
          rules: [
            { required: true, message: "请输入阶段编码" },
            { pattern: /^\d+\.\d+$/, message: "格式示例：1.1" },
          ],
          component: <Input placeholder="请输入阶段编码" />,
        },
        {
          name: "description",
          label: "阶段说明",
          rules: [{ required: true, message: "请输入阶段说明" }],
          component: <Input placeholder="请输入阶段说明" />,
        },
        {
          name: "sortOrder",
          label: "排序",
          rules: [{ required: true, message: "请输入排序值" }],
          component: <Input type="number" placeholder="请输入排序值" />,
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
        sortOrder: values.sortOrder === undefined || values.sortOrder === null || values.sortOrder === ""
          ? null
          : Number(values.sortOrder),
        isActive: Boolean(values.isActive),
      })}
    />
  );
}
