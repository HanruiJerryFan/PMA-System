import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Cascader, Input, Select, Tag, message } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI, regionAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";
import { buildRegionOptions, buildRegionPath, formatRegionLabel } from "../../utils/regions";
import { sortSelectItems } from "../../utils/selectSorting";

const { Option } = Select;

const ACTIVITY_STATUS_OPTIONS = [
  { value: "ACTIVE", label: "活跃" },
  { value: "NORMAL", label: "正常" },
  { value: "INACTIVE", label: "不活跃" },
  { value: "DORMANT", label: "沉睡" },
];

function normalizeTextFilter(value) {
  return typeof value === "string" ? value.trim() || undefined : undefined;
}

export default function CustomerInfo() {
  const [customers, setCustomers] = useState([]);
  const [total, setTotal] = useState(0);
  const [customerTypes, setCustomerTypes] = useState([]);
  const [customerIndustries, setCustomerIndustries] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [query, setQuery] = useState({ page: 1, pageSize: 10, filters: {}, reloadKey: 0 });

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

  const decorateRows = useCallback(
    (rows) =>
      rows.map((item) => ({
        ...item,
        customerTypeLabel: customerTypeMap[item.customerTypeId] || "-",
        regionLabel: regionMap[item.regionId] || "-",
        activityStatusLabel: activityStatusMap[item.activityStatus] || item.activityStatus || "-",
      })),
    [customerTypeMap, regionMap, activityStatusMap]
  );

  const decoratedCustomers = useMemo(() => decorateRows(customers), [customers, decorateRows]);

  const fetchCustomersPage = useCallback(async ({ page, pageSize, filters }) => {
    setLoading(true);
    try {
      const response = await customerAPI.getCustomersPage({ page, pageSize, ...filters });
      const pageData = response?.data || {};
      const nextRecords = Array.isArray(pageData.records) ? pageData.records : [];
      const nextTotal = Number(pageData.total ?? 0);
      if (nextRecords.length === 0 && page > 1 && nextTotal > 0) {
        const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize));
        setQuery((prev) => ({ ...prev, page: lastPage }));
        return;
      }
      setCustomers(nextRecords);
      setTotal(nextTotal);
    } catch (error) {
      message.error(error?.message || "加载客户列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCustomersPage(query);
  }, [fetchCustomersPage, query]);

  useEffect(() => {
    async function fetchDictionaries() {
      try {
        const [typeResponse, industryResponse, regionResponse] = await Promise.all([
          customerAPI.getCustomerTypes(),
          customerAPI.getCustomerIndustryDicts(),
          regionAPI.getRegions(),
        ]);
        setCustomerTypes(typeResponse.data || []);
        setCustomerIndustries(industryResponse.data || []);
        setRegions(regionResponse.data || []);
      } catch (error) {
        message.error(error?.message || "加载客户字典失败");
      }
    }

    fetchDictionaries();
  }, []);

  const reloadCurrentPage = () => {
    setQuery((prev) => ({ ...prev, reloadKey: prev.reloadKey + 1 }));
  };

  const handleCreate = async (values) => {
    await customerAPI.createCustomer(values);
    reloadCurrentPage();
  };

  const handleUpdate = async (uuid, values) => {
    await customerAPI.updateCustomer(uuid, values);
    reloadCurrentPage();
  };

  const handleDelete = async (uuid) => {
    await customerAPI.deleteCustomer(uuid);
    reloadCurrentPage();
  };

  const handleExportExcel = async () => {
    setExporting(true);
    try {
      const response = await customerAPI.getCustomers();
      const filters = query.filters || {};
      const filteredRows = (response.data || []).filter((item) => {
        if (filters.customerCode && !String(item.customerCode || "").toLowerCase().includes(filters.customerCode.toLowerCase())) {
          return false;
        }
        if (filters.customerName && !String(item.customerName || "").toLowerCase().includes(filters.customerName.toLowerCase())) {
          return false;
        }
        if (filters.taxIdentificationNumber && !String(item.taxIdentificationNumber || "").toLowerCase().includes(filters.taxIdentificationNumber.toLowerCase())) {
          return false;
        }
        if (filters.industry && !String(item.industry || "").toLowerCase().includes(filters.industry.toLowerCase())) {
          return false;
        }
        if (filters.activityStatus && item.activityStatus !== filters.activityStatus) {
          return false;
        }
        return true;
      });
      const rows = decorateRows(filteredRows);
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
    } catch (error) {
      message.error(error?.message || "导出客户数据失败");
    } finally {
      setExporting(false);
    }
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
      serverPagination={{
        current: query.page,
        pageSize: query.pageSize,
        total,
        onChange: (nextPage, nextPageSize) => setQuery((prev) => ({ ...prev, page: nextPage, pageSize: nextPageSize })),
        onSearch: (values) => {
          setQuery((prev) => ({
            ...prev,
            page: 1,
            filters: {
              customerCode: normalizeTextFilter(values.customerCode),
              customerName: normalizeTextFilter(values.customerName),
              taxIdentificationNumber: normalizeTextFilter(values.taxIdentificationNumber),
              industry: normalizeTextFilter(values.industry),
              activityStatus: values.activityStatus || undefined,
            },
          }));
        },
        onReset: () => setQuery((prev) => ({ ...prev, page: 1, filters: {} })),
      }}
      detailFields={[
        { key: "customerCode", label: "客户编码" },
        { key: "customerName", label: "客户名称" },
        { key: "customerTypeLabel", label: "客户类型" },
        { key: "regionLabel", label: "地区" },
        { key: "industry", label: "行业" },
        { key: "activityStatusLabel", label: "活跃状态" },
        { key: "lastActiveAt", label: "最后活跃时间" },
        { key: "registerAddress", label: "注册地址" },
        { key: "officeAddress", label: "办公地址" },
        { key: "taxIdentificationNumber", label: "税号" },
        { key: "bankName", label: "开户行" },
        { key: "bankAccount", label: "银行账号" },
        { key: "remark", label: "备注" },
      ]}
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
          component: ({ editingRecord }) => (
            <Select placeholder="请选择客户类型" disabled={Boolean(editingRecord?.customerCode)}>
              {sortSelectItems(customerTypes, ["typeCode"]).map((item) => (
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
          component: ({ editingRecord }) => (
            <Cascader
              options={regionOptions}
              placeholder="请选择省 / 市 / 区"
              changeOnSelect
              disabled={Boolean(editingRecord?.customerCode)}
            />
          ),
        },
        {
          name: "industry",
          label: "行业",
          component: ({ editingRecord }) => {
            const existingIndustry = editingRecord?.industry;
            const hasDisabledExisting =
              existingIndustry &&
              !activeCustomerIndustries.some((item) => item.industryName === existingIndustry);
            return (
              <Select placeholder="请选择行业" allowClear>
                {hasDisabledExisting ? (
                  <Option key={`disabled-${existingIndustry}`} value={existingIndustry}>
                    {existingIndustry}（已停用）
                  </Option>
                ) : null}
                {activeCustomerIndustries.map((item) => (
                  <Option key={item.id} value={item.industryName}>
                    {item.industryName}
                  </Option>
                ))}
              </Select>
            );
          },
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
        {
          name: "activityStatus",
          label: "活跃状态",
          component: (
            <Select placeholder="请选择活跃状态" allowClear style={{ width: 140 }}>
              {ACTIVITY_STATUS_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          ),
        },
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
      extraActions={({ canExport }) => (
        <Button
          icon={<DownloadOutlined />}
          loading={exporting}
          onClick={handleExportExcel}
          disabled={!canExport || total === 0}
        >
          导出 Excel
        </Button>
      )}
    />
  );
}
