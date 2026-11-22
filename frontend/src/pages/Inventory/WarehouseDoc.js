import React, { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  Alert,
  Button,
  Card,
  Checkbox,
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
import { useAuth } from "../../context/AuthContext";
import { inventoryAPI, permissionAPI } from "../../api/modules";
import { downloadApiFile, downloadExcel, resolveBlobErrorMessage } from "../../utils/exporters";
import { formatProjectOptionLabel } from "../../utils/projectLabels";
import { sortSelectItems } from "../../utils/selectSorting";
import { queryWarehouseDocuments } from "./warehouseDocumentQuery";
import { hasAnyAuthority } from "../../utils/authorities";
import { resolvePagePermissions } from "../../utils/pagePermissions";
import { canMaintainEntryAuditUsers, getUserLabel, isSelfEntryRecord } from "../../utils/entryAudit";
import WarehouseDocumentModal from "./WarehouseDocumentModal";
import {
  DOC_TYPES,
  isGeneratedInbound,
  isTransferDocument,
  categoryLabel,
  docMeta,
  formatAmount,
  normalize,
  toChineseUppercaseRmb,
} from "./warehouseDocumentUtils";
import {
  DEFAULT_COMPANY_NAME,
  PRINT_COL_WIDTHS_PX,
  PRINT_ROW_HEIGHTS_PT,
  buildWarehouseDocumentHtml,
  formatCounterpartyLines,
  formatPrintAmount,
  formatPrintDate,
  formatPrintQuantity,
  formatPrintUnitPrice,
  paginateWarehouseDocument,
  resolvePrintDocMeta,
  resolvePrintDocTotalAmount,
  resolvePrintItemAmount,
} from "./warehouseDocumentPrint";

export default function WarehouseDoc() {
  const { currentUser } = useAuth();
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
  const [companyName, setCompanyName] = useState(DEFAULT_COMPANY_NAME);
  const [hidePriceAndAmount, setHidePriceAndAmount] = useState(false);
  const includePrice = !hidePriceAndAmount;

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
  const projectNameMap = useMemo(
    () =>
      Object.fromEntries(
        projects.map((item) => [item.uuid, String(item.projectName || "").trim()]),
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
        projectName: item.projectId ? projectNameMap[item.projectId] ?? "" : "",
        warehouseLabel:
          item.warehouseId != null ? warehouseMap[String(item.warehouseId)] || item.warehouseId : "-",
        entryUserName: item.entryUserName || userMap[item.entryUser] || "",
        auditorUserName: item.auditorUserName || userMap[item.auditorUser] || "",
        itemCount: item.items?.length || 0,
        relatedDocNumber: isGeneratedInbound(item) ? item.sourceRefId : documents.find((doc) => isGeneratedInbound(doc) && doc.sourceRefId === item.docNumber)?.docNumber,
      })),
    [documents, projectMap, projectNameMap, userMap, warehouseMap],
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
    setQueryFilters((prev) => ({
      ...(prev?.docType ? { docType: prev.docType } : {}),
      ...values,
    }));
    setDocumentPage(1);
  };

  const resetQuery = () => {
    queryForm.resetFields();
    setQueryFilters({});
    setDocumentPage(1);
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
      const [
        docResponse,
        projectResponse,
        warehouseResponse,
        materialResponse,
        customerResponse,
        userResponse,
        printConfigResponse,
      ] = await Promise.all([
        inventoryAPI.getWarehouseDocs(),
        inventoryAPI.getWarehouseDocProjectOptions(),
        inventoryAPI.getWarehouses(),
        inventoryAPI.getWarehouseDocMaterialOptions(),
        inventoryAPI.getWarehouseDocCustomerOptions(),
        permissionAPI.getUserOptions().catch(() => []),
        inventoryAPI.getWarehouseDocPrintConfig().catch(() => null),
      ]);
      const docData = normalize(docResponse);
      setDocuments(docData);
      setProjects(normalize(projectResponse));
      setWarehouses(normalize(warehouseResponse));
      setMaterials(normalize(materialResponse));
      setCustomerOptions(normalize(customerResponse));
      setUsers(Array.isArray(userResponse) ? userResponse : normalize(userResponse));
      const printConfig = normalize(printConfigResponse);
      if (printConfig?.companyName) {
        setCompanyName(printConfig.companyName);
      }
      setSelectedDocNumber((current) =>
        current && docData.some((item) => item.docNumber === current)
          ? current
          : docData[0]?.docNumber || null,
      );
    } catch (error) {
      message.error(error?.message || "加载出入库单数据失败");
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
    if (record?.auditorUser && !canMaintainEntryAudit) {
      message.error("已审核出入库单不可修改");
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
    try {
      await inventoryAPI.deleteWarehouseDoc(docNumber);
      message.success("删除成功");
      await fetchData();
    } catch (error) {
      message.error(error?.message || "删除出入库单失败");
    }
  };

  const auditDocument = async (docNumber) => {
    if (!canAudit) {
      message.error("没有审核出入库单的权限");
      return;
    }
    try {
      await inventoryAPI.auditWarehouseDoc(docNumber);
      message.success("出入库单已审核");
      await fetchData();
    } catch (error) {
      message.error(error?.message || "审核出入库单失败");
    }
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
    popup.document.write(buildWarehouseDocumentHtml(doc, { autoPrint: true, companyName, includePrice }));
    popup.document.close();
  };

  const exportDocumentPdf = async (doc = selectedDoc) => {
    if (!doc) {
      return;
    }
    try {
      await downloadApiFile(
        inventoryAPI.downloadWarehouseDocPdf(doc.docNumber, { includePrice }),
        `${doc.docNumber || docMeta(doc.docType).label}.pdf`,
        "application/pdf"
      );
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "导出 PDF 失败"));
    }
  };

  const exportDocumentExcel = async (doc = selectedDoc) => {
    if (!doc) {
      return;
    }
    try {
      await downloadApiFile(
        inventoryAPI.downloadWarehouseDocExcel(doc.docNumber, { includePrice }),
        `${doc.docNumber || docMeta(doc.docType).label}.xlsx`,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      );
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "导出 Excel 失败"));
    }
  };

  const renderHidePriceCheckbox = () => (
    <Checkbox
      checked={hidePriceAndAmount}
      onChange={(event) => setHidePriceAndAmount(event.target.checked)}
    >
      不打印单价和金额
    </Checkbox>
  );

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
            disabled={
              !canManage ||
              isGeneratedInbound(record) ||
              (Boolean(record.auditorUser) && !canMaintainEntryAudit)
            }
            onClick={() => openModal(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<CheckCircleOutlined />}
            disabled={
              !canAudit ||
              Boolean(record.auditorUser) ||
              isGeneratedInbound(record) ||
              (isSelfEntryRecord(currentUser, record) && !canMaintainEntryAudit)
            }
            onClick={() => auditDocument(record.docNumber)}
          >
            审核
          </Button>
          <Popconfirm
            title="确定删除这张出入库单吗？"
            disabled={
              !canManage ||
              isGeneratedInbound(record) ||
              (Boolean(record.auditorUser) && !canMaintainEntryAudit)
            }
            onConfirm={() => deleteDocument(record.docNumber)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              disabled={
                !canManage ||
                isGeneratedInbound(record) ||
                (Boolean(record.auditorUser) && !canMaintainEntryAudit)
              }
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const previewPrintMeta = selectedDoc ? resolvePrintDocMeta(selectedDoc, companyName) : null;
  const previewPages = selectedDoc ? paginateWarehouseDocument(selectedDoc) : [];
  const previewDocTotal = selectedDoc ? resolvePrintDocTotalAmount(selectedDoc) : 0;

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
            {renderHidePriceCheckbox()}
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

      <Card
        title={selectedDoc ? `${selectedDoc.docTypeLabel}预览` : "单据预览"}
        extra={renderHidePriceCheckbox()}
      >
        {selectedDoc && previewPrintMeta ? (
          <div style={{ overflowX: "auto" }}>
            {previewPages.map((page) => {
              const displayTotal = page.lastPage ? previewDocTotal : page.pageSubtotal;
              const totalLabel = page.lastPage ? "合计：" : "本页小计：";
              const totalUppercaseText = includePrice ? toChineseUppercaseRmb(displayTotal) : "";
              const totalAmountText = includePrice ? `¥${formatPrintAmount(displayTotal)}` : "";
              return (
                <div key={page.pageNumber} style={previewPaperStyle}>
                  <table style={previewTableStyle}>
                    <colgroup>
                      {PRINT_COL_WIDTHS_PX.map((width, index) => (
                        <col key={`${width}-${index}`} style={{ width }} />
                      ))}
                    </colgroup>
                    <tbody>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[0]}pt` }}>
                        <td colSpan={4} style={previewNoBorderCell} />
                        <td colSpan={4} style={previewTitleCell}>
                          {previewPrintMeta.spacedTitle}
                        </td>
                        {page.totalPages > 1 ? (
                          <>
                            <td colSpan={3} style={previewPageNumCell}>
                              {`第 ${page.pageNumber} 页 / 共 ${page.totalPages} 页`}
                            </td>
                            <td style={previewNoBorderCell} />
                          </>
                        ) : (
                          <td colSpan={4} style={previewNoBorderCell} />
                        )}
                      </tr>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[1]}pt` }}>
                        <td colSpan={12} style={previewNoBorderCell} />
                      </tr>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[2]}pt` }}>
                        <td rowSpan={3} style={previewMetaLabelBoldCell}>
                          {renderMultilineText("项目\n名称")}
                        </td>
                        <td rowSpan={3} colSpan={2} style={previewMetaProjectCell}>
                          {selectedDoc.projectName || ""}
                        </td>
                        <td rowSpan={3} style={previewMetaLabelBoldCell}>
                          {renderMultilineText(previewPrintMeta.partyBlockLabel)}
                        </td>
                        <td rowSpan={3} colSpan={4} style={previewMetaPartyCell}>
                          {renderLineArray(formatCounterpartyLines(selectedDoc))}
                        </td>
                        <td style={previewMetaSmallCenterCell}>{previewPrintMeta.numberLabel}</td>
                        <td colSpan={2} style={previewMetaSmallCenterCell}>
                          {selectedDoc.docNumber || ""}
                        </td>
                        <td rowSpan={15} style={previewCopyStubCell}>
                          {renderMultilineText(previewPrintMeta.copyStubText)}
                        </td>
                      </tr>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[3]}pt` }}>
                        <td style={previewMetaSmallCenterCell}>合同编号：</td>
                        <td colSpan={2} style={previewMetaSmallCenterCell}>
                          {selectedDoc.contractNumber || ""}
                        </td>
                      </tr>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[4]}pt` }}>
                        <td style={previewMetaSmallCenterCell}>{previewPrintMeta.dateLabel}</td>
                        <td colSpan={2} style={previewMetaSmallCenterCell}>
                          {formatPrintDate(selectedDoc.docDate)}
                        </td>
                      </tr>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[5]}pt` }}>
                        <th style={previewHeadCell}>序号</th>
                        <th colSpan={2} style={previewHeadCell}>材料名称</th>
                        <th colSpan={2} style={previewHeadCell}>型号及规格</th>
                        <th style={previewHeadCell}>品牌</th>
                        <th style={previewHeadCell}>单位</th>
                        <th style={previewHeadCell}>数量</th>
                        <th style={previewHeadCell}>单价</th>
                        <th style={previewHeadCell}>金额</th>
                        <th style={previewHeadCell}>备注</th>
                      </tr>
                      {page.rows.map((item, idx) => {
                        const lineNo = page.startLineNo + idx;
                        const amount = includePrice ? resolvePrintItemAmount(item) : null;
                        return (
                          <tr key={item.uuid || `blank-${lineNo}`} style={{ height: `${PRINT_ROW_HEIGHTS_PT[6 + idx]}pt` }}>
                            <td style={previewItemIndexCell}>{lineNo}</td>
                            <td colSpan={2} style={previewItemTextCell}>
                              {item.displayName || item.materialName || ""}
                            </td>
                            <td colSpan={2} style={previewItemTextCell}>
                              {item.displayModel || item.model || ""}
                            </td>
                            <td style={previewItemBrandCell}>{item.brand || ""}</td>
                            <td style={previewItemCenter10Cell}>{item.unit || ""}</td>
                            <td style={previewItemCenter10Cell}>{formatPrintQuantity(item.quantity)}</td>
                            <td style={previewItemCenter10Cell}>{includePrice ? formatPrintUnitPrice(item.unitPrice) : ""}</td>
                            <td style={previewItemAmountCell}>{includePrice ? formatPrintAmount(amount) : ""}</td>
                            <td style={previewItemRemarkCell}>{item.remark || ""}</td>
                          </tr>
                        );
                      })}
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[16]}pt` }}>
                        <td colSpan={2} style={previewTotalLabelCell}>{totalLabel}</td>
                        <td colSpan={7} style={previewTotalUpperCell}>
                          {totalUppercaseText}
                        </td>
                        <td colSpan={2} style={previewTotalNumCell}>
                          {totalAmountText}
                        </td>
                      </tr>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[17]}pt` }}>
                        <td colSpan={12} style={previewNoBorderCell} />
                      </tr>
                      <tr style={{ height: `${PRINT_ROW_HEIGHTS_PT[18]}pt` }}>
                        <td colSpan={11} style={previewFooterCell}>
                          {previewPrintMeta.footerText}
                        </td>
                        <td style={previewNoBorderCell} />
                      </tr>
                    </tbody>
                  </table>
                </div>
              );
            })}
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

