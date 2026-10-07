import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Alert, Button, Form, Input, Modal, Select, Space, Spin, Tag, message } from "antd";
import { DeleteOutlined, DownloadOutlined, EyeOutlined, UploadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { getCurrentUser } from "../../api/auth";
import { attachmentAPI, contractAPI, customerAPI, financeAPI, inventoryAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { downloadApiFile, resolveBlobErrorMessage } from "../../utils/exporters";
import { resolvePagePermissions } from "../../utils/pagePermissions";
import { sortSelectItems } from "../../utils/selectSorting";
import { formatProjectOptionLabel } from "../../utils/projectLabels";

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

const BUSINESS_OPTION_LOADERS = {
  projects: () => projectAPI.getProjectOptions(),
  "project-lists": () => projectAPI.getProjectListOptions(),
  "finance-vouchers": () => financeAPI.getFinanceVoucherOptions(),
  contracts: () => contractAPI.getContractOptions(),
  customers: () => customerAPI.getCustomerOptions(),
  "warehouse-documents": () => inventoryAPI.getWarehouseDocOptions(),
};

const BUSINESS_OPTION_NUMBER_FIELDS = {
  projects: "projectNumber",
  "project-lists": "listName",
  "finance-vouchers": "voucherNo",
  contracts: "contractNumber",
  customers: "customerCode",
  "warehouse-documents": "docNumber",
};

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

function getBusinessOptionKey(type, item) {
  if (!item) {
    return null;
  }
  if (type === "warehouse-documents") {
    return item.docNumber || item.uuid || item.id || null;
  }
  return item.uuid || item.id || item.docNumber || null;
}

function buildBusinessOptionLabel(type, item) {
  if (!item) {
    return "-";
  }
  if (type === "projects") {
    return formatProjectOptionLabel(item);
  }
  if (type === "project-lists") {
    return item.listName || "未命名清单";
  }
  if (type === "finance-vouchers") {
    return item.voucherNo || "未编号凭证";
  }
  if (type === "contracts") {
    return item.contractNumber || "未编号合同";
  }
  if (type === "customers") {
    return item.customerCode || "未编号客户";
  }
  if (type === "warehouse-documents") {
    return item.docNumber || "未编号单据";
  }
  return item.uuid || item.id || "-";
}

function resolveBusinessDisplayLabel(type, businessUuid, businessLabelMap) {
  if (!businessUuid) {
    return "-";
  }
  if (type === "other") {
    return businessUuid;
  }
  return businessLabelMap?.[type]?.[businessUuid] || "未识别业务对象";
}

export default function AttachmentCenter() {
  const [searchParams] = useSearchParams();
  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [businessOptionsLoading, setBusinessOptionsLoading] = useState(false);
  const [businessOptions, setBusinessOptions] = useState([]);
  const [businessLabelLoading, setBusinessLabelLoading] = useState(false);
  const [businessLabelMap, setBusinessLabelMap] = useState({});
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewUrl, setPreviewUrl] = useState("");
  const [previewTitle, setPreviewTitle] = useState("");
  const [uploadForm] = Form.useForm();
  const uploadInputRef = useRef(null);
  const previewUrlRef = useRef("");
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
    const businessLabel = businessLabelLoading
      ? "加载中..."
      : resolveBusinessDisplayLabel(businessTypeFilter, businessUuidFilter, businessLabelMap);
    return businessUuidFilter
      ? `附件中心 / ${label} / ${businessLabel}`
      : `附件中心 / ${label}`;
  }, [businessLabelLoading, businessLabelMap, businessTypeFilter, businessUuidFilter]);

  const decoratedAttachments = useMemo(
    () =>
      attachments.map((item) => ({
        ...item,
        businessTypeLabel: getBusinessTypeLabel(item.businessType),
        businessDisplayLabel: resolveBusinessDisplayLabel(item.businessType, item.businessUuid, businessLabelMap),
        uploaderLabel: getUploaderLabel(item),
      })),
    [attachments, businessLabelMap]
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

  useEffect(
    () => () => {
      if (previewUrlRef.current) {
        URL.revokeObjectURL(previewUrlRef.current);
      }
    },
    []
  );

  useEffect(() => {
    const requiredTypes = new Set(
      attachments
        .map((item) => item.businessType)
        .filter((type) => type && type !== "other" && BUSINESS_OPTION_LOADERS[type])
    );

    if (businessTypeFilter && businessTypeFilter !== "other" && BUSINESS_OPTION_LOADERS[businessTypeFilter]) {
      requiredTypes.add(businessTypeFilter);
    }
    if (requiredTypes.has("project-lists")) {
      requiredTypes.add("projects");
    }

    if (!requiredTypes.size) {
      setBusinessLabelMap({});
      setBusinessLabelLoading(false);
      return;
    }

    let cancelled = false;

    async function fetchBusinessLabels() {
      setBusinessLabelLoading(true);
      try {
        const rawOptionsByType = {};
        await Promise.all(
          Array.from(requiredTypes).map(async (type) => {
            try {
              const response = await BUSINESS_OPTION_LOADERS[type]();
              rawOptionsByType[type] = normalizeResponseData(response);
            } catch {
              rawOptionsByType[type] = [];
            }
          })
        );

        const nextMap = {};
        const orderedTypes = ["projects", ...Array.from(requiredTypes).filter((type) => type !== "projects")];
        orderedTypes.forEach((type) => {
          const rows = rawOptionsByType[type] || [];
          nextMap[type] = rows.reduce((accumulator, item) => {
            const key = getBusinessOptionKey(type, item);
            if (key != null) {
              accumulator[key] = buildBusinessOptionLabel(type, item);
            }
            return accumulator;
          }, {});
        });

        if (!cancelled) {
          setBusinessLabelMap(nextMap);
        }
      } finally {
        if (!cancelled) {
          setBusinessLabelLoading(false);
        }
      }
    }

    fetchBusinessLabels();

    return () => {
      cancelled = true;
    };
  }, [attachments, businessTypeFilter]);

  useEffect(() => {
    if (!uploadVisible || !selectedUploadBusinessType || selectedUploadBusinessType === "other") {
      setBusinessOptions([]);
      setBusinessOptionsLoading(false);
      return;
    }

    async function fetchBusinessOptions() {
      setBusinessOptionsLoading(true);
      try {
        const response = await BUSINESS_OPTION_LOADERS[selectedUploadBusinessType]?.();
        setBusinessOptions(sortSelectItems(
          normalizeResponseData(response),
          [BUSINESS_OPTION_NUMBER_FIELDS[selectedUploadBusinessType]],
        ));
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

  const clearPreviewUrl = () => {
    if (previewUrlRef.current) {
      URL.revokeObjectURL(previewUrlRef.current);
      previewUrlRef.current = "";
    }
    setPreviewUrl("");
  };

  const closePreviewModal = () => {
    setPreviewVisible(false);
    setPreviewLoading(false);
    setPreviewTitle("");
    clearPreviewUrl();
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
    clearPreviewUrl();
    setPreviewTitle(record.originalFileName || record.fileName || "PDF 附件");
    setPreviewVisible(true);
    setPreviewLoading(true);
    try {
      const response = await attachmentAPI.previewAttachmentPdf(record.uuid);
      const blob = response?.data instanceof Blob ? response.data : new Blob([response?.data], { type: "application/pdf" });
      const contentType = String(response?.headers?.["content-type"] || blob.type || "").toLowerCase();

      if (!blob.size) {
        throw new Error("PDF 文件为空");
      }
      if (contentType && !contentType.includes("application/pdf")) {
        const text = await blob.text();
        throw new Error(text || "后端没有返回 PDF 文件");
      }

      const pdfBlob = blob.type === "application/pdf" ? blob : new Blob([blob], { type: "application/pdf" });
      const nextPreviewUrl = URL.createObjectURL(pdfBlob);
      previewUrlRef.current = nextPreviewUrl;
      setPreviewUrl(nextPreviewUrl);
    } catch (error) {
      closePreviewModal();
      message.error(await resolveBlobErrorMessage(error, "PDF 预览失败"));
    } finally {
      setPreviewLoading(false);
    }
  };

  const closeUploadModal = () => {
    setUploadVisible(false);
    setSelectedFiles([]);
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
      if (!selectedFiles.length) {
        message.error("请选择要上传的文件");
        return;
      }

      setUploading(true);
      const failedFiles = [];
      for (const file of selectedFiles) {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("businessType", values.businessType);
        if (values.businessUuid) {
          formData.append("businessUuid", values.businessUuid);
        }
        try {
          await attachmentAPI.uploadAttachment(formData);
        } catch (error) {
          failedFiles.push(file.name);
        }
      }

      if (failedFiles.length) {
        message.warning(`已上传 ${selectedFiles.length - failedFiles.length} 个文件，失败 ${failedFiles.length} 个：${failedFiles.join("、")}`);
      } else {
        message.success(`已上传 ${selectedFiles.length} 个附件`);
      }
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
                { title: "业务对象", dataIndex: "businessDisplayLabel", key: "businessDisplayLabel", width: 260 },
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
        loading={loading || businessLabelLoading}
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
          { name: "businessDisplayLabel", label: "业务对象" },
          { name: "originalFileName", label: "文件名" },
          { name: "uploaderLabel", label: "上传人" },
        ]}
        transformValues={(values) => ({
          originalFileName: values.originalFileName || null,
        })}
        formFields={[
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
              label="业务对象标识"
              rules={
                selectedUploadBusinessType === "other"
                  ? [{ required: true, message: "请输入业务对象标识" }]
                  : []
              }
            >
              <Input
                placeholder={
                  selectedUploadBusinessType === "other"
                    ? "请输入业务对象标识"
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
                  multiple
                  onChange={(event) => setSelectedFiles(Array.from(event.target.files || []))}
                  style={{ display: "none" }}
                />
                <Button icon={<UploadOutlined />} onClick={() => uploadInputRef.current?.click()}>
                  批量选择文件
                </Button>
                {selectedFiles.length ? (
                  <Space wrap size={6}>
                    <Tag color="blue" style={{ marginInlineEnd: 0 }}>
                      已选择 {selectedFiles.length} 个文件
                    </Tag>
                    {selectedFiles.slice(0, 4).map((file) => (
                      <Tag key={`${file.name}-${file.size}-${file.lastModified}`} style={{ marginInlineEnd: 0 }}>
                        {file.name}
                      </Tag>
                    ))}
                    {selectedFiles.length > 4 ? <Tag>另 {selectedFiles.length - 4} 个</Tag> : null}
                    <Button
                      type="link"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => {
                        setSelectedFiles([]);
                        if (uploadInputRef.current) {
                          uploadInputRef.current.value = "";
                        }
                      }}
                    >
                      清空
                    </Button>
                  </Space>
                ) : (
                  <span style={{ color: "#8c8c8c", fontSize: 12 }}>未选择文件</span>
                )}
              </Space>
            </Form.Item>
          </Form>
        </Modal>
      ) : null}

      <Modal
        title={`PDF预览：${previewTitle || "-"}`}
        open={previewVisible}
        onCancel={closePreviewModal}
        footer={null}
        width="90vw"
        style={{ top: 24 }}
        destroyOnHidden
      >
        {previewLoading ? (
          <div style={{ height: "78vh", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Spin tip="正在加载 PDF 预览" />
          </div>
        ) : previewUrl ? (
          <iframe
            title={previewTitle || "PDF预览"}
            src={previewUrl}
            style={{ width: "100%", height: "78vh", border: 0, background: "#f5f5f5" }}
          />
        ) : (
          <Alert type="warning" showIcon message="暂无可预览的 PDF 文件" />
        )}
      </Modal>
    </Space>
  );
}
