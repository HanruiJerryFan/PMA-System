import React, { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Row,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  message,
} from "antd";
import {
  ArrowLeftOutlined,
  DownloadOutlined,
  FilePdfOutlined,
  LinkOutlined,
  UnorderedListOutlined,
} from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getCurrentUser } from "../../api/auth";
import { attachmentAPI, exportAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { downloadApiFile, downloadExcel, resolveBlobErrorMessage } from "../../utils/exporters";

const LIST_TYPE_OPTIONS = [
  { value: "INITIAL_SALES", label: "初始销售清单", color: "blue" },
  { value: "PROCUREMENT", label: "采购清单", color: "green" },
  { value: "CHANGE", label: "销售变更清单", color: "orange" },
  { value: "FINAL_ALL", label: "最终清单总览", color: "geekblue" },
  { value: "FINAL_SALES", label: "最终销售清单", color: "purple" },
  { value: "FINAL_PROCUREMENT", label: "最终采购清单", color: "cyan" },
];

const FINAL_AGGREGATE_TYPES = ["FINAL_SALES", "FINAL_PROCUREMENT"];

function normalizeResponseData(response) {
  return response?.data ?? response ?? [];
}

function getListTypeMeta(value) {
  return LIST_TYPE_OPTIONS.find((item) => item.value === value) || {
    value,
    label: value || "-",
    color: "default",
  };
}

function isProcurementListType(value) {
  return value === "PROCUREMENT" || value === "FINAL_PROCUREMENT";
}

function getUnitPriceLabel(listType, aggregate = false) {
  const label = isProcurementListType(listType) ? "采购价格" : "销售价格";
  return aggregate ? `汇总${label}` : label;
}

function formatAmount(value) {
  if (value == null || value === "") {
    return "-";
  }
  return Number(value).toFixed(2);
}

function calculateItemAmount(item) {
  if (item?.totalAmount !== undefined && item?.totalAmount !== null && item?.totalAmount !== "") {
    return Number(Number(item.totalAmount).toFixed(2));
  }
  const quantity = Number(item?.quantity || 0);
  const unitPrice = Number(item?.unitPrice || 0);
  return Number((quantity * unitPrice).toFixed(2));
}

function summarizeItems(items = []) {
  return items.reduce(
    (accumulator, item) => {
      accumulator.quantity += Number(item.quantity || 0);
      accumulator.amount += Number(item.totalAmount || 0);
      return accumulator;
    },
    { quantity: 0, amount: 0 }
  );
}

function decorateAggregateView(view, aggregateType) {
  const meta = getListTypeMeta(aggregateType);
  return {
    ...view,
    aggregateType,
    aggregateTypeLabel: meta.label,
    sourceLists: (view?.sourceLists || []).map((item) => ({
      ...item,
      aggregateType,
      aggregateTypeLabel: meta.label,
    })),
    items: (view?.items || []).map((item) => ({
      ...item,
      aggregateType,
      aggregateTypeLabel: meta.label,
    })),
  };
}

function buildCombinedAggregateView(projectId, salesView, procurementView) {
  const sales = decorateAggregateView(salesView || {}, "FINAL_SALES");
  const procurement = decorateAggregateView(procurementView || {}, "FINAL_PROCUREMENT");
  const allItems = [...sales.items, ...procurement.items];
  const allSourceLists = [...sales.sourceLists, ...procurement.sourceLists];
  const totalAmount = allItems.reduce((sum, item) => sum + calculateItemAmount(item), 0);
  const totalQuantity = allItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0);

  return {
    projectId,
    aggregateType: "FINAL_ALL",
    listName: "最终清单总览",
    customerName: sales.customerName || procurement.customerName || null,
    sourceListCount: allSourceLists.length,
    itemCount: allItems.length,
    totalQuantity,
    totalAmount,
    sourceLists: allSourceLists,
    items: allItems,
    sales,
    procurement,
  };
}