const previewPaperStyle = {
  width: 820,
  padding: 12,
  margin: "0 auto 16px",
  background: "#fff",
  border: "1px solid #d9d9d9",
  boxShadow: "0 1px 4px rgba(0, 0, 0, 0.06)",
};
const previewTableStyle = {
  width: 796,
  borderCollapse: "collapse",
  tableLayout: "fixed",
  fontFamily: '"SimSun", "NSimSun", serif',
  color: "#000",
};
const previewNoBorderCell = { border: "none", padding: 0 };
const previewThinCell = {
  border: "1px solid #000",
  padding: "0 3px",
  verticalAlign: "middle",
  lineHeight: 1.15,
  wordBreak: "break-all",
  overflowWrap: "break-word",
};
const previewTitleCell = {
  border: "none",
  borderBottom: "3px double #000",
  textAlign: "center",
  verticalAlign: "middle",
  fontSize: "18pt",
  fontWeight: 700,
  whiteSpace: "pre",
};
const previewPageNumCell = {
  border: "none",
  textAlign: "right",
  verticalAlign: "bottom",
  fontSize: "9pt",
  padding: "0 3px 2px 0",
};
const previewMetaLabelBoldCell = { ...previewThinCell, textAlign: "center", fontSize: "9pt", fontWeight: 700 };
const previewMetaProjectCell = { ...previewThinCell, textAlign: "left", fontSize: "10pt" };
const previewMetaPartyCell = { ...previewThinCell, textAlign: "left", fontSize: "8.5pt", lineHeight: 1.2 };
const previewMetaSmallCenterCell = { ...previewThinCell, textAlign: "center", fontSize: "9pt" };
const previewCopyStubCell = {
  border: "none",
  textAlign: "center",
  verticalAlign: "middle",
  fontSize: "9pt",
  lineHeight: 1.18,
  padding: 0,
};
const previewHeadCell = { ...previewThinCell, textAlign: "center", fontSize: "9pt", fontWeight: 400 };
const previewItemIndexCell = { ...previewThinCell, textAlign: "center", fontSize: "9pt" };
const previewItemTextCell = { ...previewThinCell, textAlign: "left", fontSize: "10pt" };
const previewItemBrandCell = { ...previewThinCell, textAlign: "center", fontSize: "9pt" };
const previewItemCenter10Cell = { ...previewThinCell, textAlign: "center", fontSize: "10pt" };
const previewItemAmountCell = { ...previewThinCell, textAlign: "right", fontSize: "10pt" };
const previewItemRemarkCell = { ...previewThinCell, textAlign: "left", fontSize: "8pt" };
const previewTotalLabelCell = {
  borderTop: "1px solid #000",
  borderBottom: "1px solid #000",
  borderLeft: "1px solid #000",
  borderRight: "none",
  padding: "0 3px",
  verticalAlign: "middle",
  textAlign: "right",
  fontSize: "11pt",
  fontWeight: 700,
};
const previewTotalUpperCell = {
  borderTop: "1px solid #000",
  borderBottom: "1px solid #000",
  borderRight: "1px solid #000",
  borderLeft: "none",
  padding: "0 3px",
  verticalAlign: "middle",
  textAlign: "left",
  fontSize: "11pt",
  fontWeight: 700,
};
const previewTotalNumCell = { ...previewThinCell, textAlign: "center", fontSize: "9pt" };
const previewFooterCell = {
  border: "none",
  padding: 0,
  verticalAlign: "middle",
  textAlign: "left",
  fontSize: "11pt",
  fontWeight: 700,
  whiteSpace: "pre",
};

function renderLineArray(lines) {
  return lines.map((line, index) => (
    <React.Fragment key={`${index}-${line}`}>
      {index > 0 ? <br /> : null}
      {line}
    </React.Fragment>
  ));
}

function renderMultilineText(value) {
  return renderLineArray(String(value ?? "").split(/\r?\n/));
}
