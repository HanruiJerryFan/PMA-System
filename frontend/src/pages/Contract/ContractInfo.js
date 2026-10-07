import React, { useEffect, useMemo, useRef, useState } from "react";
import dayjs from "dayjs";
import { Button, DatePicker, Input, InputNumber, Select, message } from "antd";
import { DownloadOutlined, FilePdfOutlined, LinkOutlined } from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getCurrentUser } from "../../api/auth";
import BusinessAttachmentUpload from "../../components/Common/BusinessAttachmentUpload";
import CRUDTable from "../../components/Common/CRUDTable";
import { contractAPI, customerAPI, exportAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { uploadBusinessAttachments } from "../../utils/attachments";
import { downloadApiFile, downloadExcel, resolveBlobErrorMessage } from "../../utils/exporters";
import { sortSelectItems } from "../../utils/selectSorting";
import { formatProjectOptionLabel } from "../../utils/projectLabels";

const { Option } = Select;

function getContractTypeName(item) {
  return item?.typeName || item?.name || item?.code || String(item?.id ?? "");
}

function normalizeApiData(response) {
  return response?.data ?? response ?? [];
}

function normalizeApiEntity(response) {
  return response?.data ?? response ?? null;
}

export default function ContractInfo() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const projectUuidFilter = searchParams.get("projectUuid") || "";
  const [contracts, setContracts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [contacts, setContacts] = useState([]);
  const [contractTypes, setContractTypes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [pendingContractAttachmentFiles, setPendingContractAttachmentFiles] = useState([]);
  const contractAttachmentInputRef = useRef(null);
  const canAccessAttachments = hasAnyAuthority(currentUser, ["attachment.access"]);
  const canManageAttachments = hasAnyAuthority(currentUser, ["attachment.manage"]);

  const customerMap = useMemo(
    () => Object.fromEntries(customers.map((item) => [item.uuid, item.customerName])),
    [customers]
  );
  const projectMap = useMemo(
    () => Object.fromEntries(projects.map((item) => [item.uuid, formatProjectOptionLabel(item)])),
    [projects]
  );
  const contactMap = useMemo(
    () => Object.fromEntries(contacts.map((item) => [item.uuid, item.contactName])),
    [contacts]
  );
  const contractTypeMap = useMemo(
    () => Object.fromEntries(contractTypes.map((item) => [item.id, getContractTypeName(item)])),
    [contractTypes]
  );

  const decoratedContracts = useMemo(
    () =>
      contracts.map((item) => ({
        ...item,
        customerLabel: customerMap[item.clientId] || item.clientId,
        projectLabel: projectMap[item.projectBasicInfoId] || item.projectBasicInfoId,
        contactLabel: contactMap[item.contactId] || "-",
        contractTypeLabel: contractTypeMap[item.contractTypeId] || "-",
      })),
    [contracts, customerMap, projectMap, contactMap, contractTypeMap]
  );

  const visibleContracts = useMemo(
    () =>
      projectUuidFilter
        ? decoratedContracts.filter((item) => item.projectBasicInfoId === projectUuidFilter)
        : decoratedContracts,
    [decoratedContracts, projectUuidFilter]
  );

  const fetchContracts = async () => {
    setLoading(true);
    try {
      const response = await contractAPI.getContracts();
      setContracts(normalizeApiData(response));
    } finally {
      setLoading(false);
    }
  };

  const fetchOptions = async () => {
    const [customerResponse, projectResponse, contactResponse, contractTypeResponse] = await Promise.all([
      customerAPI.getCustomerOptions(),
      projectAPI.getProjectOptions(),
      customerAPI.getCustomerContactOptions(),
      contractAPI.getContractTypes(),
    ]);
    setCustomers(normalizeApiData(customerResponse));
    setProjects(normalizeApiData(projectResponse));
    setContacts(normalizeApiData(contactResponse));
    setContractTypes(normalizeApiData(contractTypeResponse));
  };

  useEffect(() => {
    fetchContracts();
    fetchOptions();
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

  const resetPendingContractAttachment = () => {
    setPendingContractAttachmentFiles([]);
    if (contractAttachmentInputRef.current) {
      contractAttachmentInputRef.current.value = "";
    }
  };

  const handleCreate = async (values) => {
    const createdResponse = await contractAPI.createContract(values);
    const createdContract = normalizeApiEntity(createdResponse);
    if (pendingContractAttachmentFiles.length && createdContract?.uuid) {
      await uploadBusinessAttachments("contracts", createdContract.uuid, pendingContractAttachmentFiles);
    }
    await fetchContracts();
    return createdContract;
  };

  const handleUpdate = async (uuid, values) => {
    const updatedResponse = await contractAPI.updateContract(uuid, values);
    if (pendingContractAttachmentFiles.length) {
      await uploadBusinessAttachments("contracts", uuid, pendingContractAttachmentFiles);
    }
    await fetchContracts();
    return normalizeApiEntity(updatedResponse);
  };

  const handleDelete = async (uuid) => {
    await contractAPI.deleteContract(uuid);
    await fetchContracts();
  };

  const openAttachments = (uuid) => {
    navigate(`/attachment/center?businessType=contracts&businessUuid=${uuid}`);
  };

  const handleExportExcel = (rows = decoratedContracts) => {
    downloadExcel(
      `contracts-${new Date().toISOString().slice(0, 10)}.xls`,
      "合同台账",
      [
        "合同编号",
        "合同类型",
        "客户",
        "项目",
        "联系人",
        "签约日期",
        "合同金额",
        "分项编号",
        "分项内容",
      ],
      rows.map((item) => [
        item.contractNumber || "",
        item.contractTypeLabel || "",
        item.customerLabel || "",
        item.projectLabel || "",
        item.contactLabel || "",
        item.signDate ? dayjs(item.signDate).format("YYYY-MM-DD") : "",
        item.contractAmount ?? "",
        item.subItemNo || "",
        item.subItemContent || "",
      ])
    );
  };

  const handleExportPdf = async (rows = decoratedContracts) => {
    if (!rows.length) {
      return;
    }

    const totalAmount = rows.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0);
    const payload = {
      title: "合同台账",
      subtitle: `生成时间：${dayjs().format("YYYY-MM-DD HH:mm")}`,
      fileName: `contracts-${new Date().toISOString().slice(0, 10)}.pdf`,
      summaries: [
        { label: "记录数", value: String(rows.length) },
        { label: "合同总额", value: totalAmount.toFixed(2) },
      ],
      columns: [
        { header: "序号", align: "center", width: 5 },
        { header: "合同编号", width: 13 },
        { header: "合同类型", width: 10 },
        { header: "客户", width: 15 },
        { header: "项目", width: 15 },
        { header: "联系人", width: 10 },
        { header: "签约日期", align: "center", width: 10 },
        { header: "金额", align: "right", width: 9 },
        { header: "分项编号", width: 8 },
        { header: "分项内容", width: 15 },
      ],
      rows: rows.map((item, index) => [
        index + 1,
        item.contractNumber || "",
        item.contractTypeLabel || "",
        item.customerLabel || "",
        item.projectLabel || "",
        item.contactLabel || "",
        item.signDate ? dayjs(item.signDate).format("YYYY-MM-DD") : "",
        item.contractAmount ?? "",
        item.subItemNo || "",
        item.subItemContent || "",
      ]),
    };

    try {
      await downloadApiFile(exportAPI.downloadTablePdf(payload), payload.fileName, "application/pdf");
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "导出 PDF 失败"));
    }
  };

  return (
    <CRUDTable
      title="合同管理"
      columns={[
        { title: "合同编号", dataIndex: "contractNumber", key: "contractNumber", width: 180 },
        { title: "合同类型", dataIndex: "contractTypeLabel", key: "contractTypeLabel", width: 140 },
        { title: "客户", dataIndex: "customerLabel", key: "customerLabel", width: 220 },
        { title: "项目", dataIndex: "projectLabel", key: "projectLabel", width: 220 },
        { title: "联系人", dataIndex: "contactLabel", key: "contactLabel", width: 160 },
        {
          title: "签约日期",
          dataIndex: "signDate",
          key: "signDate",
          width: 140,
          render: (value) => (value ? dayjs(value).format("YYYY-MM-DD") : "-"),
        },
        { title: "合同金额", dataIndex: "contractAmount", key: "contractAmount", width: 140 },
        { title: "分项编号", dataIndex: "subItemNo", key: "subItemNo", width: 120 },
      ]}
      dataSource={visibleContracts}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      onModalCancel={resetPendingContractAttachment}
      onModalSuccess={resetPendingContractAttachment}
      rowActions={({ record }) =>
        canAccessAttachments ? (
          <Button type="link" icon={<LinkOutlined />} onClick={() => openAttachments(record.uuid)} size="small">
            查看附件
          </Button>
        ) : null
      }
      rowKey="uuid"
      extraActions={({ filteredData, canExport }) => (
        <>
          <Button
            icon={<DownloadOutlined />}
            onClick={() => handleExportExcel(filteredData)}
            disabled={!canExport || !filteredData.length}
          >
            导出 Excel
          </Button>
          <Button
            icon={<FilePdfOutlined />}
            onClick={() => handleExportPdf(filteredData)}
            disabled={!canExport || !filteredData.length}
          >
            导出 PDF
          </Button>
        </>
      )}
      searchFields={[
        { name: "contractNumber", label: "合同编号" },
        {
          name: "contractTypeLabel",
          label: "合同类型",
          component: (
            <Select
              allowClear
              showSearch
              optionFilterProp="children"
              placeholder="请选择合同类型"
              style={{ width: 180 }}
            >
              {sortSelectItems(contractTypes, ["typeCode"]).map((item) => {
                const label = getContractTypeName(item);
                return (
                  <Option key={item.id} value={label}>
                    {label}
                  </Option>
                );
              })}
            </Select>
          ),
        },
        { name: "customerLabel", label: "客户" },
        { name: "projectLabel", label: "项目" },
      ]}
      mapRecordToFormValues={(record) => ({
        ...record,
        signDate: record.signDate ? dayjs(record.signDate) : null,
      })}
      transformValues={(values) => ({
        ...values,
        projectBasicInfoId: values.projectBasicInfoId || projectUuidFilter || null,
        signDate: values.signDate ? values.signDate.format("YYYY-MM-DD") : null,
      })}
      formFields={[
        {
          name: "contractNumber",
          label: "合同编号",
          rules: [{ required: true, message: "请输入合同编号" }],
          component: <Input placeholder="请输入合同编号" />,
        },
        {
          name: "contractTypeId",
          label: "合同类型",
          rules: [{ required: true, message: "请选择合同类型" }],
          component: (
            <Select placeholder="请选择合同类型">
              {sortSelectItems(contractTypes, ["typeCode"]).map((item) => (
                <Option key={item.id} value={item.id}>
                  {getContractTypeName(item)}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "clientId",
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
          name: "projectBasicInfoId",
          label: "项目",
          rules: projectUuidFilter ? [] : [{ required: true, message: "请选择项目" }],
          component: (
            <Select
              placeholder="请选择项目"
              showSearch
              optionFilterProp="children"
              disabled={Boolean(projectUuidFilter)}
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
          name: "contactId",
          label: "客户联系人",
          component: (
            <Select placeholder="请选择联系人" allowClear showSearch optionFilterProp="children">
              {contacts.map((item) => (
                <Option key={item.uuid} value={item.uuid}>
                  {item.contactName}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "subItemNo",
          label: "分项编号",
          rules: [{ required: true, message: "请输入分项编号" }],
          component: <Input placeholder="例如 01" />,
        },
        { name: "subItemContent", label: "分项内容", component: <Input.TextArea rows={3} placeholder="请输入分项内容" /> },
        { name: "signDate", label: "签约日期", component: <DatePicker style={{ width: "100%" }} /> },
        { name: "contractAmount", label: "合同金额", component: <InputNumber min={0} precision={2} style={{ width: "100%" }} /> },
        {
          key: "contract-attachment-upload",
          renderOnly: true,
          render: ({ editingRecord }) => (
            <BusinessAttachmentUpload
              title="合同附件"
              businessType="contracts"
              businessUuid={editingRecord?.uuid}
              pendingFiles={pendingContractAttachmentFiles}
              onPendingFilesChange={setPendingContractAttachmentFiles}
              inputRef={contractAttachmentInputRef}
              canAccess={canAccessAttachments}
              canManage={canManageAttachments}
              chooseText={editingRecord ? "继续添加 PDF 附件" : "选择 PDF 附件"}
              helpText="保存合同时会自动上传并关联当前合同，可一次选择多个文件。"
              noManageText="当前账号没有附件上传权限，无法在这里上传合同附件。"
              onOpenAttachments={editingRecord ? () => openAttachments(editingRecord.uuid) : undefined}
            />
          ),
        },
      ]}
    />
  );
}
