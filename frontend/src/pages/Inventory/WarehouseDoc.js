import React, { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from "antd";
import { DeleteOutlined, DownloadOutlined, EditOutlined, FilePdfOutlined, PlusOutlined } from "@ant-design/icons";
import { getCurrentUser } from "../../api/auth";
import { inventoryAPI } from "../../api/modules";
import { downloadApiFile, downloadHtmlExcel, resolveBlobErrorMessage } from "../../utils/exporters";
import { hasAnyAuthority } from "../../utils/authorities";
import { resolvePagePermissions } from "../../utils/pagePermissions";
import WarehouseDocumentModal from "./WarehouseDocumentModal";
import {
  categoryLabel,
  docMeta,
  formatAmount,
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
  const [selectedDocNumber, setSelectedDocNumber] = useState(null);
  const [editingDoc, setEditingDoc] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const pagePermissions = useMemo(() => resolvePagePermissions("/inventory/warehouse"), []);
  const canManage = hasAnyAuthority(currentUser, pagePermissions.manageAuthorities);
  const projectMap = useMemo(
    () =>
      Object.fromEntries(
        projects.map((item) => [item.uuid, `${item.projectNumber || ""} ${item.projectName || ""}`.trim()]),
      ),
    [projects],
  );
  const warehouseMap = useMemo(
    () => Object.fromEntries(warehouses.map((item) => [String(item.id), item.warehouseName])),
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
        itemCount: item.items?.length || 0,
      })),
    [documents, projectMap, warehouseMap],
  );
  const selectedDoc = useMemo(
    () => decoratedDocs.find((item) => item.docNumber === selectedDocNumber) || null,
    [decoratedDocs, selectedDocNumber],
  );
  const summary = useMemo(
    () =>
      decoratedDocs.reduce(
        (acc, item) => {
          acc.total += 1;
          if (item.docType === "INBOUND_NOTE") {
            acc.inbound += 1;
          }
          if (item.docType === "OUTBOUND_NOTE") {
            acc.outbound += 1;
          }
          return acc;
        },
        { total: 0, inbound: 0, outbound: 0 },
      ),
    [decoratedDocs],
  );

  const fetchData = async () => {
    setLoading(true);
    try {
      const user = await getCurrentUser().catch(() => null);
      const [docResponse, projectResponse, warehouseResponse, materialResponse, customerResponse] =
        await Promise.all([
          inventoryAPI.getWarehouseDocs(),
          inventoryAPI.getWarehouseDocProjectOptions(),
          inventoryAPI.getWarehouses(),
          inventoryAPI.getWarehouseDocMaterialOptions(),
          inventoryAPI.getWarehouseDocCustomerOptions(),
        ]);
      const docData = normalize(docResponse);
      setCurrentUser(user);
      setDocuments(docData);
      setProjects(normalize(projectResponse));
      setWarehouses(normalize(warehouseResponse));
      setMaterials(normalize(materialResponse));
      setCustomerOptions(normalize(customerResponse));
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
        const result = await inventoryAPI.updateWarehouseDoc(initialDocument.docNumber, payload);
        setSelectedDocNumber(result?.docNumber || initialDocument.docNumber);
        message.success("出入库单已更新");
      } else {
        const result = await inventoryAPI.createWarehouseDoc(payload);
        setSelectedDocNumber(result?.docNumber || null);
        message.success("出入库单已创建");
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
    { title: "单据编号", dataIndex: "docNumber", key: "docNumber", width: 170 },
    {
      title: "单据类型",
      dataIndex: "docTypeLabel",
      key: "docTypeLabel",
      width: 120,
      render: (value, record) => <Tag color={docMeta(record.docType).tagColor}>{value}</Tag>,
    },
    { title: "业务类别", dataIndex: "businessCategoryLabel", key: "businessCategoryLabel", width: 180 },
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
    { title: "总金额", dataIndex: "totalAmount", key: "totalAmount", width: 120, render: formatAmount },
    { title: "明细数", dataIndex: "itemCount", key: "itemCount", width: 100 },
    {
      title: "操作",
      key: "action",
      width: 220,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" icon={<FilePdfOutlined />} onClick={() => printDocument(record)}>
            打印
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            disabled={!canManage}
            onClick={() => openModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除这张出入库单吗？"
            onConfirm={() => deleteDocument(record.docNumber)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />} disabled={!canManage}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const previewMeta = docMeta(selectedDoc?.docType);
  const previewRows = selectedDoc ? buildWarehouseDocumentRows(selectedDoc) : [];

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="单据总数" value={summary.total} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="入库单" value={summary.inbound} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="发货单" value={summary.outbound} />
          </Card>
        </Col>
      </Row>

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
          message="出入库单采用“单据头 + 多行物料明细”模式。物料按物料代码引用，保存后会自动生成库存流水。"
        />
        <Table
          rowKey="docNumber"
          loading={loading}
          dataSource={decoratedDocs}
          columns={columns}
          pagination={{ pageSize: 8 }}
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
                      <td style={rightCell}>{item.unitPrice != null ? formatAmount(item.unitPrice) : ""}</td>
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
        currentUserId={currentUser?.id || null}
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
  const previewMeta = docMeta(doc.docType);
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
    <td class="right nowrap">${item.unitPrice != null ? escapeHtml(Number(item.unitPrice).toFixed(2)) : ""}</td>
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