export default function ProjectListDashboard() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const projectUuid = searchParams.get("projectUuid") || searchParams.get("projectId") || "";
  const listUuid = searchParams.get("listUuid") || "";
  const aggregateType = searchParams.get("aggregateType") || "";
  const from = searchParams.get("from") || "dashboard";

  const [loading, setLoading] = useState(true);
  const [currentUser, setCurrentUser] = useState(null);
  const [project, setProject] = useState(null);
  const [projectList, setProjectList] = useState(null);
  const [aggregateView, setAggregateView] = useState(null);
  const [listItems, setListItems] = useState([]);
  const [attachments, setAttachments] = useState([]);

  const canAccessAttachments = hasAnyAuthority(currentUser, ["attachment.access"]);
  const canManageProject = hasAnyAuthority(currentUser, ["project.manage"]);

  const isAggregateView = Boolean(aggregateType);
  const isCombinedAggregateView = aggregateType === "FINAL_ALL";
  const effectiveListType = aggregateType || projectList?.listType;
  const listTypeMeta = useMemo(() => getListTypeMeta(effectiveListType), [effectiveListType]);
  const unitPriceLabel = useMemo(
    () => isCombinedAggregateView ? "汇总价格" : getUnitPriceLabel(effectiveListType, isAggregateView),
    [effectiveListType, isAggregateView, isCombinedAggregateView]
  );
  const itemSummary = useMemo(() => summarizeItems(listItems), [listItems]);
  const aggregateSections = useMemo(
    () =>
      isCombinedAggregateView
        ? FINAL_AGGREGATE_TYPES.map((type) => {
            const key = type === "FINAL_SALES" ? "sales" : "procurement";
            const section = aggregateView?.[key] || decorateAggregateView({}, type);
            return {
              key: type,
              title: getListTypeMeta(type).label,
              color: getListTypeMeta(type).color,
              data: section,
              items: section.items || [],
              sourceLists: section.sourceLists || [],
              summary: summarizeItems(section.items || []),
            };
          })
        : [],
    [aggregateView, isCombinedAggregateView]
  );

  const currentPdfAttachment = useMemo(() => {
    if (isAggregateView) {
      return null;
    }
    if (!projectList?.pdfAttachmentId) {
      return attachments[0] || null;
    }
    return attachments.find((item) => item.uuid === projectList.pdfAttachmentId) || attachments[0] || null;
  }, [attachments, isAggregateView, projectList?.pdfAttachmentId]);

  const backTarget = useMemo(() => {
    if (from === "lists-project") {
      return {
        label: "返回项目清单管理",
        route: `/project/lists?projectUuid=${projectUuid}`,
      };
    }
    if (from === "lists-all") {
      return {
        label: "返回项目清单管理",
        route: "/project/lists",
      };
    }
    return {
      label: "返回项目总览",
      route: `/project/dashboard?projectUuid=${projectUuid}`,
    };
  }, [from, projectUuid]);

  useEffect(() => {
    async function loadData() {
      if (!projectUuid || (!listUuid && !aggregateType)) {
        setLoading(false);
        return;
      }

      setLoading(true);
      try {
        const user = await getCurrentUser().catch(() => null);
        const allowAttachment = hasAnyAuthority(user, ["attachment.access"]);
        const aggregateRequest = aggregateType
          ? isCombinedAggregateView
            ? Promise.all(FINAL_AGGREGATE_TYPES.map((type) => projectAPI.getProjectListAggregate(projectUuid, type)))
            : projectAPI.getProjectListAggregate(projectUuid, aggregateType)
          : Promise.resolve(null);
        const [projectResponse, listResponse, aggregateResponse, itemResponse, attachmentResponse] = await Promise.all([
          projectAPI.getProjects(),
          projectAPI.getProjectListsByProject(projectUuid),
          aggregateRequest,
          listUuid ? projectAPI.getProjectListItems(listUuid) : Promise.resolve([]),
          allowAttachment && !aggregateType
            ? attachmentAPI.getAttachmentsByBusiness("project-lists", listUuid)
            : Promise.resolve([]),
        ]);

        const projectListRows = normalizeResponseData(listResponse);
        const projectRows = normalizeResponseData(projectResponse);
        const aggregateData = isCombinedAggregateView
          ? buildCombinedAggregateView(
              projectUuid,
              normalizeResponseData(aggregateResponse?.[0]),
              normalizeResponseData(aggregateResponse?.[1])
            )
          : normalizeResponseData(aggregateResponse);

        setCurrentUser(user);
        setProject(projectRows.find((item) => item.uuid === projectUuid) || null);
        setProjectList(aggregateType ? null : projectListRows.find((item) => item.uuid === listUuid) || null);
        setAggregateView(aggregateType ? aggregateData : null);
        setListItems(aggregateType ? aggregateData?.items || [] : normalizeResponseData(itemResponse));
        setAttachments(normalizeResponseData(attachmentResponse));
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [aggregateType, isCombinedAggregateView, listUuid, projectUuid]);

  const buildItemColumns = (listType, options = {}) => {
    const aggregate = options.aggregate ?? isAggregateView;
    const includeAggregateType = options.includeAggregateType ?? false;
    return [
      ...(includeAggregateType ? [{ title: "汇总类型", dataIndex: "aggregateTypeLabel", key: "aggregateTypeLabel", width: 140 }] : []),
      { title: "物料编码", dataIndex: "materialCode", key: "materialCode", width: 140 },
      { title: "物料名称", dataIndex: "itemName", key: "itemName", width: 220 },
      { title: "型号", dataIndex: "model", key: "model", width: 180 },
      { title: "品牌", dataIndex: "brand", key: "brand", width: 140 },
      { title: "单位", dataIndex: "unit", key: "unit", width: 100 },
      { title: "数量", dataIndex: "quantity", key: "quantity", width: 120 },
      { title: includeAggregateType ? unitPriceLabel : getUnitPriceLabel(listType, aggregate), dataIndex: "unitPrice", key: "unitPrice", width: 120, render: formatAmount },
      { title: "金额", dataIndex: "totalAmount", key: "totalAmount", width: 120, render: formatAmount },
      ...(aggregate ? [{ title: "来源清单数", dataIndex: "sourceListCount", key: "sourceListCount", width: 120 }] : []),
      { title: "备注", dataIndex: "remark", key: "remark", width: 220 },
    ];
  };

  const itemColumns = buildItemColumns(effectiveListType, {
    aggregate: isAggregateView,
    includeAggregateType: isCombinedAggregateView,
  });

  const sourceColumns = [
    ...(isCombinedAggregateView ? [{ title: "汇总类型", dataIndex: "aggregateTypeLabel", key: "aggregateTypeLabel", width: 140 }] : []),
    { title: "清单名称", dataIndex: "listName", key: "listName", width: 220 },
    { title: "清单类型", dataIndex: "listType", key: "listType", width: 140, render: (value) => getListTypeMeta(value).label },
    { title: "录入日期", dataIndex: "entryDate", key: "entryDate", width: 140, render: (value) => value ? dayjs(value).format("YYYY-MM-DD") : "-" },
  ];

  const handleExportExcel = () => {
    const fileName = `${project?.projectName || "项目"}-${aggregateView?.listName || projectList?.listName || "清单"}.xls`;
    if (isCombinedAggregateView) {
      const rows = listItems.map((item) => [
        item.aggregateTypeLabel || getListTypeMeta(item.aggregateType).label,
        item.materialCode || "",
        item.itemName || "",
        item.model || "",
        item.brand || "",
        item.unit || "",
        Number(item.quantity || 0),
        item.unitPrice == null ? "" : Number(item.unitPrice),
        calculateItemAmount(item),
        item.sourceListCount || 0,
      ]);
      const totalAmount = rows.reduce((sum, row) => sum + Number(row[8] || 0), 0);
      downloadExcel(
        fileName,
        "最终清单总览",
        ["汇总类型", "物料编码", "物料名称", "型号", "品牌", "单位", "数量", "汇总价格", "金额", "来源清单数"],
        [...rows, ["", "", "", "", "", "合计", "", "", Number(totalAmount.toFixed(2)), ""]]
      );
      return;
    }

    const rows = listItems.map((item) =>
      isAggregateView
        ? [
            item.materialCode || "",
            item.itemName || "",
            item.model || "",
            item.brand || "",
            item.unit || "",
            Number(item.quantity || 0),
            item.unitPrice == null ? "" : Number(item.unitPrice),
            calculateItemAmount(item),
            item.sourceListCount || 0,
          ]
        : [
            item.materialCode || "",
            item.itemName || "",
            item.model || "",
            item.brand || "",
            item.unit || "",
            Number(item.quantity || 0),
            item.unitPrice == null ? "" : Number(item.unitPrice),
            calculateItemAmount(item),
            item.remark || "",
          ]
    );
    const totalAmount = rows.reduce((sum, row) => sum + Number(row[7] || 0), 0);
    downloadExcel(
      fileName,
      isAggregateView ? "最终清单明细" : "项目清单明细",
      isAggregateView
        ? ["物料编码", "物料名称", "型号", "品牌", "单位", "数量", unitPriceLabel, "金额", "来源清单数"]
        : ["物料编码", "物料名称", "型号", "品牌", "单位", "数量", unitPriceLabel, "金额", "备注"],
      [...rows, ["", "", "", "", "合计", "", "", Number(totalAmount.toFixed(2)), ""]]
    );
  };

  const handleExportPdf = async () => {
    const totalAmount = listItems.reduce((sum, item) => sum + calculateItemAmount(item), 0);
    const combinedPdfColumns = [
      { header: "汇总类型", width: 9 },
      { header: "物料编码", width: 13 },
      { header: "物料名称", width: 17 },
      { header: "型号", width: 14 },
      { header: "品牌", width: 9 },
      { header: "单位", align: "center", width: 6 },
      { header: "数量", align: "right", width: 8 },
      { header: "汇总价格", align: "right", width: 9 },
      { header: "金额", align: "right", width: 9 },
      { header: "来源数", align: "right", width: 6 },
    ];
    const combinedPdfRows = listItems.map((item) => [
      item.aggregateTypeLabel || getListTypeMeta(item.aggregateType).label,
      item.materialCode || "",
      item.itemName || "",
      item.model || "",
      item.brand || "",
      item.unit || "",
      item.quantity || "",
      formatAmount(item.unitPrice),
      formatAmount(calculateItemAmount(item)),
      item.sourceListCount || 0,
    ]);
    const payload = {
      title: isCombinedAggregateView ? "最终清单总览" : isAggregateView ? "最终清单明细" : "项目清单明细",
      subtitle: `生成时间：${dayjs().format("YYYY-MM-DD HH:mm")}`,
      fileName: `${project?.projectName || "项目"}-${aggregateView?.listName || projectList?.listName || "清单"}.pdf`,
      metadata: [
        { label: "项目", value: project?.projectName || "-" },
        { label: "清单名称", value: aggregateView?.listName || projectList?.listName || "-" },
        { label: "客户", value: aggregateView?.customerName || projectList?.customerName || "-" },
        { label: "清单类型", value: listTypeMeta.label },
      ],
      summaries: [{ label: "合计金额", value: formatAmount(totalAmount) }],
      columns: isCombinedAggregateView
        ? combinedPdfColumns
        : isAggregateView
        ? [
            { header: "物料编码", width: 13 },
            { header: "物料名称", width: 18 },
            { header: "型号", width: 15 },
            { header: "品牌", width: 10 },
            { header: "单位", align: "center", width: 7 },
            { header: "数量", align: "right", width: 8 },
            { header: unitPriceLabel, align: "right", width: 10 },
            { header: "金额", align: "right", width: 10 },
            { header: "来源清单数", align: "right", width: 10 },
          ]
        : [
            { header: "物料编码", width: 13 },
            { header: "物料名称", width: 18 },
            { header: "型号", width: 15 },
            { header: "品牌", width: 10 },
            { header: "单位", align: "center", width: 7 },
            { header: "数量", align: "right", width: 8 },
            { header: unitPriceLabel, align: "right", width: 10 },
            { header: "金额", align: "right", width: 10 },
            { header: "备注", width: 14 },
          ],
      rows: isCombinedAggregateView
        ? combinedPdfRows
        : listItems.map((item) =>
            isAggregateView
              ? [
                  item.materialCode || "",
                  item.itemName || "",
                  item.model || "",
                  item.brand || "",
                  item.unit || "",
                  item.quantity || "",
                  formatAmount(item.unitPrice),
                  formatAmount(calculateItemAmount(item)),
                  item.sourceListCount || 0,
                ]
              : [
                  item.materialCode || "",
                  item.itemName || "",
                  item.model || "",
                  item.brand || "",
                  item.unit || "",
                  item.quantity || "",
                  formatAmount(item.unitPrice),
                  formatAmount(calculateItemAmount(item)),
                  item.remark || "",
                ]
          ),
      extraSections: isAggregateView
        ? [
            {
              title: "来源清单",
              columns: [
                ...(isCombinedAggregateView ? [{ header: "汇总类型", width: 20 }] : []),
                { header: "清单名称", width: 40 },
                { header: "清单类型", width: 20 },
                { header: "录入日期", align: "center", width: 20 },
              ],
              rows: (aggregateView?.sourceLists || []).map((item) =>
                isCombinedAggregateView
                  ? [
                      item.aggregateTypeLabel || getListTypeMeta(item.aggregateType).label,
                      item.listName || "",
                      getListTypeMeta(item.listType).label,
                      item.entryDate ? dayjs(item.entryDate).format("YYYY-MM-DD") : "",
                    ]
                  : [
                      item.listName || "",
                      getListTypeMeta(item.listType).label,
                      item.entryDate ? dayjs(item.entryDate).format("YYYY-MM-DD") : "",
                    ]
              ),
            },
          ]
        : [],
    };
    try {
      await downloadApiFile(exportAPI.downloadTablePdf(payload), payload.fileName, "application/pdf");
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "导出 PDF 失败"));
    }
  };

  if (loading) {
    return (
      <div style={{ padding: 40, textAlign: "center" }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!projectUuid || (!isAggregateView && (!listUuid || !projectList)) || (isAggregateView && !aggregateView)) {
    return (
      <Card>
        <Empty description={isAggregateView ? "未找到对应的最终清单" : "未找到对应的项目清单详情"}>
          <Button type="primary" icon={<ArrowLeftOutlined />} onClick={() => navigate(backTarget.route)}>
            {backTarget.label}
          </Button>
        </Empty>
      </Card>
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 16, flexWrap: "wrap" }}>
          <Space direction="vertical" size={4}>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(backTarget.route)}>
              {backTarget.label}
            </Button>
            <div style={{ fontSize: 24, fontWeight: 700 }}>{aggregateView?.listName || projectList?.listName || "未命名清单"}</div>
            <Space wrap size={8}>
              <Tag color={listTypeMeta.color}>{listTypeMeta.label}</Tag>
              <Tag>{project?.projectName || projectUuid}</Tag>
            </Space>
          </Space>

          <Space wrap>
            <Button icon={<UnorderedListOutlined />} onClick={() => navigate(`/project/lists?projectUuid=${projectUuid}`)}>
              查看当前项目全部清单
            </Button>
            <Button icon={<DownloadOutlined />} onClick={handleExportExcel} disabled={!listItems.length}>
              导出 Excel
            </Button>
            <Button icon={<FilePdfOutlined />} onClick={handleExportPdf} disabled={!listItems.length}>
              导出 PDF
            </Button>
            {!isAggregateView && canAccessAttachments ? (
              <Button icon={<LinkOutlined />} onClick={() => navigate(`/attachment/center?businessType=project-lists&businessUuid=${listUuid}`)}>
                查看清单附件
              </Button>
            ) : null}
          </Space>
        </div>
      </Card>

      <Row gutter={[16, 16]}>
        {isCombinedAggregateView ? (
          <>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title="最终销售明细" value={aggregateSections[0]?.items.length || 0} />
              </Card>
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title="最终采购明细" value={aggregateSections[1]?.items.length || 0} />
              </Card>
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title="合计金额" value={itemSummary.amount} precision={2} />
              </Card>
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title="来源清单数" value={aggregateView?.sourceListCount || 0} />
              </Card>
            </Col>
          </>
        ) : (
          <>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title={isAggregateView ? "汇总明细条数" : "清单明细条数"} value={listItems.length} />
              </Card>
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title="总数量" value={itemSummary.quantity} precision={4} />
              </Card>
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title="总金额" value={itemSummary.amount} precision={2} />
              </Card>
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <Card>
                <Statistic title={isAggregateView ? "来源清单数" : "附件数量"} value={isAggregateView ? aggregateView?.sourceListCount || 0 : attachments.length} />
              </Card>
            </Col>
          </>
        )}
      </Row>

      <Card
        title={isAggregateView ? "最终清单信息" : "清单信息"}
        extra={
          canManageProject ? (
            <Button onClick={() => navigate(`/project/lists?projectUuid=${projectUuid}`)}>
              进入项目清单台账维护
            </Button>
          ) : null
        }
      >
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="所属项目">{project?.projectName || "-"}</Descriptions.Item>
          <Descriptions.Item label="客户">{aggregateView?.customerName || projectList?.customerName || "-"}</Descriptions.Item>
          <Descriptions.Item label="清单名称">{aggregateView?.listName || projectList?.listName || "-"}</Descriptions.Item>
          <Descriptions.Item label="清单类型">{listTypeMeta.label}</Descriptions.Item>
          <Descriptions.Item label="录入日期">
            {isAggregateView ? "-" : projectList?.entryDate ? dayjs(projectList.entryDate).format("YYYY-MM-DD") : "-"}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {isAggregateView ? "-" : projectList?.createTime ? dayjs(projectList.createTime).format("YYYY-MM-DD HH:mm") : "-"}
          </Descriptions.Item>
          <Descriptions.Item label={isAggregateView ? "来源清单" : "PDF附件"} span={2}>
            {isAggregateView ? (
              <Tag color="processing">{aggregateView?.sourceListCount || 0} 张来源清单自动汇总</Tag>
            ) : (
              canAccessAttachments ? (
                currentPdfAttachment ? (
                  <Space wrap>
                    <Tag color="green">{currentPdfAttachment.originalFileName || currentPdfAttachment.fileName || "已上传"}</Tag>
                    <Button type="link" size="small" onClick={() => navigate(`/attachment/center?businessType=project-lists&businessUuid=${listUuid}`)}>
                      查看附件中心
                    </Button>
                  </Space>
                ) : (
                  <Tag>未上传</Tag>
                )
              ) : (
                "无附件查看权限"
              )
            )}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {isAggregateView ? (
        <Card title="来源清单">
          <Table
            rowKey="uuid"
            size="middle"
            pagination={false}
            columns={sourceColumns}
            dataSource={aggregateView?.sourceLists || []}
            locale={{ emptyText: "暂无来源清单" }}
            scroll={{ x: "max-content" }}
          />
        </Card>
      ) : null}

      {isCombinedAggregateView ? (
        aggregateSections.map((section) => (
          <Card
            key={section.key}
            title={section.title}
            extra={
              <Space wrap>
                <Tag color={section.color}>{section.items.length} 条明细</Tag>
                <Tag>金额 {formatAmount(section.summary.amount)}</Tag>
              </Space>
            }
          >
            <Table
              rowKey={(record) => `${section.key}-${record.uuid || record.materialId || record.materialCode || `${record.itemName}-${record.model}-${record.brand}`}`}
              size="middle"
              pagination={false}
              columns={buildItemColumns(section.key, { aggregate: true })}
              dataSource={section.items}
              locale={{ emptyText: `暂无${section.title}` }}
              scroll={{ x: "max-content" }}
            />
          </Card>
        ))
      ) : (
        <Card title={isAggregateView ? "汇总清单明细" : "清单明细"}>
          <Table
            rowKey={(record) => record.uuid || record.materialId || record.materialCode || `${record.itemName}-${record.model}-${record.brand}`}
            size="middle"
            pagination={false}
            columns={itemColumns}
            dataSource={listItems}
            locale={{ emptyText: "暂无清单明细" }}
            scroll={{ x: "max-content" }}
          />
        </Card>
      )}
    </Space>
  );
}
