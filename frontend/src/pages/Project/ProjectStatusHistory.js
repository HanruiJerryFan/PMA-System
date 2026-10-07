import React, { useCallback, useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import { DatePicker, Select } from "antd";
import { useSearchParams } from "react-router-dom";
import CRUDTable from "../../components/Common/CRUDTable";
import { permissionAPI, projectAPI } from "../../api/modules";
import { sortSelectItems } from "../../utils/selectSorting";
import { formatProjectOptionLabel } from "../../utils/projectLabels";

const { Option } = Select;

export default function ProjectStatusHistory() {
  const [searchParams] = useSearchParams();
  const projectIdFilter = searchParams.get("projectId") || searchParams.get("projectUuid") || "";
  const [records, setRecords] = useState([]);
  const [projects, setProjects] = useState([]);
  const [stages, setStages] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);

  const projectMap = useMemo(
    () => Object.fromEntries(projects.map((item) => [item.uuid, formatProjectOptionLabel(item)])),
    [projects]
  );
  const stageMap = useMemo(() => Object.fromEntries(stages.map((item) => [item.id, item.description])), [stages]);
  const userMap = useMemo(
    () => Object.fromEntries(users.map((item) => [item.id, item.realName || item.username || `User ${item.id}`])),
    [users]
  );
  const activeStages = useMemo(
    () => stages.filter((item) => item.isActive !== false),
    [stages]
  );

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [historyResponse, projectResponse, stageResponse, userResponse] = await Promise.all([
        projectAPI.getProjectStatusHistory(projectIdFilter || undefined),
        projectAPI.getProjects(),
        projectAPI.getStages(),
        permissionAPI.getUserOptions(),
      ]);
      setRecords(historyResponse.data || []);
      setProjects(projectResponse.data || []);
      setStages(stageResponse.data || []);
      setUsers(Array.isArray(userResponse) ? userResponse : []);
    } finally {
      setLoading(false);
    }
  }, [projectIdFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = async (values) => {
    await projectAPI.createProjectStatusHistory(values);
    await fetchData();
  };

  const handleUpdate = async (uuid, values) => {
    await projectAPI.updateProjectStatusHistory(uuid, values);
    await fetchData();
  };

  const handleDelete = async (uuid) => {
    await projectAPI.deleteProjectStatusHistory(uuid);
    await fetchData();
  };

  return (
    <CRUDTable
      title="项目记录"
      columns={[
        {
          title: "项目",
          dataIndex: "projectId",
          key: "projectId",
          width: 280,
          render: (value) => projectMap[value] || value || "-",
        },
        {
          title: "阶段",
          dataIndex: "stageId",
          key: "stageId",
          width: 180,
          render: (value, record) => record.stageName || stageMap[value] || "-",
        },
        {
          title: "创建时间",
          dataIndex: "createdAt",
          key: "createdAt",
          width: 200,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
        {
          title: "创建人",
          dataIndex: "createdBy",
          key: "createdBy",
          width: 160,
          render: (value) => userMap[value] || (value ?? "-"),
        },
      ]}
      dataSource={records}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      formFields={[
        {
          name: "projectId",
          label: "项目",
          rules: projectIdFilter ? [] : [{ required: true, message: "请选择项目" }],
          component: (
            <Select
              placeholder="请选择项目"
              showSearch
              optionFilterProp="children"
              disabled={Boolean(projectIdFilter)}
            >
              {sortSelectItems(projects, ["projectNumber"]).map((item) => (
                <Option key={item.uuid} value={item.uuid}>
                  {formatProjectOptionLabel(item)}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "stageId",
          label: "阶段",
          rules: [{ required: true, message: "请选择阶段" }],
          component: (
            <Select placeholder="请选择阶段">
              {sortSelectItems(activeStages, ["sortOrder","stageCode"]).map((item) => (
                <Option key={item.id} value={item.id}>
                  {item.description}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "createdAt",
          label: "创建时间",
          component: <DatePicker showTime style={{ width: "100%" }} />,
        },
        {
          name: "createdBy",
          label: "创建人",
          component: (
            <Select placeholder="请选择用户" allowClear showSearch optionFilterProp="children">
              {users.map((item) => (
                <Option key={item.id} value={item.id}>
                  {item.realName || item.username}
                </Option>
              ))}
            </Select>
          ),
        },
      ]}
      searchFields={[
        { name: "projectId", label: "项目 ID" },
        { name: "stageName", label: "阶段" },
      ]}
      rowKey="uuid"
      mapRecordToFormValues={(record) => ({
        ...record,
        createdAt: record.createdAt ? dayjs(record.createdAt) : null,
      })}
      transformValues={(values) => ({
        ...values,
        projectId: values.projectId || projectIdFilter,
        createdAt: values.createdAt ? values.createdAt.toISOString() : null,
        createdBy: values.createdBy || null,
      })}
    />
  );
}
