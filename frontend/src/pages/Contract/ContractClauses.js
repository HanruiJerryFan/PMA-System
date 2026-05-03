import React, { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import { Button, Card, Empty, Input, InputNumber, Select, Space, Tag, message } from "antd";
import { DownloadOutlined, FilePdfOutlined } from "@ant-design/icons";
import { getCurrentUser } from "../../api/auth";
import CRUDTable from "../../components/Common/CRUDTable";
import { contractAPI, customerAPI, exportAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { resolvePagePermissions } from "../../utils/pagePermissions";
import { downloadApiFile, downloadExcel, resolveBlobErrorMessage } from "../../utils/exporters";

const { Option } = Select;

export default function ContractClauses() {
  const [contracts, setContracts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [contractTypes, setContractTypes] = useState([]);
  const [clauseTypes, setClauseTypes] = useState([]);
  const [selectedContractUuid, setSelectedContractUuid] = useState(null);
  const [clauses, setClauses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [clausesLoading, setClausesLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const pagePermissions = useMemo(() => resolvePagePermissions("/contract/clauses"), []);
  const canExport = hasAnyAuthority(currentUser, pagePermissions.exportAuthorities);

  const customerMap = useMemo(
    () => Object.fromEntries(customers.map((item) => [item.uuid, item.customerName])),
    [customers]
  );

  const projectMap = useMemo(
    () => Object.fromEntries(projects.map((item) => [item.uuid, item.projectName])),
    [projects]
  );

  const contractTypeMap = useMemo(
    () => Object.fromEntries(contractTypes.map((item) => [item.id, item.typeName])),
    [contractTypes]
  );

  const clauseTypeMap = useMemo(
    () => Object.fromEntries(clauseTypes.map((item) => [item.id, item.description])),
    [clauseTypes]
  );

  const decoratedContracts = useMemo(
    () =>
      contracts.map((item) => ({
        ...item,
        customerLabel: customerMap[item.clientId] || item.clientId || "-",
        projectLabel: projectMap[item.projectBasicInfoId] || item.projectBasicInfoId || "-",
        contractTypeLabel: contractTypeMap[item.contractTypeId] || item.contractTypeId || "-",
      })),
    [contracts, contractTypeMap, customerMap, projectMap]
  );

  const contractMap = useMemo(
    () => Object.fromEntries(decoratedContracts.map((item) => [item.uuid, item])),
    [decoratedContracts]
  );

  const selectedContract = selectedContractUuid ? contractMap[selectedContractUuid] : null;

  const decoratedClauses = useMemo(
    () =>
      clauses.map((item) => ({
        ...item,
        clauseTypeLabel: clauseTypeMap[item.clauseTypeId] || "-",
      })),
    [clauses, clauseTypeMap]
  );

  const fetchContracts = async () => {
    setLoading(true);
    try {
      const [contractResponse, clauseTypeResponse] = await Promise.all([
        contractAPI.getContracts(),
        contractAPI.getClauseTypes(),
      ]);
      const contractList = contractResponse.data || [];
      setContracts(contractList);
      setClauseTypes(clauseTypeResponse.data || []);
      setSelectedContractUuid((current) =>
        current && contractList.some((item) => item.uuid === current) ? current : contractList[0]?.uuid || null
      );
      const [customerResponse, projectResponse, contractTypeResponse] = await Promise.all([
        customerAPI.getCustomerOptions(),
        projectAPI.getProjectOptions(),
        contractAPI.getContractTypes(),
      ]);
      setCustomers(customerResponse.data || []);
      setProjects(projectResponse.data || []);
      setContractTypes(contractTypeResponse.data || []);
    } finally {
      setLoading(false);
    }
  };

  const fetchClauses = async (contractUuid) => {
    if (!contractUuid) {
      setClauses([]);
      return;
    }
    setClausesLoading(true);
    try {
      const response = await contractAPI.getContractClauses(contractUuid);
      setClauses(response.data || []);
    } finally {
      setClausesLoading(false);
    }
  };

  useEffect(() => {
    fetchContracts();
  }, []);

  useEffect(() => {
    async function fetchCurrent() {
      try {
        setCurrentUser(await getCurrentUser());
      } catch (error) {
        setCurrentUser(null);
      }
    }

    fetchCurrent();
  }, []);

  useEffect(() => {
    fetchClauses(selectedContractUuid);
  }, [selectedContractUuid]);

  const handleCreate = async (values) => {
    await contractAPI.createContractClause(values);
    await fetchClauses(selectedContractUuid);
  };

  const handleUpdate = async (uuid, values) => {
    await contractAPI.updateContractClause(uuid, values);
    await fetchClauses(selectedContractUuid);
  };

  const handleDelete = async (uuid) => {
    await contractAPI.deleteContractClause(uuid);
    await fetchClauses(selectedContractUuid);
  };

  const handleExportExcel = () => {
    if (!selectedContract) {
      return;
    }
    downloadExcel(
      `contract-clauses-${selectedContract.contractNumber}.xls`,
      "合同条款",
      ["合同编号", "条款类型", "条款内容", "条款金额"],
      decoratedClauses.map((item) => [
        selectedContract.contractNumber || "",
        item.clauseTypeLabel || "",
        item.clauseDescription || "",
        item.clauseAmount ?? "",
      ])
    );
  };

  const handleExportPdf = async () => {
    if (!selectedContract) {
      return;
    }
    const payload = {
      title: "合同条款",
      subtitle: `生成时间：${dayjs().format("YYYY-MM-DD HH:mm")}`,
      fileName: `contract-clauses-${selectedContract.contractNumber || selectedContract.uuid}.pdf`,
      metadata: [
        { label: "合同编号", value: selectedContract.contractNumber || "-" },
        { label: "客户", value: selectedContract.customerLabel || "-" },
        { label: "项目", value: selectedContract.projectLabel || "-" },
        { label: "合同类型", value: selectedContract.contractTypeLabel || "-" },
      ],
      columns: [
        { header: "序号", align: "center", width: 6 },
        { header: "条款类型", width: 18 },
        { header: "条款内容", width: 54 },
        { header: "条款金额", align: "right", width: 14 },
      ],
      rows: decoratedClauses.map((item, index) => [
        index + 1,
        item.clauseTypeLabel || "",
        item.clauseDescription || "",
        item.clauseAmount ?? "",
      ]),
    };

    try {
      await downloadApiFile(exportAPI.downloadTablePdf(payload), payload.fileName, "application/pdf");
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "导出 PDF 失败"));
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <CRUDTable
        title="合同列表"
        columns={[
          { title: "合同编号", dataIndex: "contractNumber", key: "contractNumber", width: 180 },
          { title: "客户", dataIndex: "customerLabel", key: "customerLabel", width: 220 },
          { title: "项目", dataIndex: "projectLabel", key: "projectLabel", width: 220 },
          {
            title: "合同类型",
            dataIndex: "contractTypeLabel",
            key: "contractTypeLabel",
            width: 140,
          },
        ]}
        dataSource={decoratedContracts}
        loading={loading}
        rowKey="uuid"
        searchFields={[
          { name: "contractNumber", label: "合同编号" },
          { name: "customerLabel", label: "客户" },
          { name: "projectLabel", label: "项目" },
          { name: "contractTypeLabel", label: "合同类型" },
        ]}
        tableProps={{
          rowClassName: (record) => (record.uuid === selectedContractUuid ? "ant-table-row-selected" : ""),
          onRow: (record) => ({
            onClick: () => setSelectedContractUuid(record.uuid),
            style: { cursor: "pointer" },
          }),
        }}
      />

      <Card
        title={selectedContract ? `合同条款：${selectedContract.contractNumber}` : "合同条款"}
        extra={
          <Space>
            <Button
              icon={<DownloadOutlined />}
              onClick={handleExportExcel}
              disabled={!canExport || !selectedContractUuid || !decoratedClauses.length}
            >
              导出 Excel
            </Button>
            <Button
              icon={<FilePdfOutlined />}
              onClick={handleExportPdf}
              disabled={!canExport || !selectedContractUuid || !decoratedClauses.length}
            >
              导出 PDF
            </Button>
          </Space>
        }
      >
        {selectedContractUuid ? (
          <CRUDTable
            title=""
            columns={[
              {
                title: "条款类型",
                dataIndex: "clauseTypeLabel",
                key: "clauseTypeLabel",
                width: 180,
                render: (value) => <Tag color="blue">{value}</Tag>,
              },
              {
                title: "条款内容",
                dataIndex: "clauseDescription",
                key: "clauseDescription",
                width: 420,
              },
              { title: "条款金额", dataIndex: "clauseAmount", key: "clauseAmount", width: 160 },
            ]}
            dataSource={decoratedClauses}
            loading={clausesLoading}
            onCreate={handleCreate}
            onUpdate={handleUpdate}
            onDelete={handleDelete}
            rowKey="uuid"
            searchFields={[
              { name: "clauseTypeLabel", label: "条款类型" },
              { name: "clauseDescription", label: "条款内容" },
            ]}
            transformValues={(values) => ({
              ...values,
              contractBasicInfoId: selectedContractUuid,
            })}
            formFields={[
              {
                name: "clauseTypeId",
                label: "条款类型",
                rules: [{ required: true, message: "请选择条款类型" }],
                component: (
                  <Select placeholder="请选择条款类型">
                    {clauseTypes.map((item) => (
                      <Option key={item.id} value={item.id}>
                        {item.description}
                      </Option>
                    ))}
                  </Select>
                ),
              },
              {
                name: "clauseDescription",
                label: "条款内容",
                rules: [{ required: true, message: "请输入条款内容" }],
                component: <Input.TextArea rows={4} placeholder="请输入条款内容" />,
              },
              {
                name: "clauseAmount",
                label: "条款金额",
                component: <InputNumber min={0} precision={2} style={{ width: "100%" }} />,
              },
            ]}
          />
        ) : (
          <Empty description="请选择一个合同后管理条款" />
        )}
      </Card>
    </Space>
  );
}
