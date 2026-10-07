import React, { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import { Cascader, DatePicker, Input, Select } from "antd";
import { useNavigate } from "react-router-dom";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI, permissionAPI, projectAPI, regionAPI } from "../../api/modules";
import { buildRegionOptions, buildRegionPath, formatRegionLabel } from "../../utils/regions";
import { sortSelectItems } from "../../utils/selectSorting";

const { Option } = Select;

export default function ProjectInfo() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [projectTypes, setProjectTypes] = useState([]);
  const [users, setUsers] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);

  const userMap = useMemo(
    () => Object.fromEntries(users.map((item) => [item.id, item.realName || item.username || `用户${item.id}`])),
    [users]
  );

  const regionById = useMemo(
    () => Object.fromEntries(regions.map((item) => [item.id, item])),
    [regions]
  );

  const regionByAreaCode = useMemo(
    () => Object.fromEntries(regions.map((item) => [item.areaCode, item])),
    [regions]
  );

  const regionMap = useMemo(
    () => Object.fromEntries(regions.map((item) => [item.id, formatRegionLabel(item)])),
    [regions]
  );

  const regionOptions = useMemo(() => buildRegionOptions(regions), [regions]);
  const activeProjectTypes = useMemo(
    () => projectTypes.filter((item) => item.isActive !== false),
    [projectTypes]
  );

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const response = await projectAPI.getProjects();
      setProjects(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  const fetchOptions = async () => {
    const [customerResponse, typeResponse, userResponse, regionResponse] = await Promise.all([
      customerAPI.getCustomerOptions(),
      projectAPI.getProjectTypes(),
      permissionAPI.getUserOptions(),
      regionAPI.getRegionOptions(),
    ]);

    setCustomers(customerResponse.data || []);
    setProjectTypes(typeResponse.data || []);
    setUsers(Array.isArray(userResponse) ? userResponse : []);
    setRegions(regionResponse.data || []);
  };

  useEffect(() => {
    fetchProjects();
    fetchOptions();
  }, []);

  const handleCreate = async (values) => {
    await projectAPI.createProject(values);
    await fetchProjects();
  };

  const handleUpdate = async (uuid, values) => {
    await projectAPI.updateProject(uuid, values);
    await fetchProjects();
  };

  const handleDelete = async (uuid) => {
    await projectAPI.deleteProject(uuid);
    await fetchProjects();
  };

  const openProjectDashboard = (record) => {
    navigate(`/project/dashboard?projectUuid=${record.uuid}`);
  };

  const userOptions = users.map((item) => (
    <Option key={item.id} value={item.id}>
      {item.realName || item.username}
    </Option>
  ));

  return (
    <CRUDTable
      title="项目管理"
      columns={[
        { title: "项目编号", dataIndex: "projectNumber", key: "projectNumber", width: 160 },
        { title: "项目名称", dataIndex: "projectName", key: "projectName", width: 260 },
        {
          title: "地区",
          dataIndex: "regionId",
          key: "regionId",
          width: 220,
          render: (value) => regionMap[value] || "-",
        },
        {
          title: "负责人",
          dataIndex: "managerId",
          key: "managerId",
          width: 180,
          render: (value) => userMap[value] || "-",
        },
      ]}
      dataSource={projects}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      showActions={false}
      formFields={[
        {
          name: "projectNumber",
          label: "项目编号",
          rules: [
            { required: true, message: "请输入项目编号" },
            { pattern: /^[A-Z]{2}-\d{4}$/, message: "格式应为两位大写字母-四位数字，例如 DL-2303" },
          ],
          component: <Input placeholder="例如 DL-2303" />,
        },
        {
          name: "projectName",
          label: "项目名称",
          rules: [{ required: true, message: "请输入项目名称" }],
          component: <Input placeholder="请输入项目名称" />,
        },
        {
          name: "customerId",
          label: "客户",
          rules: [{ required: true, message: "请选择客户" }],
          component: (
            <Select placeholder="请选择客户" showSearch optionFilterProp="children">
              {sortSelectItems(customers, ["customerCode"]).map((item) => (
                <Option key={item.uuid} value={item.uuid}>
                  {item.customerName}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "managerId",
          label: "负责人",
          rules: [{ required: true, message: "请选择负责人" }],
          component: (
            <Select placeholder="请选择负责人" showSearch optionFilterProp="children">
              {userOptions}
            </Select>
          ),
        },
        {
          name: "participant1UserId",
          label: "参与人 1",
          component: (
            <Select placeholder="请选择参与人" allowClear showSearch optionFilterProp="children">
              {userOptions}
            </Select>
          ),
        },
        {
          name: "participant2UserId",
          label: "参与人 2",
          component: (
            <Select placeholder="请选择参与人" allowClear showSearch optionFilterProp="children">
              {userOptions}
            </Select>
          ),
        },
        {
          name: "participant3UserId",
          label: "参与人 3",
          component: (
            <Select placeholder="请选择参与人" allowClear showSearch optionFilterProp="children">
              {userOptions}
            </Select>
          ),
        },
        {
          name: "regionId",
          label: "地区",
          rules: [{ required: true, message: "请选择地区" }],
          component: <Cascader options={regionOptions} placeholder="请选择省 / 市 / 区" changeOnSelect />,
        },
        {
          name: "projectTypeId",
          label: "项目类型",
          rules: [{ required: true, message: "请选择项目类型" }],
          component: (
            <Select placeholder="请选择项目类型">
              {sortSelectItems(activeProjectTypes, ["sortOrder","code"]).map((item) => (
                <Option key={item.id} value={item.id}>
                  {item.code} {item.name}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "planStartTime",
          label: "计划开始",
          component: <DatePicker showTime style={{ width: "100%" }} />,
        },
        {
          name: "planEndTime",
          label: "计划结束",
          component: <DatePicker showTime style={{ width: "100%" }} />,
        },
        {
          name: "warrantyUntil",
          label: "质保截止",
          component: <DatePicker style={{ width: "100%" }} />,
        },
        {
          name: "salesContractAttachmentUuid",
          label: "销售合同附件 UUID",
          component: <Input placeholder="请输入关联附件 UUID" />,
        },
      ]}
      searchFields={[
        { name: "projectNumber", label: "项目编号" },
        { name: "projectName", label: "项目名称" },
        { name: "customerId", label: "客户 ID" },
      ]}
      rowKey="uuid"
      mapRecordToFormValues={(record) => ({
        ...record,
        regionId: record.regionId ? buildRegionPath(record.regionId, regionById, regionByAreaCode) : [],
        warrantyUntil: record.warrantyUntil ? dayjs(record.warrantyUntil) : null,
        planStartTime: record.planStartTime ? dayjs(record.planStartTime) : null,
        planEndTime: record.planEndTime ? dayjs(record.planEndTime) : null,
      })}
      transformValues={(values) => ({
        ...values,
        regionId: Array.isArray(values.regionId) ? values.regionId[values.regionId.length - 1] || null : values.regionId,
        participant1UserId: values.participant1UserId || null,
        participant2UserId: values.participant2UserId || null,
        participant3UserId: values.participant3UserId || null,
        salesContractAttachmentUuid: values.salesContractAttachmentUuid || null,
        warrantyUntil: values.warrantyUntil ? values.warrantyUntil.format("YYYY-MM-DD") : null,
        planStartTime: values.planStartTime ? values.planStartTime.toISOString() : null,
        planEndTime: values.planEndTime ? values.planEndTime.toISOString() : null,
      })}
      tableProps={{
        onRow: (record) => ({
          onClick: () => openProjectDashboard(record),
          style: { cursor: "pointer" },
        }),
      }}
    />
  );
}
