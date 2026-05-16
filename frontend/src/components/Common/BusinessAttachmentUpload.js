import React, { useEffect, useMemo, useState } from "react";
import { Alert, Button, Space, Spin, Tag } from "antd";
import { DeleteOutlined, LinkOutlined, UploadOutlined } from "@ant-design/icons";
import { attachmentAPI } from "../../api/modules";
import { normalizeSelectedFiles } from "../../utils/attachments";

function normalizeApiData(response) {
  return response?.data ?? response ?? [];
}

function getAttachmentDisplayName(attachment) {
  return attachment?.originalFileName || attachment?.fileName || attachment?.uuid || "未命名附件";
}

export default function BusinessAttachmentUpload({
  title = "附件",
  businessType,
  businessUuid,
  pendingFiles = [],
  onPendingFilesChange,
  inputRef,
  canAccess = false,
  canManage = false,
  accept = "application/pdf,.pdf",
  multiple = true,
  chooseText = "选择附件",
  helpText = "保存后会自动上传并关联当前业务对象。",
  noAccessText = "当前账号没有附件查看权限。",
  noManageText = "当前账号没有附件上传权限。",
  onOpenAttachments,
}) {
  const selectedFiles = useMemo(() => normalizeSelectedFiles(pendingFiles), [pendingFiles]);
  const [existingAttachments, setExistingAttachments] = useState([]);
  const [loadingExisting, setLoadingExisting] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);

  useEffect(() => {
    let active = true;

    if (!businessType || !businessUuid || !canAccess) {
      setExistingAttachments([]);
      setLoadFailed(false);
      return () => {
        active = false;
      };
    }

    setLoadingExisting(true);
    setLoadFailed(false);
    attachmentAPI
      .getAttachmentsByBusiness(businessType, businessUuid)
      .then((response) => {
        if (active) {
          setExistingAttachments(normalizeApiData(response));
        }
      })
      .catch(() => {
        if (active) {
          setExistingAttachments([]);
          setLoadFailed(true);
        }
      })
      .finally(() => {
        if (active) {
          setLoadingExisting(false);
        }
      });

    return () => {
      active = false;
    };
  }, [businessType, businessUuid, canAccess]);

  const clearPendingFiles = () => {
    onPendingFilesChange?.([]);
    if (inputRef?.current) {
      inputRef.current.value = "";
    }
  };

  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{ marginBottom: 8, fontWeight: 500 }}>{title}</div>
      <Space direction="vertical" size={8} style={{ width: "100%" }}>
        {businessUuid ? (
          canAccess ? (
            <Alert
              type={existingAttachments.length ? "success" : "info"}
              showIcon
              message={
                loadingExisting ? (
                  <Space size={8}>
                    <Spin size="small" />
                    <span>正在读取已有附件</span>
                  </Space>
                ) : loadFailed ? (
                  "已有附件读取失败"
                ) : (
                  `已有附件 ${existingAttachments.length} 个`
                )
              }
              description={
                !loadingExisting && !loadFailed && existingAttachments.length ? (
                  <Space size={[4, 4]} wrap>
                    {existingAttachments.map((attachment) => (
                      <Tag key={attachment.uuid}>{getAttachmentDisplayName(attachment)}</Tag>
                    ))}
                  </Space>
                ) : null
              }
              action={
                onOpenAttachments ? (
                  <Button size="small" icon={<LinkOutlined />} onClick={onOpenAttachments}>
                    查看
                  </Button>
                ) : null
              }
            />
          ) : (
            <Alert type="warning" showIcon message={noAccessText} />
          )
        ) : (
          <Alert type="info" showIcon message="新建记录保存后才会生成附件关联。" />
        )}

        {selectedFiles.length ? (
          <Space size={[4, 4]} wrap>
            <span style={{ color: "#595959" }}>待上传：</span>
            {selectedFiles.map((file) => (
              <Tag color="blue" key={`${file.name}-${file.size}-${file.lastModified}`}>
                {file.name}
              </Tag>
            ))}
            <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={clearPendingFiles}>
              移除待上传文件
            </Button>
          </Space>
        ) : null}

        <input
          ref={inputRef}
          type="file"
          accept={accept}
          multiple={multiple}
          style={{ display: "none" }}
          onChange={(event) => onPendingFilesChange?.(Array.from(event.target.files || []))}
        />
        <Space size={8} wrap>
          <Button icon={<UploadOutlined />} disabled={!canManage} onClick={() => inputRef?.current?.click()}>
            {chooseText}
          </Button>
          <span style={{ color: "#8c8c8c", fontSize: 12 }}>{helpText}</span>
        </Space>
        {!canManage ? <div style={{ color: "#d4380d", fontSize: 12 }}>{noManageText}</div> : null}
      </Space>
    </div>
  );
}
