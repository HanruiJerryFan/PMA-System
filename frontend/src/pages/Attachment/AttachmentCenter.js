import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Alert, Button, Form, Input, Modal, Select, Space, Tag, message } from "antd";
import { DeleteOutlined, DownloadOutlined, EyeOutlined, UploadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { getCurrentUser } from "../../api/auth";
import { attachmentAPI, contractAPI, customerAPI, financeAPI, inventoryAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { downloadApiFile, resolveBlobErrorMessage } from "../../utils/exporters";
import { resolvePagePermissions } from "../../utils/pagePermissions";

const { Option } = Select;

const BUSINESS_TYPE_OPTIONS = [
  { value: "projects", label: "项目" },
  { value: "project-lists", label: "项目清单" },
  { value: "finance-vouchers", label: "财务凭证" },
  { value: "contracts", label: "合同" },
  { value: "customers", label: "客户" },
  { value: "warehouse-documents", label: "出入库单" },
  { value: "other", label: "其他" },
];

function formatFileSize(value) {
  if (value == null || value === "") {
    return "-";
  }
  const size = Number(value);
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  if (size < 1024 * 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  return `${(size / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

function getBusinessTypeLabel(value) {
  return BUSINESS_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || "-";
}

function normalizeResponseData(response) {
  return response?.data ?? response ?? [];
}

function isPdfAttachment(record) {
  const mimeType = String(record?.mimeType || "").toLowerCase();
  const fileExt = String(record?.fileExt || "").toLowerCase();
  const fileName = String(record?.originalFileName || "").toLowerCase();
  return mimeType === "application/pdf" || fileExt === "pdf" || fileName.endsWith(".pdf");
}

function getUploaderLabel(record) {
  return record?.uploadedByName || record?.uploadedByUsername || (record?.uploadedBy ? `用户 ${record.uploadedBy}` : "-");
}

function buildBusinessOptionLabel(type, item) {
  if (!item) {
    return "-";
  }
  if (type === "projects") {
    return `${item.projectName || "未命名项目"}${item.projectNumber ? ` / ${item.projectNumber}` : ""}`;
  }
  if (type === "project-lists") {
    return `${item.listName || "未命名清单"}${item.projectId ? ` / ${item.projectId}` : ""}`;
  }
  if (type === "finance-vouchers") {
    return `${item.voucherNo || "未命名凭证"}${item.summary ? ` / ${item.summary}` : ""}`;
  }
  if (type === "contracts") {
    return `${item.contractNumber || "未命名合同"}${item.subItemContent ? ` / ${item.subItemContent}` : ""}`;
  }
  if (type === "customers") {
    return `${item.customerName || "未命名客户"}${item.customerCode ? ` / ${item.customerCode}` : ""}`;
  }
  if (type === "warehouse-documents") {
    return `${item.docNumber || "未命名单据"}${item.counterpartyName ? ` / ${item.counterpartyName}` : ""}`;
  }
  return item.uuid || item.id || "-";
}

export default function AttachmentCenter() {
  const [searchParams] = useSearchParams();
  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [businessOptionsLoading, setBusinessOptionsLoading] = useState(false);
  const [businessOptions, setBusinessOptions] = useState([]);
  const [uploadForm] = Form.useForm();
  const uploadInputRef = useRef(null);
  const pagePermissions = useMemo(() => resolvePagePermissions("/attachment/center"), []);
  const canManage = hasAnyAuthority(currentUser, pagePermissions.manageAuthorities);
  const canAccess = hasAnyAuthority(currentUser, pagePermissions.exportAuthorities);

  const businessTypeFilter = searchParams.get("businessType") || "";
  const businessUuidFilter = searchParams.get("businessUuid") || "";
  const isContextMode = Boolean(businessTypeFilter || businessUuidFilter);
  const selectedUploadBusinessType = Form.useWatch("businessType", uploadForm);

  const pageTitle = useMemo(() => {
    if (!businessTypeFilter) {
      return "附件中心";
    }
    const label = getBusinessTypeLabel(businessTypeFilter);
    return businessUuidFilter
      ? `附件中心 / ${label} / ${businessUuidFilter}`
      : `附件中心 / ${label}`;
  }, [businessTypeFilter, businessUuidFilter]);

  const decoratedAttachments = useMemo(
    () =>
      attachments.map((item) => ({
        ...item,
        businessTypeLabel: getBusinessTypeLabel(item.businessType),
        uploaderLabel: getUploaderLabel(item),
      })),
    [attachments]
  );

  const resetUploadForm = useCallback(() => {
    uploadForm.resetFields();
    uploadForm.setFieldsValue({
      businessType: businessTypeFilter || undefined,
      businessUuid: businessUuidFilter || undefined,
      businessTargetUuid: businessUuidFilter || undefined,
    });
    setBusinessOptions([]);
  }, [businessTypeFilter, businessUuidFilter, uploadForm]);

  const fetchAttachments = useCallback(async () => {
    setLoading(true);
    try {
      const response = businessTypeFilter
        ? await attachmentAPI.getAttachmentsByBusiness(businessTypeFilter, businessUuidFilter || undefined)
        : await attachmentAPI.getAttachments();
      setAttachments(response.data || []);
    } finally {
      setLoading(false);
    }
  }, [businessTypeFilter, businessUuidFilter]);

  useEffect(() => {
    fetchAttachments();
    resetUploadForm();
  }, [fetchAttachments, resetUploadForm]);

  useEffect(() => {
    async function fetchCurrentUser() {
      try {
        setCurrentUser(await getCurrentUser());
      } catch (error) {
        setCurrentUser(null);
      }
    }

    fetchCurrentUser();
  }, []);

  useEffect(() => {
    if (!uploadVisible || !selectedUploadBusinessType || selectedUploadBusinessType === "other") {
      setBusinessOptions([]);
      setBusinessOptionsLoading(false);
      return;
    }

    async function fetchBusinessOptions() {
      setBusinessOptionsLoading(true);
      try {
        const loaderMap = {
          projects: () => projectAPI.getProjectOptions(),
          "project-lists": () => projectAPI.getProjectListOptions(),
          "finance-vouchers": () => financeAPI.getFinanceVoucherOptions(),
          contracts: () => contractAPI.getContractOptions(),
          customers: () => customerAPI.getCustomerOptions(),
          "warehouse-documents": () => inventoryAPI.getWarehouseDocOptions(),
        };
        const response = await loaderMap[selectedUploadBusinessType]?.();
        setBusinessOptions(normalizeResponseData(response));
      } catch (error) {
        setBusinessOptions([]);
        message.error(error?.message || "加载业务对象列表失败");
      } finally {
        setBusinessOptionsLoading(false);
      }
    }

    fetchBusinessOptions();
  }, [selectedUploadBusinessType, uploadVisible]);

  const handleUpdate = async (uuid, values) => {
    if (!canManage) {
      message.error("无权限操作");
      return;
    }
    await attachmentAPI.updateAttachment(uuid, values);
    await fetchAttachments();
  };

  const handleDelete = async (uuid) => {
    if (!canManage) {
      message.error("无权限操作");
      return;
    }
    await attachmentAPI.deleteAttachment(uuid);
    await fetchAttachments();
  };

  const handleDownload = async (record) => {
    if (!canAccess) {
      message.error("无权限操作");
      return;
    }
    try {
      await downloadApiFile(
        attachmentAPI.downloadAttachment(record.uuid),
        record.originalFileName || record.fileName || "download",
        record.mimeType || "application/octet-stream"
      );
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "附件下载失败"));
    }
  };

  const handlePreviewPdf = async (record) => {
    if (!canAccess) {
      message.error("无权限操作");
      return;
    }
    if (!isPdfAttachment(record)) {
      message.warning("仅 PDF 附件支持网页预览");
      return;
    }
    const previewWindow = window.open("", "_blank", "noopener,noreferrer");
    try {
      const response = await attachmentAPI.previewAttachmentPdf(record.uuid);
      const blob = response?.data instanceof Blob ? response.data : response;
      const previewUrl = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
      if (previewWindow) {
        previewWindow.location.href = previewUrl;
      } else {
        window.open(previewUrl, "_blank", "noopener,noreferrer");
      }
      window.setTimeout(() => URL.revokeObjectURL(previewUrl), 60 * 1000);
    } catch (error) {
      if (previewWindow) {
        previewWindow.close();
      }
      message.error(await resolveBlobErrorMessage(error, "PDF 预览失败"));
    }
  };

  const closeUploadModal = () => {
    setUploadVisible(false);
    setSelectedFile(null);
    if (uploadInputRef.current) {
      uploadInputRef.current.value = "";
    }
    resetUploadForm();
  };

  const openUploadModal = () => {
    resetUploadForm();
    setUploadVisible(true);
  };

  const handleUpload = async () => {
    if (!canManage) {
      message.error("Permission denied");
      return;
    }
    try {
      const values = await uploadForm.validateFields();
      if (!selectedFile) {
        message.error("请选择要上传的文件");
        return;
      }

      const formData = new FormData();
      formData.append("file", selectedFile);
      formData.append("businessType", values.businessType);
      if (values.businessUuid) {
        formData.append("businessUuid", values.businessUuid);
      }

      setUploading(true);
      await attachmentAPI.uploadAttachment(formData);
      message.success("附件上传成功");
      closeUploadModal();
      await fetchAttachments();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(error?.message || "上传失败");
    } finally {
      setUploading(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      {isContextMode ? (
        <Alert
          type="info"
          showIcon
          message="当前为业务附件视图"
          description="这里用于查看、下载和维护当前业务对象的附件。新增附件请回到对应业务页面直接上传。"
        />
      ) : (
        <div style={{ display: "flex", justifyContent: "flex-end" }}>
          <Button type="primary" icon={<UploadOutlined />} onClick={openUploadModal} disabled={!canManage}>
            上传附件
          </Button>
        </div>
      )}

      <CRUDTable
        title={pageTitle}
        columns={[
          ...(!isContextMode
            ? [
                {
                  title: "业务类型",
                  dataIndex: "businessTypeLabel",
                  key: "businessTypeLabel",
                  width: 180,
                },
                { title: "业务 UUID", dataIndex: "businessUuid", key: "businessUuid", width: 220 },
              ]
            : []),
          { title: "文件名", dataIndex: "originalFileName", key: "originalFileName", width: 260 },
          { title: "文件类型", dataIndex: "fileExt", key: "fileExt", width: 100, render: (value) => value || "-" },
          { title: "文件大小", dataIndex: "fileSize", key: "fileSize", width: 120, render: formatFileSize },
          { title: "上传人", dataIndex: "uploaderLabel", key: "uploaderLabel", width: 140 },
          {
            title: "上传时间",
            dataIndex: "uploadedAt",
            key: "uploadedAt",
            width: 180,
            render: (value) => (value ? new Date(value).toLocaleString() : "-"),
          },
        ]}
        dataSource={decoratedAttachments}
        loading={loading}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
        updateAuthorities={["attachment.manage"]}
        deleteAuthorities={["attachment.manage"]}
        rowKey="uuid"
        enableView={false}
        rowActions={({ record }) => (
          <>
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={() => handlePreviewPdf(record)}
              disabled={!canAccess || !isPdfAttachment(record)}
              size="small"
            >
              预览PDF
            </Button>
            <Button
              type="link"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
              disabled={!canAccess}
              size="small"
            >
              下载
            </Button>
          </>
        )}
        searchFields={[
          { name: "businessTypeLabel", label: "业务类型" },
          { name: "businessUuid", label: "业务 UUID" },
          { name: "originalFileName", label: "文件名" },
          { name: "uploaderLabel", label: "上传人" },
        ]}
        transformValues={(values) => ({
          businessType: values.businessType,
          businessUuid: values.businessUuid || null,
          originalFileName: values.originalFileName || null,
        })}
        formFields={[
          {
            name: "businessType",
            label: "业务类型",
            rules: [{ required: true, message: "请选择业务类型" }],
            component: (
              <Select placeholder="请选择业务类型">
                {BUSINESS_TYPE_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.value}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "businessUuid",
            label: "业务 UUID",
            component: <Input placeholder="请输入关联业务 UUID" />,
          },
          {
            name: "originalFileName",
            label: "文件名",
            component: <Input placeholder="请输入展示文件名" />,
          },
        ]}
      />

      {!isContextMode ? (
        <Modal
          title="上传附件"
          open={uploadVisible}
          onOk={handleUpload}
          confirmLoading={uploading}
          onCancel={closeUploadModal}
          destroyOnHidden
        >
          <Form form={uploadForm} layout="vertical" preserve={false}>
            <Form.Item
              name="businessType"
              label="业务类型"
              rules={[{ required: true, message: "请选择业务类型" }]}
            >
              <Select
                placeholder="请选择业务类型"
                onChange={(value) => {
                  uploadForm.setFieldsValue({
                    businessType: value,
                    businessTargetUuid: undefined,
                    businessUuid: undefined,
                  });
                  setBusinessOptions([]);
                }}
              >
                {BUSINESS_TYPE_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.value}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            </Form.Item>
            {selectedUploadBusinessType && selectedUploadBusinessType !== "other" ? (
              <Form.Item
                name="businessTargetUuid"
                label="关联业务对象"
                rules={[{ required: true, message: "请选择关联业务对象" }]}
              >
                <Select
                  placeholder="请选择关联业务对象"
                  showSearch
                  optionFilterProp="label"
                  loading={businessOptionsLoading}
                  onChange={(value) => {
                    uploadForm.setFieldValue("businessUuid", value || undefined);
                  }}
                  options={businessOptions.map((item) => ({
                    value: item.uuid || item.docNumber || item.id,
                    label: buildBusinessOptionLabel(selectedUploadBusinessType, item),
                  }))}
                />
              </Form.Item>
            ) : null}
            <Form.Item
              name="businessUuid"
              label="业务 UUID"
              rules={
                selectedUploadBusinessType === "other"
                  ? [{ required: true, message: "请输入关联业务 UUID" }]
                  : []
              }
            >
              <Input
                placeholder={
                  selectedUploadBusinessType === "other"
                    ? "请输入关联业务 UUID"
                    : "选择上方业务对象后自动回填"
                }
                readOnly={selectedUploadBusinessType !== "other"}
              />
            </Form.Item>
            <Form.Item label="文件" required style={{ marginBottom: 0 }}>
              <Space wrap size={8} align="center" style={{ width: "100%" }}>
                <input
                  ref={uploadInputRef}
                  type="file"
                  onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
                  style={{ display: "none" }}
                />
                <Button icon={<UploadOutlined />} onClick={() => uploadInputRef.current?.click()}>
                  选择文件
                </Button>
                {selectedFile ? (
                  <>
                    <Tag color="blue" style={{ marginInlineEnd: 0 }}>
                      {selectedFile.name}
                    </Tag>
                    <Button
                      type="link"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => {
                        setSelectedFile(null);
                        if (uploadInputRef.current) {
                          uploadInputRef.current.value = "";
                        }
                      }}
                    >
                      移除
                    </Button>
                  </>
                ) : (
                  <span style={{ color: "#8c8c8c", fontSize: 12 }}>未选择文件</span>
                )}
              </Space>
            </Form.Item>
          </Form>
        </Modal>
      ) : null}
    </Space>
  );
}
