import React, { useEffect, useMemo, useState } from "react";
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
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from "antd";
import { CheckCircleOutlined, DeleteOutlined, DownloadOutlined, EditOutlined, FilePdfOutlined, PlusOutlined, SearchOutlined } from "@ant-design/icons";
import { getCurrentUser } from "../../api/auth";
import { inventoryAPI, permissionAPI } from "../../api/modules";
import { downloadApiFile, downloadExcel, downloadHtmlExcel, resolveBlobErrorMessage } from "../../utils/exporters";
import { formatProjectOptionLabel } from "../../utils/projectLabels";
import { sortSelectItems } from "../../utils/selectSorting";
import { queryWarehouseDocuments } from "./warehouseDocumentQuery";
import { hasAnyAuthority } from "../../utils/authorities";
import { resolvePagePermissions } from "../../utils/pagePermissions";
import { canMaintainEntryAuditUsers, getUserLabel } from "../../utils/entryAudit";
import WarehouseDocumentModal from "./WarehouseDocumentModal";
import {
  DOC_TYPES,
  isGeneratedInbound,
  isTransferDocument,
  categoryLabel,
  docMeta,
  formatAmount,
  formatUnitPrice,
  normalize,
  toChineseUppercaseRmb,
} from "./warehouseDocumentUtils";

