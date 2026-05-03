import React, { useEffect, useMemo } from "react";
import dayjs from "dayjs";
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Statistic,
  Typography,
} from "antd";
import { DeleteOutlined, PlusOutlined } from "@ant-design/icons";
import {
  BUSINESS_CATEGORIES,
  CUSTOMER_TYPE_RULES,
  DOC_TYPES,
  buildWarehouseDocumentFormValues,
  docMeta,
  formatAmount,
  materialBrand,
} from "./warehouseDocumentUtils";

const { Text } = Typography;

function ItemEditor({ field, form, materialOptions, materialMap, onRemove }) {
  const rowValue = Form.useWatch(["items", field.name], form) || {};
  const material = rowValue.materialId ? materialMap[rowValue.materialId] || null : null;
  const amount =
    rowValue.unitPrice == null || rowValue.unitPrice === ""
      ? null
      : Number(rowValue.quantity || 0) * Number(rowValue.unitPrice || 0);

  useEffect(() => {
    if (!rowValue.materialId || !material) {
      return;
    }
    if (!rowValue.displayName) {
      form.setFieldValue(
        ["items", field.name, "displayName"],
        material.productName || "",
      );
    }
    if (!rowValue.displayModel) {
      form.setFieldValue(
        ["items", field.name, "displayModel"],
        material.productModel || material.specification || "",
      );
    }
  }, [
    field.name,
    form,
    material,
    rowValue.displayModel,
    rowValue.displayName,
    rowValue.materialId,
  ]);

  return (
    <Card
      size="small"
      title={`物料行 ${field.name + 1}`}
      extra={
        <Button danger type="text" icon={<DeleteOutlined />} onClick={onRemove}>
          删除
        </Button>
      }
      style={{ marginBottom: 12 }}
    >
      <Row gutter={12}>
        <Col xs={24} lg={10}>
          <Form.Item
            name={[field.name, "materialId"]}
            label="物料代码"
            rules={[{ required: true, message: "请选择物料" }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              filterOption={(input, option) =>
                option?.searchText?.includes(input.trim().toLowerCase())
              }
              options={materialOptions}
              placeholder="按物料代码 / 名称 / 型号搜索"
              onChange={(materialId) => {
                const selectedMaterial = materialMap[materialId];
                if (!selectedMaterial) {
                  return;
                }
                form.setFieldValue(
                  ["items", field.name, "displayName"],
                  selectedMaterial.productName || "",
                );
                form.setFieldValue(
                  ["items", field.name, "displayModel"],
                  selectedMaterial.productModel || selectedMaterial.specification || "",
                );
              }}
            />
          </Form.Item>
        </Col>
        <Col xs={24} lg={7}>
          <Form.Item
            name={[field.name, "displayName"]}
            label="显示名称"
            rules={[{ required: true, message: "请输入显示名称" }]}
          >
            <Input placeholder="请输入显示名称" />
          </Form.Item>
        </Col>
        <Col xs={24} lg={7}>
          <Form.Item
            name={[field.name, "displayModel"]}
            label="显示型号"
            rules={[{ required: true, message: "请输入显示型号" }]}
          >
            <Input placeholder="请输入显示型号" />
          </Form.Item>
        </Col>
      </Row>
      <Row gutter={12}>
        <Col xs={24} md={8} lg={4}>
          <Form.Item
            name={[field.name, "quantity"]}
            label="数量"
            rules={[{ required: true, message: "请输入数量" }]}
          >
            <InputNumber min={0.0001} precision={4} style={{ width: "100%" }} />
          </Form.Item>
        </Col>
        <Col xs={24} md={8} lg={4}>
          <Form.Item name={[field.name, "unitPrice"]} label="单价">
            <InputNumber min={0} precision={4} style={{ width: "100%" }} />
          </Form.Item>
        </Col>
        <Col xs={24} md={8} lg={6}>
          <Form.Item name={[field.name, "remark"]} label="备注">
            <Input placeholder="请输入备注" />
          </Form.Item>
        </Col>
      </Row>
      <Row gutter={[12, 12]}>
        <Col xs={12} md={6}>
          <Text type="secondary">材料名称</Text>
          <div>{material?.productName || "-"}</div>
        </Col>
        <Col xs={12} md={6}>
          <Text type="secondary">型号及规格</Text>
          <div>{material?.productModel || material?.specification || "-"}</div>
        </Col>
        <Col xs={12} md={4}>
          <Text type="secondary">品牌</Text>
          <div>{materialBrand(material)}</div>
        </Col>
        <Col xs={12} md={4}>
          <Text type="secondary">单位</Text>
          <div>{material?.unit || "-"}</div>
        </Col>
        <Col xs={12} md={4}>
          <Text type="secondary">金额</Text>
          <div>{amount == null ? "-" : formatAmount(amount)}</div>
        </Col>
      </Row>
    </Card>
  );
}

export default function WarehouseDocumentModal({
  open,
  initialDocument,
  defaultWarehouse,
  projects,
  warehouses,
  materials,
  customerOptions,
  currentUserId,
  saving,
  onCancel,
  onSubmit,
}) {
  const [form] = Form.useForm();
  const initialValues = useMemo(
    () => buildWarehouseDocumentFormValues(initialDocument, defaultWarehouse?.id),
    [defaultWarehouse?.id, initialDocument],
  );
  const currentDocType = Form.useWatch("docType", form) || "INBOUND_NOTE";
  const currentDocDate = Form.useWatch("docDate", form);
  const watchedItems = Form.useWatch("items", form);
  const currentItems = watchedItems ?? [];
  const meta = docMeta(currentDocType);

  const materialMap = useMemo(
    () => Object.fromEntries(materials.map((item) => [item.uuid, item])),
    [materials],
  );
  const customerOptionMap = useMemo(
    () => Object.fromEntries(customerOptions.map((item) => [item.uuid, item])),
    [customerOptions],
  );
  const allowedCustomerTypeCodes = useMemo(
    () => CUSTOMER_TYPE_RULES[currentDocType] || CUSTOMER_TYPE_RULES.INBOUND_NOTE,
    [currentDocType],
  );
  const filteredCustomerOptions = useMemo(() => {
    const allowedSet = new Set(allowedCustomerTypeCodes);
    return customerOptions
      .filter((item) => allowedSet.has(String(item.customerTypeCode || "")))
      .map((item) => ({
        value: item.uuid,
        label: item.customerName || "-",
        searchText: [item.customerName, item.customerCode, item.customerTypeName]
          .filter(Boolean)
          .join(" ")
          .toLowerCase(),
      }));
  }, [allowedCustomerTypeCodes, customerOptions]);
  const materialOptions = useMemo(
    () =>
      materials
        .filter((item) => item.isActive !== false)
        .map((item) => ({
          value: item.uuid,
          label: [
            item.materialCode,
            item.productName,
            item.productModel,
            item.bandCode || item.frequency,
          ]
            .filter(Boolean)
            .join(" / "),
          searchText: [
            item.materialCode,
            item.productName,
            item.productModel,
            item.bandCode,
            item.frequency,
            item.brandCode,
            item.manufacturer,
          ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase(),
        })),
    [materials],
  );
  const totals = useMemo(
    () =>
      (watchedItems ?? []).reduce(
        (acc, item) => {
          acc.quantity += Number(item?.quantity || 0);
          if (item?.unitPrice != null && item?.unitPrice !== "") {
            acc.amount += Number(item.quantity || 0) * Number(item.unitPrice || 0);
          }
          return acc;
        },
        { quantity: 0, amount: 0 },
      ),
    [watchedItems],
  );
  const docNumberPreview = useMemo(() => {
    const prefix = currentDocType === "INBOUND_NOTE" ? "RKD" : "FHD";
    const yearMonth = dayjs(currentDocDate || dayjs()).format("YYYYMM");
    return `${prefix}${yearMonth}001`;
  }, [currentDocDate, currentDocType]);

  useEffect(() => {
    if (!open || !defaultWarehouse) {
      return;
    }
    if (!form.getFieldValue("warehouseId")) {
      form.setFieldValue("warehouseId", defaultWarehouse.id);
    }
  }, [defaultWarehouse, form, open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const currentCustomerId = form.getFieldValue("counterpartyCustomerId");
    if (!currentCustomerId) {
      return;
    }
    const currentCustomer = customerOptionMap[currentCustomerId];
    if (
      !currentCustomer ||
      !allowedCustomerTypeCodes.includes(String(currentCustomer.customerTypeCode || ""))
    ) {
      form.setFieldsValue({ counterpartyCustomerId: undefined });
    }
  }, [allowedCustomerTypeCodes, customerOptionMap, form, open]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const payload = {
      docNumber: values.docNumber,
      docType: values.docType,
      businessCategory: values.businessCategory,
      projectId: values.projectId || null,
      warehouseId: values.warehouseId || defaultWarehouse?.id || null,
      counterpartyCustomerId: values.counterpartyCustomerId,
      counterpartyAddress: values.counterpartyAddress || null,
      counterpartyContact: values.counterpartyContact || null,
      contractNumber: values.contractNumber || null,
      docDate: values.docDate ? values.docDate.toISOString() : null,
      remark: values.remark || null,
      createUser: initialDocument?.createUser || currentUserId || null,
      updateUser: currentUserId || null,
      items: (values.items || [])
        .filter((item) => item?.materialId && item?.quantity)
        .map((item) => ({
          materialId: item.materialId,
          displayName: item.displayName || null,
          displayModel: item.displayModel || null,
          quantity: Number(item.quantity),
          unitPrice:
            item.unitPrice == null || item.unitPrice === "" ? null : Number(item.unitPrice),
          remark: item.remark || null,
        })),
    };
    await onSubmit(payload, initialDocument);
  };

  return (
    <Modal
      title={initialDocument ? "编辑出入库单" : "新增出入库单"}
      open={open}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
      width={1180}
    >
      <Form form={form} layout="vertical" preserve={false} initialValues={initialValues}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="物料按物料代码引用，保存时以后端物料主数据自动回填名称、型号、品牌、单位等快照字段。"
        />
        <Row gutter={12}>
          <Col xs={24} md={8}>
            <Form.Item
              name="docType"
              label="单据类型"
              rules={[{ required: true, message: "请选择单据类型" }]}
            >
              <Select
                options={Object.entries(DOC_TYPES).map(([value, item]) => ({
                  value,
                  label: item.label,
                }))}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item
              name="docNumber"
              label={meta.numberLabel}
              extra={
                initialDocument
                  ? "单据编号创建后不可修改"
                  : `保存时自动生成，规则如：${docNumberPreview}`
              }
            >
              <Input
                placeholder={
                  initialDocument
                    ? meta.placeholder
                    : `保存时自动生成，如 ${docNumberPreview}`
                }
                disabled
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item
              name="businessCategory"
              label="业务类别"
              rules={[{ required: true, message: "请选择业务类别" }]}
            >
              <Select
                options={Object.entries(BUSINESS_CATEGORIES).map(([value, label]) => ({
                  value,
                  label,
                }))}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="projectId" label="项目">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                placeholder="请选择项目"
                filterOption={(input, option) =>
                  option?.searchText?.includes(input.trim().toLowerCase())
                }
                options={projects.map((item) => ({
                  value: item.uuid,
                  label: item.projectName || "-",
                  searchText: [item.projectName, item.projectNumber]
                    .filter(Boolean)
                    .join(" ")
                    .toLowerCase(),
                }))}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            {defaultWarehouse ? (
              <>
                <Form.Item label="仓库">
                  <Input value={defaultWarehouse.warehouseName} disabled />
                </Form.Item>
                <Form.Item name="warehouseId" hidden>
                  <Input />
                </Form.Item>
              </>
            ) : (
              <Form.Item
                name="warehouseId"
                label="仓库"
                rules={[{ required: true, message: "请选择仓库" }]}
              >
                <Select placeholder="请选择仓库">
                  {warehouses.map((item) => (
                    <Select.Option key={item.id} value={item.id}>
                      {item.warehouseName}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            )}
          </Col>
          <Col xs={24} md={8}>
            <Form.Item
              name="counterpartyCustomerId"
              label={meta.partyName}
              rules={[{ required: true, message: `请选择${meta.partyName}` }]}
            >
              <Select
                showSearch
                optionFilterProp="label"
                options={filteredCustomerOptions}
                placeholder={`请选择${meta.partyName}`}
                filterOption={(input, option) =>
                  option?.searchText?.includes(input.trim().toLowerCase())
                }
                onChange={(customerId) => {
                  const customer = customerOptionMap[customerId];
                  const nextAddress = customer?.officeAddress || customer?.registerAddress || "";
                  if (nextAddress) {
                    form.setFieldValue("counterpartyAddress", nextAddress);
                  }
                }}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item name="counterpartyAddress" label={meta.partyAddress}>
              <Input placeholder={`请输入${meta.partyAddress}`} />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item name="counterpartyContact" label={meta.partyContact}>
              <Input placeholder={`请输入${meta.partyContact}`} />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item name="contractNumber" label="合同编号">
              <Input placeholder="请输入合同编号" />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item
              name="docDate"
              label={meta.dateLabel}
              rules={[{ required: true, message: "请选择单据日期" }]}
            >
              <DatePicker style={{ width: "100%" }} />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item name="remark" label="整单备注">
              <Input placeholder="请输入整单备注" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={[16, 16]} style={{ marginBottom: 12 }}>
          <Col xs={24} sm={8}>
            <Card size="small">
              <Statistic title="明细条数" value={currentItems.length} />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card size="small">
              <Statistic title="总数量" value={totals.quantity} precision={4} />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card size="small">
              <Statistic title="预计总金额" value={totals.amount} precision={2} />
            </Card>
          </Col>
        </Row>
        <Form.List name="items">
          {(fields, { add, remove }) => (
            <>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: 12,
                }}
              >
                <div style={{ fontWeight: 600 }}>物料明细</div>
                <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({})}>
                  添加物料
                </Button>
              </div>
              {fields.map((field) => (
                <ItemEditor
                  key={field.key}
                  field={field}
                  form={form}
                  materialOptions={materialOptions}
                  materialMap={materialMap}
                  onRemove={() => remove(field.name)}
                />
              ))}
              {!fields.length ? <Empty description="请至少添加一条物料明细" /> : null}
            </>
          )}
        </Form.List>
      </Form>
    </Modal>
  );
}
