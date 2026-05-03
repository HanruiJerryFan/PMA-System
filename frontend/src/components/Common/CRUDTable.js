import React, { useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import { Button, Card, Descriptions, Form, Input, Modal, Popconfirm, Space, Table, message } from "antd";
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined, SearchOutlined } from "@ant-design/icons";
import { getCurrentUser } from "../../api/auth";
import { hasAnyAuthority } from "../../utils/authorities";
import { resolvePagePermissions } from "../../utils/pagePermissions";

function resolveDefaultFormValues(formFields, editingRecord) {
  return formFields.reduce((accumulator, field) => {
    if (typeof field.getDefaultValue === "function") {
      accumulator[field.name] = field.getDefaultValue(editingRecord);
      return accumulator;
    }

    if (field.defaultValue !== undefined) {
      accumulator[field.name] = field.defaultValue;
      return accumulator;
    }

    if (field.name === "isActive" && field.valuePropName === "checked") {
      accumulator[field.name] = true;
    }

    return accumulator;
  }, {});
}

export default function CRUDTable({
  title,
  columns,
  dataSource = [],
  loading = false,
  onCreate,
  onUpdate,
  onDelete,
  formFields = [],
  searchFields = [],
  rowKey = "id",
  pagination = true,
  size = "middle",
  mapRecordToFormValues = (record) => record,
  transformValues = (values) => values,
  tableProps = {},
  extraActions = null,
  rowActions = null,
  enableView = true,
  showActions = true,
  detailFields = null,
  createAuthorities = null,
  updateAuthorities = null,
  deleteAuthorities = null,
  onModalCancel = null,
  onModalSuccess = null,
  onFilteredDataChange = null,
}) {
  const location = useLocation();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [viewingRecord, setViewingRecord] = useState(null);
  const [filteredData, setFilteredData] = useState(dataSource);
  const [currentUser, setCurrentUser] = useState(null);
  const [initialFormValues, setInitialFormValues] = useState({});
  const [modalInitKey, setModalInitKey] = useState(0);
  const [form] = Form.useForm();
  const [searchForm] = Form.useForm();
  const pagePermissions = useMemo(() => resolvePagePermissions(location.pathname), [location.pathname]);

  const normalizedSearchFields = useMemo(
    () => searchFields.map((field) => (typeof field === "string" ? { name: field, label: field } : field)),
    [searchFields]
  );

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

  const effectiveCreateAuthorities = createAuthorities ?? pagePermissions.manageAuthorities;
  const effectiveUpdateAuthorities = updateAuthorities ?? pagePermissions.manageAuthorities;
  const effectiveDeleteAuthorities = deleteAuthorities ?? pagePermissions.manageAuthorities;
  const defaultFormValues = useMemo(
    () => resolveDefaultFormValues(formFields, editingRecord),
    [editingRecord, formFields]
  );
  const hasDefaultFormValues = useMemo(() => Object.keys(defaultFormValues).length > 0, [defaultFormValues]);

  const canCreate = typeof onCreate === "function" && hasAnyAuthority(currentUser, effectiveCreateAuthorities);
  const canUpdate = typeof onUpdate === "function" && hasAnyAuthority(currentUser, effectiveUpdateAuthorities);
  const canDelete = typeof onDelete === "function" && hasAnyAuthority(currentUser, effectiveDeleteAuthorities);
  const canEditData = formFields.length > 0 && (canCreate || canUpdate);

  useEffect(() => {
    setFilteredData(dataSource);
  }, [dataSource]);

  useEffect(() => {
    onFilteredDataChange?.(filteredData);
  }, [filteredData, onFilteredDataChange]);

  useEffect(() => {
    if (!isModalVisible) {
      return;
    }

    if (editingRecord) {
      form.setFieldsValue(initialFormValues);
      return;
    }

    form.resetFields();
    if (Object.keys(initialFormValues).length > 0) {
      form.setFieldsValue(initialFormValues);
    }
  }, [editingRecord, form, initialFormValues, isModalVisible, modalInitKey]);

  const handleCreate = () => {
    if (!canCreate) {
      return;
    }
    const nextInitialFormValues = resolveDefaultFormValues(formFields, null);
    setEditingRecord(null);
    setInitialFormValues(nextInitialFormValues);
    setModalInitKey((current) => current + 1);
    setIsModalVisible(true);
  };

  const handleEdit = (record) => {
    if (!canUpdate) {
      return;
    }
    const nextInitialFormValues = mapRecordToFormValues(record);
    setEditingRecord(record);
    setInitialFormValues(nextInitialFormValues);
    setModalInitKey((current) => current + 1);
    setIsModalVisible(true);
  };

  const handleView = (record) => {
    if (!enableView) {
      return;
    }
    setViewingRecord(record);
  };

  const handleDelete = async (record) => {
    if (!canDelete) {
      return;
    }
    try {
      await onDelete(record[rowKey], record);
      message.success("删除成功");
    } catch (error) {
      message.error(error?.message || "删除失败");
    }
  };

  const handleSubmit = async () => {
    try {
      const rawValues = await form.validateFields();
      const values = transformValues(rawValues, editingRecord);
      let submitResult = null;

      if (editingRecord && canUpdate) {
        submitResult = await onUpdate(editingRecord[rowKey], values, editingRecord);
        message.success("保存成功");
      } else if (!editingRecord && canCreate) {
        submitResult = await onCreate(values);
        message.success("新增成功");
      }

      if (typeof onModalSuccess === "function") {
        await onModalSuccess({
          mode: editingRecord ? "edit" : "create",
          editingRecord,
          values,
          rawValues,
          result: submitResult,
        });
      }

      setIsModalVisible(false);
      form.resetFields();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(error?.message || (editingRecord ? "保存失败" : "新增失败"));
    }
  };

  const handleSearch = (values) => {
    const filtered = dataSource.filter((item) =>
      Object.entries(values).every(([key, value]) => {
        if (!value) {
          return true;
        }
        return String(item?.[key] ?? "")
          .toLowerCase()
          .includes(String(value).toLowerCase());
      })
    );
    setFilteredData(filtered);
  };

  const handleResetSearch = () => {
    searchForm.resetFields();
    setFilteredData(dataSource);
  };

  const handleRestoreInitialValues = () => {
    form.setFieldsValue(initialFormValues);
  };

  const handleSetToDefault = () => {
    form.setFieldsValue(defaultFormValues);
  };

  const handleModalAfterOpenChange = (open) => {
    if (!open) {
      return;
    }

    if (editingRecord) {
      form.setFieldsValue(initialFormValues);
      return;
    }

    form.resetFields();
    if (Object.keys(initialFormValues).length > 0) {
      form.setFieldsValue(initialFormValues);
    }
  };

  const actionColumn =
    showActions && (enableView || canUpdate || canDelete || typeof rowActions === "function")
      ? {
          title: "操作",
          key: "__action__",
          width: 280,
          render: (_, record) => {
            const customRowActions =
              typeof rowActions === "function"
                ? rowActions({
                    record,
                    currentUser,
                    canView: enableView,
                    canUpdate,
                    canDelete,
                  })
                : null;

            const normalizedRowActions = Array.isArray(customRowActions)
              ? customRowActions.filter(Boolean)
              : customRowActions
                ? [customRowActions]
                : [];

            return (
              <Space size="small" wrap>
                {enableView && (
                  <Button type="link" icon={<EyeOutlined />} onClick={() => handleView(record)} size="small">
                    查看
                  </Button>
                )}
                {canUpdate && (
                  <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)} size="small">
                    编辑
                  </Button>
                )}
                {normalizedRowActions}
                {canDelete && (
                  <Popconfirm
                    title="确定要删除这条记录吗？"
                    onConfirm={() => handleDelete(record)}
                    okText="确定"
                    cancelText="取消"
                  >
                    <Button type="link" danger icon={<DeleteOutlined />} size="small">
                      删除
                    </Button>
                  </Popconfirm>
                )}
              </Space>
            );
          },
        }
      : null;

  const sortableColumns = useMemo(
    () =>
      columns.map((column) => {
        if (column.sorter !== undefined || !column.dataIndex || typeof column.dataIndex !== "string") {
          return column;
        }

        return {
          ...column,
          sorter: (left, right) => {
            const leftValue = left?.[column.dataIndex];
            const rightValue = right?.[column.dataIndex];

            if (leftValue == null && rightValue == null) {
              return 0;
            }
            if (leftValue == null) {
              return -1;
            }
            if (rightValue == null) {
              return 1;
            }

            const leftNumber = Number(leftValue);
            const rightNumber = Number(rightValue);
            if (!Number.isNaN(leftNumber) && !Number.isNaN(rightNumber)) {
              return leftNumber - rightNumber;
            }

            return String(leftValue).localeCompare(String(rightValue), "zh-CN");
          },
        };
      }),
    [columns]
  );

  const finalColumns = actionColumn ? [actionColumn, ...sortableColumns] : sortableColumns;
  const renderedExtraActions =
    typeof extraActions === "function"
      ? extraActions({
          filteredData,
          dataSource,
          currentUser,
          canManage: hasAnyAuthority(currentUser, pagePermissions.manageAuthorities),
          canExport: hasAnyAuthority(currentUser, pagePermissions.exportAuthorities),
          hasAnyAuthority: (authorities) => hasAnyAuthority(currentUser, authorities),
        })
      : extraActions;

  const resolvedDetailFields = useMemo(() => {
    if (Array.isArray(detailFields) && detailFields.length > 0) {
      return detailFields;
    }

    return columns
      .filter((column) => column.dataIndex && typeof column.dataIndex === "string")
      .map((column) => ({
        key: column.dataIndex,
        label: column.title,
      }));
  }, [columns, detailFields]);

  return (
    <Card>
      <div
        style={{
          marginBottom: 16,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          gap: 16,
        }}
      >
        <div style={{ flex: 1 }}>
          {title ? <h2 style={{ margin: "0 0 16px" }}>{title}</h2> : null}
          {normalizedSearchFields.length > 0 && (
            <Form form={searchForm} layout="inline" onFinish={handleSearch}>
              {normalizedSearchFields.map((field) => (
                <Form.Item key={field.name} name={field.name} label={field.label}>
                  {typeof field.component === "function"
                    ? field.component({ searchForm })
                    : field.component || (
                        <Input placeholder={field.placeholder || `搜索${field.label}`} allowClear />
                      )}
                </Form.Item>
              ))}
              <Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                    搜索
                  </Button>
                  <Button onClick={handleResetSearch}>重置</Button>
                </Space>
              </Form.Item>
            </Form>
          )}
        </div>

        <Space>
          {renderedExtraActions}
          {canCreate && (
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新增
            </Button>
          )}
        </Space>
      </div>

      <Table
        columns={finalColumns}
        dataSource={filteredData}
        rowKey={rowKey}
        loading={loading}
        pagination={pagination}
        size={size}
        scroll={{ x: "max-content" }}
        {...tableProps}
      />

      {canEditData && (
        <Modal
          title={editingRecord ? `编辑${title || "记录"}` : `新增${title || "记录"}`}
          open={isModalVisible}
          afterOpenChange={handleModalAfterOpenChange}
          onCancel={() => {
            setIsModalVisible(false);
            form.resetFields();
            onModalCancel?.();
          }}
          destroyOnHidden
          width={640}
          footer={[
            editingRecord ? (
              <Button key="restore-initial" onClick={handleRestoreInitialValues}>
                恢复原值
              </Button>
            ) : null,
            hasDefaultFormValues ? (
              <Button key="set-default" onClick={handleSetToDefault}>
                设为默认
              </Button>
            ) : null,
            <Button
              key="cancel"
              onClick={() => {
                setIsModalVisible(false);
                form.resetFields();
                onModalCancel?.();
              }}
            >
              取消
            </Button>,
            <Button key="submit" type="primary" onClick={handleSubmit}>
              {editingRecord ? "保存" : "创建"}
            </Button>,
          ].filter(Boolean)}
        >
          <Form form={form} layout="vertical" preserve={false}>
            {formFields.map((field) =>
              field.renderOnly ? (
                <React.Fragment key={field.name || field.key || Math.random()}>
                  {field.render?.({ form, editingRecord })}
                </React.Fragment>
              ) : (
                <Form.Item
                  key={field.name}
                  name={field.name}
                  label={field.label}
                  rules={typeof field.rules === "function" ? field.rules({ form, editingRecord }) : field.rules}
                  valuePropName={field.valuePropName}
                >
                  {typeof field.component === "function"
                    ? field.component({ form, editingRecord })
                    : field.component || field.render?.({ form, editingRecord }) || field.render?.()}
                </Form.Item>
              )
            )}
          </Form>
        </Modal>
      )}

      {enableView && (
        <Modal
          title={viewingRecord ? `${title || "记录"}详情` : "详情"}
          open={Boolean(viewingRecord)}
          footer={null}
          onCancel={() => setViewingRecord(null)}
          destroyOnHidden
          width={760}
        >
          <Descriptions bordered column={1} size="small">
            {resolvedDetailFields.map((field) => (
              <Descriptions.Item key={field.key} label={field.label}>
                {viewingRecord?.[field.key] == null || viewingRecord?.[field.key] === ""
                  ? "-"
                  : String(viewingRecord[field.key])}
              </Descriptions.Item>
            ))}
          </Descriptions>
        </Modal>
      )}
    </Card>
  );
}
