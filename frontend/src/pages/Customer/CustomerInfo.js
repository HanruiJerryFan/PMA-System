import React, { useEffect, useMemo, useState } from "react";
import { Button, Cascader, Input, Select, Tag } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI, regionAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";
import { buildRegionOptions, buildRegionPath, formatRegionLabel } from "../../utils/regions";

const { Option } = Select;

const ACTIVITY_STATUS_OPTIONS = [
  { value: "ACTIVE", label: "活跃" },
  { value: "NORMAL", label: "正常" },
  { value: "INACTIVE", label: "不活跃" },
  { value: "DORMANT", label: "沉睡" },
];

export default function CustomerInfo() {
  const [customers, setCustomers] = useState([]);
  const [customerTypes, setCustomerTypes] = useState([]);
  const [customerIndustries, setCustomerIndustries] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);

  const customerTypeMap = useMemo(
    () => Object.fromEntries(customerTypes.map((item) => [item.id, `${item.typeCode} - ${item.typeName}`])),
    [customerTypes]
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

  const activityStatusMap = useMemo(
    () => Object.fromEntries(ACTIVITY_STATUS_OPTIONS.map((item) => [item.value, item.label])),
    []
  );

  const activeCustomerIndustries = useMemo(
    () => customerIndustries.filter((item) => item.isActive !== false),
    [customerIndustries]
  );

  const decoratedCustomers = useMemo(
    () =>
      customers.map((item) => ({
        ...item,
        customerTypeLabel: customerTypeMap[item.customerTypeId] || "-",
        regionLabel: regionMap[item.regionId] || "-",
        activityStatusLabel: activityStatusMap[item.activityStatus] || item.activityStatus || "-",
      })),
    [customers, customerTypeMap, regionMap, activityStatusMap]
  );

  const fetchCustomers = async () => {
    setLoading(true);
    try {
      const response = await customerAPI.getCustomers();
      setCustomers(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  const fetchDictionaries = async () => {
    const [typeResponse, industryResponse, regionResponse] = await Promise.all([
      customerAPI.getCustomerTypes(),
      customerAPI.getCustomerIndustryDicts(),
      regionAPI.getRegions(),
    ]);
    setCustomerTypes(typeResponse.data || []);
    setCustomerIndustries(industryResponse.data || []);
    setRegions(regionResponse.data || []);
  };

  useEffect(() => {
    fetchCustomers();
    fetchDictionaries();
  }, []);

  const handleCreate = async (values) => {
    await customerAPI.createCustomer(values);
    await fetchCustomers();
  };

  const handleUpdate = async (uuid, values) => {
    await customerAPI.updateCustomer(uuid, values);
    await fetchCustomers();
  };

  const handleDelete = async (uuid) => {
    await customerAPI.deleteCustomer(uuid);
    await fetchCustomers();
  };

  const handleExportExcel = (rows = decoratedCustomers) => {
    downloadExcel(
      `customers-${new Date().toISOString().slice(0, 10)}.xls`,
      "客户台账",
      [
        "客户编码",
        "客户名称",
        "客户类型",
        "地区",
        "行业",
        "活跃状态",
        "最后活跃时间",
        "注册地址",
        "办公地址",
        "税号",
        "开户行",
        "银行账号",
        "备注",
        "更新时间",
      ],
      rows.map((item) => [
        item.customerCode || "",
        item.customerName || "",
        item.customerTypeLabel || "",
        item.regionLabel || "",
        item.industry || "",
        item.activityStatusLabel || "",
        item.lastActiveAt ? new Date(item.lastActiveAt).toLocaleString() : "",
        item.registerAddress || "",
        item.officeAddress || "",
        item.taxIdentificationNumber || "",
        item.bankName || "",
        item.bankAccount || "",
        item.remark || "",
        item.updateTime ? new Date(item.updateTime).toLocaleString() : "",
      ])
    );
  };

  return (
    <CRUDTable
      title="客户管理"
      columns={[
        { title: "客户编码", dataIndex: "customerCode", key: "customerCode", width: 140 },
        { title: "客户名称", dataIndex: "customerName", key: "customerName", width: 220 },
        {
          title: "客户类型",
          dataIndex: "customerTypeId",
          key: "customerTypeId",
          width: 140,
          render: (value) => <Tag color="blue">{customerTypeMap[value] || "-"}</Tag>,
        },
        {
          title: "地区",
          dataIndex: "regionId",
          key: "regionId",
          width: 180,
          render: (value) => regionMap[value] || "-",
        },
        { title: "行业", dataIndex: "industry", key: "industry", width: 160 },
        {
          title: "活跃状态",
          dataIndex: "activityStatusLabel",
          key: "activityStatusLabel",
          width: 140,
          render: (value) => <Tag color="gold">{value || "-"}</Tag>,
        },
        {
          title: "最后活跃时间",
          dataIndex: "lastActiveAt",
          key: "lastActiveAt",
          width: 180,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
        { title: "注册地址", dataIndex: "registerAddress", key: "registerAddress", width: 220, ellipsis: true },
        { title: "办公地址", dataIndex: "officeAddress", key: "officeAddress", width: 220, ellipsis: true },
        { title: "税号", dataIndex: "taxIdentificationNumber", key: "taxIdentificationNumber", width: 180 },
        { title: "开户行", dataIndex: "bankName", key: "bankName", width: 180 },
        { title: "银行账号", dataIndex: "bankAccount", key: "bankAccount", width: 180 },
        { title: "备注", dataIndex: "remark", key: "remark", width: 220, ellipsis: true },
      ]}
      dataSource={decoratedCustomers}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      formFields={[
        {
          name: "customerCode",
          label: "客户编码",
          component: <Input placeholder="系统将根据地区和客户类型自动生成" disabled />,
        },
        {
          name: "customerName",
          label: "客户名称",
          rules: [{ required: true, message: "请输入客户名称" }],
          component: <Input placeholder="请输入客户名称" />,
        },
        {
          name: "customerTypeId",
          label: "客户类型",
          rules: [{ required: true, message: "请选择客户类型" }],
          component: (
            <Select placeholder="请选择客户类型">
              {customerTypes.map((item) => (
                <Option key={item.id} value={item.id}>
                  {item.typeCode} - {item.typeName}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "regionId",
          label: "地区",
          rules: [{ required: true, message: "请选择地区" }],
          component: (
            <Cascader options={regionOptions} placeholder="请选择省 / 市 / 区" changeOnSelect />
          ),
        },
        {
          name: "industry",
          label: "行业",
          component: (
            <Select placeholder="请选择行业" allowClear>
              {activeCustomerIndustries.map((item) => (
                <Option key={item.id} value={item.industryName}>
                  {item.industryName}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "registerAddress",
          label: "注册地址",
          component: <Input placeholder="请输入注册地址" />,
        },
        {
          name: "officeAddress",
          label: "办公地址",
          component: <Input placeholder="请输入办公地址" />,
        },
        {
          name: "taxIdentificationNumber",
          label: "税号",
          rules: [{ len: 18, message: "税号长度必须为 18 位" }],
          component: <Input placeholder="请输入 18 位税号" maxLength={18} />,
        },
        { name: "bankName", label: "开户行", component: <Input placeholder="请输入开户行" /> },
        { name: "bankAccount", label: "银行账号", component: <Input placeholder="请输入银行账号" /> },
        { name: "remark", label: "备注", component: <Input.TextArea placeholder="请输入备注" rows={3} /> },
      ]}
      searchFields={[
        { name: "customerCode", label: "客户编码" },
        { name: "customerName", label: "客户名称" },
        { name: "taxIdentificationNumber", label: "税号" },
        { name: "industry", label: "行业" },
      ]}
      rowKey="uuid"
      mapRecordToFormValues={(record) => ({
        ...record,
        regionId: record.regionId ? buildRegionPath(record.regionId, regionById, regionByAreaCode) : [],
      })}
      transformValues={(values) => ({
        ...values,
        regionId: Array.isArray(values.regionId) ? values.regionId[values.regionId.length - 1] || null : values.regionId,
        customerCode: values.customerCode || null,
        industry: values.industry || null,
        remark: values.remark || null,
      })}
      extraActions={({ filteredData, canExport }) => (
        <Button
          icon={<DownloadOutlined />}
          onClick={() => handleExportExcel(filteredData)}
          disabled={!canExport || !filteredData.length}
        >
          导出 Excel
        </Button>
      )}
    />
  );
}
