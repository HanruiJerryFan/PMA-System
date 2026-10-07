import React, { useEffect, useMemo, useState } from "react";
import { Alert, Input, InputNumber, Select, Space, Tag, message } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { financeAPI } from "../../api/modules";
import { SUBJECT_LEVEL_OPTIONS } from "../../utils/financeSubjects";

export default function FinanceSubjectManagement() {
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const rows = useMemo(
    () => subjects.map((item) => ({
      ...item,
      subjectLevelLabel: SUBJECT_LEVEL_OPTIONS.find((level) => level.value === item.subjectLevel)?.label,
    })),
    [subjects]
  );

  const fetchSubjects = async () => {
    setLoading(true);
    try {
      const response = await financeAPI.getFinanceSubjects();
      setSubjects(response?.data ?? response ?? []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubjects().catch((error) => message.error(error?.message || "加载财务科目失败"));
  }, []);

  const handleCreate = async (values) => {
    await financeAPI.createFinanceSubject(values);
    await fetchSubjects();
  };

  const handleUpdate = async (id, values) => {
    await financeAPI.updateFinanceSubject(id, values);
    await fetchSubjects();
  };

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Alert
        type="info"
        showIcon
        message="在这里新增或修改一级、二级科目。修改名称后，历史凭证也会显示新名称；科目级别在创建后不可修改。"
      />
      <CRUDTable
        title="科目"
        columns={[
          {
            title: "科目级别", dataIndex: "subjectLevelLabel", key: "subjectLevelLabel", width: 140,
            render: (value, record) => <Tag color={record.subjectLevel === 1 ? "blue" : "green"}>{value}</Tag>,
            filters: SUBJECT_LEVEL_OPTIONS.map((item) => ({ text: item.label, value: item.value })),
            filterMultiple: false,
            onFilter: (value, record) => record.subjectLevel === value,
          },
          { title: "科目名称", dataIndex: "subjectName", key: "subjectName", width: 220 },
          {
            title: "排序", dataIndex: "sortOrder", key: "sortOrder", width: 100,
            sorter: (left, right) => left.sortOrder - right.sortOrder,
          },
        ]}
        dataSource={rows}
        loading={loading}
        onCreate={handleCreate}
        onUpdate={handleUpdate}
        rowKey="id"
        enableView={false}
        createAuthorities={["finance.manage"]}
        updateAuthorities={["finance.manage"]}
        searchFields={[{ name: "subjectName", label: "科目名称" }]}
        formFields={[
          {
            name: "subjectLevel",
            label: "科目级别",
            getDefaultValue: () => 1,
            rules: [{ required: true, message: "请选择科目级别" }],
            component: ({ editingRecord }) => (
              <Select options={SUBJECT_LEVEL_OPTIONS} disabled={Boolean(editingRecord)} />
            ),
          },
          {
            name: "subjectName",
            label: "科目名称",
            rules: [
              { required: true, whitespace: true, message: "请输入科目名称" },
              { max: 64, message: "科目名称不能超过64个字符" },
            ],
            component: <Input placeholder="请输入科目名称" maxLength={64} />,
          },
          {
            name: "sortOrder",
            label: "排序",
            getDefaultValue: () => 0,
            rules: [{ required: true, message: "请输入排序值" }],
            component: <InputNumber min={0} max={2147483647} precision={0} style={{ width: "100%" }} />,
          },
        ]}
        transformValues={(values) => ({ ...values, subjectName: values.subjectName?.trim() })}
      />
    </Space>
  );
}