export default function WarehouseDoc() {
  const [currentUser, setCurrentUser] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [projects, setProjects] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [customerOptions, setCustomerOptions] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedDocNumber, setSelectedDocNumber] = useState(null);
  const [editingDoc, setEditingDoc] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [queryForm] = Form.useForm();
  const [queryFilters, setQueryFilters] = useState({});
  const [documentPage, setDocumentPage] = useState(1);

  const pagePermissions = useMemo(() => resolvePagePermissions("/inventory/warehouse"), []);
  const canManage = hasAnyAuthority(currentUser, ["inventory.warehouse-doc.entry", ...pagePermissions.manageAuthorities]);
  const canAudit = hasAnyAuthority(currentUser, ["inventory.warehouse-doc.audit", ...pagePermissions.manageAuthorities]);
  const canMaintainEntryAudit = canMaintainEntryAuditUsers(currentUser);
  const userMap = useMemo(() => Object.fromEntries(users.map((item) => [item.id, getUserLabel(item)])), [users]);
  const projectMap = useMemo(
    () =>
      Object.fromEntries(
        projects.map((item) => [item.uuid, formatProjectOptionLabel(item)]),
      ),
    [projects],
  );
  const warehouseMap = useMemo(
    () => Object.fromEntries(warehouses.map((item) => [String(item.id), [item.warehouseCode, item.warehouseName].filter(Boolean).join(" - ")])),
    [warehouses],
  );
  const defaultWarehouse = useMemo(
    () => (warehouses.length === 1 ? warehouses[0] : null),
    [warehouses],
  );
  const decoratedDocs = useMemo(
    () =>
      documents.map((item) => ({
        ...item,
        docTypeLabel: docMeta(item.docType).label,
        businessCategoryLabel: categoryLabel(item.businessCategory),
        projectLabel: item.projectId ? projectMap[item.projectId] || item.projectId : "-",
        warehouseLabel:
          item.warehouseId != null ? warehouseMap[String(item.warehouseId)] || item.warehouseId : "-",
        entryUserName: item.entryUserName || userMap[item.entryUser] || "",
        auditorUserName: item.auditorUserName || userMap[item.auditorUser] || "",
        itemCount: item.items?.length || 0,
        relatedDocNumber: isGeneratedInbound(item) ? item.sourceRefId : documents.find((doc) => isGeneratedInbound(doc) && doc.sourceRefId === item.docNumber)?.docNumber,
      })),
    [documents, projectMap, userMap, warehouseMap],
  );
  const queryResult = useMemo(() => queryWarehouseDocuments(decoratedDocs, queryFilters), [decoratedDocs, queryFilters]);
  const filteredDocs = queryResult.documents;
  const summary = queryResult.summary;
  const selectedDoc = useMemo(
    () => filteredDocs.find((item) => item.docNumber === selectedDocNumber) || null,
    [filteredDocs, selectedDocNumber],
  );

  useEffect(() => {
    setSelectedDocNumber((current) => filteredDocs.some((doc) => doc.docNumber === current) ? current : filteredDocs[0]?.docNumber || null);
  }, [filteredDocs]);

  const applyQuery = (values) => {
    setQueryFilters(values);
    setDocumentPage(1);
  };

  const resetQuery = () => {
    queryForm.resetFields();
    applyQuery({});
  };

  const exportQueryStatistics = () => {
    downloadExcel(
      `warehouse-document-statistics-${dayjs().format("YYYY-MM-DD")}.xls`, "出入库查询统计",
      ["物料编码", "物料名称", "型号", "品牌", "单位", "单据数", "入库数量", "出库数量", "入库金额", "出库金额"],
      queryResult.materials.map((item) => [item.materialCode, item.materialName, item.model, item.brand, item.unit,
        item.documentCount, item.inboundQuantity, item.outboundQuantity, item.inboundAmount, item.outboundAmount]),
    );
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const user = await getCurrentUser().catch(() => null);
      const [docResponse, projectResponse, warehouseResponse, materialResponse, customerResponse, userResponse] =
        await Promise.all([
          inventoryAPI.getWarehouseDocs(),
          inventoryAPI.getWarehouseDocProjectOptions(),
          inventoryAPI.getWarehouses(),
          inventoryAPI.getWarehouseDocMaterialOptions(),
          inventoryAPI.getWarehouseDocCustomerOptions(),
          permissionAPI.getUserOptions().catch(() => []),
        ]);
      const docData = normalize(docResponse);
      setCurrentUser(user);
      setDocuments(docData);
      setProjects(normalize(projectResponse));
      setWarehouses(normalize(warehouseResponse));
      setMaterials(normalize(materialResponse));
      setCustomerOptions(normalize(customerResponse));
      setUsers(Array.isArray(userResponse) ? userResponse : normalize(userResponse));
      setSelectedDocNumber((current) =>
        current && docData.some((item) => item.docNumber === current)
          ? current
          : docData[0]?.docNumber || null,
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const openModal = (record = null) => {
    if (!canManage) {
      message.error("没有维护出入库单的权限");
      return;
    }
    setEditingDoc(record);
    setModalVisible(true);
  };

  const closeModal = () => {
    setModalVisible(false);
    setEditingDoc(null);
  };

  const submitDocument = async (payload, initialDocument) => {
    setSaving(true);
    try {
      if (initialDocument?.docNumber) {
        const result = normalize(await inventoryAPI.updateWarehouseDoc(initialDocument.docNumber, payload));
        setSelectedDocNumber(result?.docNumber || initialDocument.docNumber);
        message.success("出入库单已更新");
      } else {
        const result = normalize(await inventoryAPI.createWarehouseDoc(payload));
        setSelectedDocNumber(result?.docNumber || null);
        message.success(isTransferDocument(payload.docType) ? "划拨单已创建，调入仓库入库单已自动生成" : "出入库单已创建");
      }
      closeModal();
      await fetchData();
    } catch (error) {
      message.error(error?.message || "保存出入库单失败");
    } finally {
      setSaving(false);
    }
  };

  const deleteDocument = async (docNumber) => {
    if (!canManage) {
      message.error("没有维护出入库单的权限");
      return;
    }
    await inventoryAPI.deleteWarehouseDoc(docNumber);
    message.success("删除成功");
    await fetchData();
  };

  const auditDocument = async (docNumber) => {
    if (!canAudit) {
      message.error("没有审核出入库单的权限");
      return;
    }
    await inventoryAPI.auditWarehouseDoc(docNumber);
    message.success("出入库单已审核");
    await fetchData();
  };

  const printDocument = (doc = selectedDoc) => {
    if (!doc) {
      return;
    }
    const popup = window.open("", "_blank", "width=1280,height=900");
    if (!popup) {
      message.error("浏览器拦截了打印窗口，请允许弹窗后重试");
      return;
    }
    popup.document.write(buildWarehouseDocumentHtml(doc, { autoPrint: true }));
    popup.document.close();
  };

  const exportDocumentPdf = async (doc = selectedDoc) => {
    if (!doc) {
      return;
    }
    try {
      await downloadApiFile(
        inventoryAPI.downloadWarehouseDocPdf(doc.docNumber),
        `${doc.docNumber || docMeta(doc.docType).label}.pdf`,
        "application/pdf"
      );
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "导出 PDF 失败"));
    }
  };

  const exportDocumentExcel = (doc = selectedDoc) => {
    if (!doc) {
      return;
    }
    downloadHtmlExcel(`${doc.docNumber || docMeta(doc.docType).label}.xls`, buildWarehouseDocumentHtml(doc));
  };

  const columns = [
    {
      title: "单据编号",
      dataIndex: "docNumber",
      key: "docNumber",
      width: 170,
      sorter: (left, right) =>
        (left.docNumber || "").localeCompare(right.docNumber || "", "zh-CN", { numeric: true }),
    },
    {
      title: "单据类型",
      dataIndex: "docTypeLabel",
      key: "docTypeLabel",
      width: 120,
      filters: Object.entries(DOC_TYPES).map(([value, meta]) => ({ text: meta.label, value })),
      filterMultiple: false,
      filteredValue: queryFilters.docType ? [queryFilters.docType] : null,
      onFilter: (value, record) => record.docType === value,
      render: (value, record) => <Tag color={docMeta(record.docType).tagColor}>{value}</Tag>,
    },
    { title: "业务类别", dataIndex: "businessCategoryLabel", key: "businessCategoryLabel", width: 180 },
    { title: "关联单据", dataIndex: "relatedDocNumber", key: "relatedDocNumber", width: 180, render: (value) => value || "-" },
    { title: "项目", dataIndex: "projectLabel", key: "projectLabel", width: 220 },
    { title: "仓库", dataIndex: "warehouseLabel", key: "warehouseLabel", width: 180 },
    { title: "往来单位", dataIndex: "counterpartyName", key: "counterpartyName", width: 220 },
    {
      title: "单据日期",
      dataIndex: "docDate",
      key: "docDate",
      width: 140,
      render: (value) => (value ? dayjs(value).format("YYYY-MM-DD") : "-"),
    },
    { title: "单据总金额", dataIndex: "totalAmount", key: "totalAmount", width: 120, render: formatAmount },
    { title: "匹配数量", dataIndex: "matchedQuantity", key: "matchedQuantity", width: 120 },
    { title: "匹配金额", dataIndex: "matchedAmount", key: "matchedAmount", width: 120, render: formatAmount },
    { title: "明细数", dataIndex: "itemCount", key: "itemCount", width: 100 },
    { title: "录入人", dataIndex: "entryUserName", key: "entryUserName", width: 120, render: (value) => value || "-" },
    { title: "审核人", dataIndex: "auditorUserName", key: "auditorUserName", width: 120, render: (value) => value || "-" },
    {
      title: "操作",
      key: "action",
      width: 300,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" icon={<FilePdfOutlined />} onClick={() => printDocument(record)}>
            打印
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            disabled={!canManage || isGeneratedInbound(record)}
            onClick={() => openModal(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<CheckCircleOutlined />}
            disabled={!canAudit || Boolean(record.auditorUser) || isGeneratedInbound(record)}
            onClick={() => auditDocument(record.docNumber)}
          >
            审核
          </Button>
          <Popconfirm
            title="确定删除这张出入库单吗？"
            onConfirm={() => deleteDocument(record.docNumber)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />} disabled={!canManage || isGeneratedInbound(record)}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const previewMeta = docMeta(selectedDoc?.docType, selectedDoc?.sourceRefType);
  const previewRows = selectedDoc ? buildWarehouseDocumentRows(selectedDoc) : [];

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card title="出入库查询统计">
        <Form form={queryForm} layout="inline" onFinish={applyQuery} style={{ rowGap: 16 }}>
          <Form.Item name="projectId" label="项目">
            <Select allowClear showSearch optionFilterProp="label" placeholder="全部项目" style={{ width: 260 }}
              options={sortSelectItems(projects, ["projectNumber"]).map((item) => ({ value: item.uuid, label: formatProjectOptionLabel(item) }))} />
          </Form.Item>
          <Form.Item name="txnType" label="出入库">
            <Select allowClear placeholder="全部" style={{ width: 140 }} options={[
              { value: "IN", label: "入库" }, { value: "OUT", label: "出库（含划拨）" },
            ]} />
          </Form.Item>
          <Form.Item name="warehouseId" label="仓库">
            <Select allowClear placeholder="全部仓库" style={{ width: 230 }} options={sortSelectItems(warehouses, ["warehouseCode"]).map((item) => ({ value: item.id, label: warehouseMap[String(item.id)] }))} />
          </Form.Item>
          <Form.Item name="docDateRange" label="单据日期">
            <DatePicker.RangePicker placeholder={["开始日期", "结束日期"]} />
          </Form.Item>
          <Form.Item name="materialCode" label="物料编码">
            <Input allowClear placeholder="支持部分编码" style={{ width: 180 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
              <Button onClick={resetQuery}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
      <Row gutter={[16, 16]}>
        {[
          { key: "total", title: "单据总数" },
          { key: "inbound", title: "入库单" },
          { key: "outbound", title: "出库单" },
          { key: "balanceQuantity", title: "库存数量", precision: 4 },
          { key: "inboundQuantity", title: "入库数量", precision: 4 },
          { key: "outboundQuantity", title: "出库数量", precision: 4 },
          { key: "amountDifference", title: "出入库金额差额", precision: 2, suffix: "元" },
          { key: "inboundAmount", title: "入库金额", precision: 2, suffix: "元" },
          { key: "outboundAmount", title: "出库金额", precision: 2, suffix: "元" },
        ].map(({ key, ...statistic }) => (
          <Col span={8} key={key}>
            <Card>
              <Statistic {...statistic} value={summary[key]} />
            </Card>
          </Col>
        ))}
      </Row>

      <Card title="物料统计" extra={<Button icon={<DownloadOutlined />} disabled={!queryResult.materials.length} onClick={exportQueryStatistics}>导出统计</Button>}>
        <Table rowKey="key" loading={loading} dataSource={queryResult.materials} size="middle" scroll={{ x: "max-content" }}
          pagination={{ pageSize: 10 }} columns={[
            { title: "物料编码", dataIndex: "materialCode", width: 140 },
            { title: "物料名称", dataIndex: "materialName", width: 200 },
            { title: "型号", dataIndex: "model", width: 160 },
            { title: "品牌", dataIndex: "brand", width: 120 },
            { title: "单位", dataIndex: "unit", width: 80 },
            { title: "单据数", dataIndex: "documentCount", width: 100 },
            { title: "入库数量", dataIndex: "inboundQuantity", width: 120 },
            { title: "出库数量", dataIndex: "outboundQuantity", width: 120 },
            { title: "入库金额", dataIndex: "inboundAmount", width: 140, render: formatAmount },
            { title: "出库金额", dataIndex: "outboundAmount", width: 140, render: formatAmount },
          ]} />
      </Card>

      <Card
        title="出入库单列表"
        extra={
          <Space>
            <Button icon={<FilePdfOutlined />} disabled={!selectedDoc} onClick={() => printDocument(selectedDoc)}>
              打印当前单据
            </Button>
            <Button icon={<DownloadOutlined />} disabled={!selectedDoc} onClick={() => exportDocumentExcel(selectedDoc)}>
              导出 Excel
            </Button>
            <Button icon={<FilePdfOutlined />} disabled={!selectedDoc} onClick={() => exportDocumentPdf(selectedDoc)}>
              导出 PDF
            </Button>
            <Button type="primary" icon={<PlusOutlined />} disabled={!canManage} onClick={() => openModal()}>
              新增单据
            </Button>
          </Space>
        }
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="划拨会生成调出单和关联入库单，由划拨单统一修改、审核和删除。统计按查询条件计算，可按仓库查看；物料编码只统计匹配明细，单据打印保留完整内容。"
        />
        <Table
          rowKey="docNumber"
          loading={loading}
          dataSource={filteredDocs}
          columns={columns}
          pagination={{ pageSize: 8, current: documentPage }}
          onChange={(pagination, filters, sorter, extra) => {
            if (extra.action === "filter") {
              const docType = filters.docTypeLabel?.[0];
              applyQuery({ ...queryFilters, docType });
            } else {
              setDocumentPage(pagination.current);
            }
          }}
          scroll={{ x: "max-content" }}
          rowClassName={(record) => (record.docNumber === selectedDocNumber ? "ant-table-row-selected" : "")}
          onRow={(record) => ({
            onClick: () => setSelectedDocNumber(record.docNumber),
            style: { cursor: "pointer" },
          })}
        />
      </Card>

      <Card title={selectedDoc ? `${selectedDoc.docTypeLabel}预览` : "单据预览"}>
        {selectedDoc ? (
          <div style={{ overflowX: "auto" }}>
            <div style={{ minWidth: 1120, border: "2px solid #222", background: "#fff" }}>
              <div
                style={{
                  textAlign: "center",
                  fontSize: 34,
                  letterSpacing: 12,
                  padding: "12px 0 8px",
                  fontFamily: '"SimSun", "Microsoft YaHei", serif',
                }}
              >
                {selectedDoc.docTypeLabel}
              </div>
              <table style={metaTableStyle}>
                <colgroup>
                  {documentMetaColumnWidths.map((width, index) => (
                    <col key={`${width}-${index}`} style={{ width }} />
                  ))}
                </colgroup>
                <tbody>
                  <tr>
                    <td style={cellLabel}>项目名称</td>
                    <td style={compactProjectCell}>
                      {selectedDoc.projectLabel || "-"}
                    </td>
                    <td style={wideLabelCell}>{previewMeta.partyBlock}</td>
                    <td style={cellValue} colSpan={2}>
                      {selectedDoc.counterpartyName || "-"}
                      <br />
                      {selectedDoc.counterpartyAddress || ""}
                      <br />
                      {selectedDoc.counterpartyContact || ""}
                    </td>
                    <td style={cellLabel}>{previewMeta.numberLabel}</td>
                    <td style={nowrapValueCell}>{selectedDoc.docNumber}</td>
                    <td style={cellLabel}>{previewMeta.dateLabel}</td>
                    <td style={nowrapValueCell}>{selectedDoc.docDate ? dayjs(selectedDoc.docDate).format("YYYY年M月D日") : "-"}</td>
                  </tr>
                </tbody>
              </table>
              <table style={detailTableStyle}>
                <colgroup>
                  {documentColumnWidths.map((width, index) => (
                    <col key={`${width}-${index}`} style={{ width }} />
                  ))}
                </colgroup>
                <tbody>
                  <tr>
                    {["序号", "材料名称", "型号及规格", "品牌", "单位", "数量", "单价", "金额", "备注"].map(
                      (label) => (
                        <th key={label} style={headCell}>
                          {label}
                        </th>
                      ),
                    )}
                  </tr>
                  {previewRows.map((item, index) => (
                    <tr key={item.uuid || `blank-${index}`}>
                      <td style={centerCell}>{index + 1}</td>
                      <td style={bodyCell}>{item.displayName || item.materialName || ""}</td>
                      <td style={bodyCell}>{item.displayModel || item.model || ""}</td>
                      <td style={centerCell}>{item.brand || ""}</td>
                      <td style={centerCell}>{item.unit || ""}</td>
                      <td style={rightCell}>{item.quantity ?? ""}</td>
                      <td style={rightCell}>{item.unitPrice != null ? formatUnitPrice(item.unitPrice) : ""}</td>
                      <td style={rightCell}>{item.amount != null ? formatAmount(item.amount) : ""}</td>
                      <td style={bodyCell}>{item.remark || ""}</td>
                    </tr>
                  ))}
                  <tr>
                    <td style={{ ...bodyCell, fontWeight: 700 }} colSpan={7}>
                      合计：{toChineseUppercaseRmb(selectedDoc.totalAmount || 0)}
                    </td>
                    <td style={{ ...bodyCell, textAlign: "right", fontWeight: 700 }} colSpan={2}>
                      ￥{formatAmount(selectedDoc.totalAmount || 0)}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <Empty description="请选择一张出入库单查看预览" />
        )}
      </Card>

      <WarehouseDocumentModal
        key={editingDoc?.docNumber || "new"}
        open={modalVisible}
        initialDocument={editingDoc}
        defaultWarehouse={defaultWarehouse}
        projects={projects}
        warehouses={warehouses}
        materials={materials}
        customerOptions={customerOptions}
        users={users}
        canMaintainEntryAudit={canMaintainEntryAudit}
        saving={saving}
        onCancel={closeModal}
        onSubmit={submitDocument}
      />
    </Space>
  );
}

const tableStyle = { width: "100%", borderCollapse: "collapse", tableLayout: "fixed" };
const metaTableStyle = { ...tableStyle, marginBottom: -1 };
const detailTableStyle = tableStyle;
const keepWordStyle = { wordBreak: "keep-all", overflowWrap: "break-word" };
const noWrapStyle = { whiteSpace: "nowrap", wordBreak: "keep-all", overflowWrap: "normal" };
const cellLabel = { border: "1px solid #222", padding: 8, textAlign: "center", fontWeight: 700, ...noWrapStyle };
const wideLabelCell = { ...cellLabel, padding: "8px 4px", fontSize: 13 };
const cellValue = { border: "1px solid #222", padding: 8, lineHeight: 1.6, ...keepWordStyle };
const compactProjectCell = { ...cellValue, fontSize: 13 };
const nowrapValueCell = { ...cellValue, textAlign: "center", ...noWrapStyle };
const headCell = { border: "1px solid #222", padding: 8, textAlign: "center", background: "#fafafa", ...noWrapStyle };
const bodyCell = { border: "1px solid #222", padding: 8, minHeight: 36, ...keepWordStyle };
const centerCell = { ...bodyCell, textAlign: "center", ...noWrapStyle };
const rightCell = { ...bodyCell, textAlign: "right", ...noWrapStyle };
const documentColumnWidths = ["6%", "25%", "12%", "12%", "7%", "9%", "10%", "10%", "9%"];
const documentMetaColumnWidths = documentColumnWidths;
const documentMetaColumnGroupHtml = `<colgroup>${documentMetaColumnWidths
  .map((width) => `<col style="width:${width}" />`)
  .join("")}</colgroup>`;
const documentColumnGroupHtml = `<colgroup>${documentColumnWidths
  .map((width) => `<col style="width:${width}" />`)
  .join("")}</colgroup>`;

function buildWarehouseDocumentHtml(doc, { autoPrint = false } = {}) {
  const previewMeta = docMeta(doc.docType, doc.sourceRefType);
  const rows = buildWarehouseDocumentRows(doc);
  return `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>${escapeHtml(previewMeta.label)}</title><style>
    @page{margin:6mm}
    body{font-family:"SimSun","Microsoft YaHei",serif;margin:0;padding:12px;color:#111}
    .doc-sheet{width:100%;box-sizing:border-box}
    .doc-title{text-align:center;font-size:34px;letter-spacing:12px;margin:8px 0 14px}
    table{width:100%;border-collapse:collapse;table-layout:fixed}
    .meta-table{margin-bottom:-2px}
    td,th{border:2px solid #222;padding:6px;font-size:14px;vertical-align:middle;word-break:keep-all;overflow-wrap:normal}
    th{background:#fafafa;font-weight:700;white-space:nowrap}
    .label,.nowrap{white-space:nowrap}
    .wide-label{font-size:13px}
    .project-cell{font-size:13px}
    .text-cell,.party-cell{white-space:normal;word-break:keep-all;overflow-wrap:break-word}
    .party-cell{line-height:1.45}
    .center{text-align:center}.right{text-align:right}.strong{font-size:18px;font-weight:600}
    .model-cell{font-size:13px}
    @media print{
      body{padding:0}
      .doc-title{font-size:30px;letter-spacing:8px;margin:0 0 10px}
      td,th{padding:4px 5px;font-size:12px}
      .wide-label{font-size:11px}
      .project-cell{font-size:11px}
      .model-cell{font-size:11px}
      .strong{font-size:15px}
    }
  </style></head><body><div class="doc-sheet"><div class="doc-title">${escapeHtml(previewMeta.label)}</div><table class="meta-table">
    ${documentMetaColumnGroupHtml}
    <tr>
      <td class="label center"><strong>项目名称</strong></td>
      <td class="text-cell project-cell">${escapeHtml(doc.projectLabel || "")}</td>
      <td class="label center wide-label"><strong>${escapeHtml(previewMeta.partyBlock)}</strong></td>
      <td class="party-cell" colspan="2">${formatCounterpartyHtml(doc)}</td>
      <td class="label center"><strong>${escapeHtml(previewMeta.numberLabel)}</strong></td>
      <td class="center nowrap">${escapeHtml(doc.docNumber || "")}</td>
      <td class="label center"><strong>${escapeHtml(previewMeta.dateLabel)}</strong></td>
      <td class="center nowrap">${escapeHtml(formatWarehouseDocDate(doc))}</td>
    </tr>
  </table><table class="detail-table">
    ${documentColumnGroupHtml}
    <tr class="center"><th>序号</th><th>材料名称</th><th>型号及规格</th><th>品牌</th><th>单位</th><th>数量</th><th>单价</th><th>金额</th><th>备注</th></tr>
    ${rows.map((item, index) => buildWarehouseDocumentItemRow(item, index)).join("")}
    <tr><td colspan="7" class="center strong">合计：${escapeHtml(toChineseUppercaseRmb(doc.totalAmount || 0))}</td><td colspan="2" class="right strong">￥${escapeHtml(formatAmount(doc.totalAmount || 0))}</td></tr>
  </table></div>${autoPrint ? "<script>window.onload=function(){window.print();}</script>" : ""}</body></html>`;
}

function buildWarehouseDocumentItemRow(item, index) {
  return `<tr>
    <td class="center nowrap">${index + 1}</td>
    <td class="text-cell">${escapeHtml(item.displayName || item.materialName || "")}</td>
    <td class="text-cell model-cell">${escapeHtml(item.displayModel || item.model || "")}</td>
    <td class="center text-cell">${escapeHtml(item.brand || "")}</td>
    <td class="center nowrap">${escapeHtml(item.unit || "")}</td>
    <td class="right nowrap">${escapeHtml(item.quantity ?? "")}</td>
    <td class="right nowrap">${item.unitPrice != null ? escapeHtml(Number(item.unitPrice).toFixed(4)) : ""}</td>
    <td class="right nowrap">${item.amount != null ? escapeHtml(Number(item.amount).toFixed(2)) : ""}</td>
    <td class="text-cell">${escapeHtml(item.remark || "")}</td>
  </tr>`;
}

function buildWarehouseDocumentRows(doc) {
  const rows = [...(doc.items || [])];
  while (rows.length < 10) {
    rows.push({});
  }
  return rows;
}

function formatWarehouseDocDate(doc) {
  return doc.docDate ? dayjs(doc.docDate).format("YYYY年M月D日") : "";
}

function formatCounterpartyHtml(doc) {
  return [doc.counterpartyName, doc.counterpartyAddress, doc.counterpartyContact]
    .map((item) => escapeHtml(item || ""))
    .filter(Boolean)
    .join("<br/>");
}

function escapeHtml(value) {
  if (value == null) {
    return "";
  }
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
