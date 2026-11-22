import React, { useCallback, useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Popconfirm,
  Progress,
  Row,
  Segmented,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from "antd";
import {
  AppstoreOutlined,
  ArrowRightOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  DownloadOutlined,
  EyeOutlined,
  ReconciliationOutlined,
  ReloadOutlined,
  SearchOutlined,
  UnorderedListOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { projectAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";
import {
  RISK_OPTIONS,
  filterTrackingCards,
  formatMoney,
  formatPercent,
  getRiskMeta,
  getWarrantyMeta,
  groupCardsIntoSwimlanes,
} from "../../utils/trackingBoardHelpers";

const { Text, Title } = Typography;
const { Option } = Select;

export default function ProjectTrackingBoard() {
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [advancingUuid, setAdvancingUuid] = useState(null);
  const [viewMode, setViewMode] = useState("kanban");
  const [filters, setFilters] = useState({
    keyword: "",
    managerId: undefined,
    projectTypeId: undefined,
    riskTag: undefined,
  });
  const [board, setBoard] = useState({
    stages: [],
    stalledDaysThreshold: 30,
    permissions: {},
    summary: {},
    items: [],
  });

  const loadBoard = useCallback(async () => {
    setLoading(true);
    try {
      const response = await projectAPI.getTrackingBoard();
      const payload = response?.data ?? response ?? {};
      setBoard({
        stages: Array.isArray(payload.stages) ? payload.stages : [],
        stalledDaysThreshold: payload.stalledDaysThreshold || 30,
        permissions: payload.permissions || {},
        summary: payload.summary || {},
        items: Array.isArray(payload.items) ? payload.items : [],
      });
    } catch (error) {
      message.error(error?.message || "加载项目跟踪看板失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBoard();
  }, [loadBoard]);

  const permissions = board.permissions || {};
  const hasApprovalAccess = Boolean(
    permissions.canAuditProjectLists || permissions.canAuditWarehouseDocs || permissions.canAuditFinanceVouchers
  );

  const managerOptions = useMemo(() => {
    const map = new Map();
    (board.items || []).forEach((item) => {
      if (item.managerId != null && !map.has(item.managerId)) {
        map.set(item.managerId, item.managerName || `用户${item.managerId}`);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [board.items]);

  const projectTypeOptions = useMemo(() => {
    const map = new Map();
    (board.items || []).forEach((item) => {
      if (item.projectTypeId != null && !map.has(item.projectTypeId)) {
        map.set(item.projectTypeId, item.projectTypeName || `类型${item.projectTypeId}`);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [board.items]);

  const filteredItems = useMemo(
    () => filterTrackingCards(board.items || [], filters),
    [board.items, filters]
  );

  const displaySummary = useMemo(
    () => ({
      totalProjects: filteredItems.length,
      activeProjects: filteredItems.filter((item) => !item.completed).length,
      overdueCount: filteredItems.filter(
        (item) => item.overdue || (item.riskTags || []).includes("OVERDUE")
      ).length,
      stalledCount: filteredItems.filter((item) => (item.riskTags || []).includes("STALLED")).length,
      warrantyExpiringCount: filteredItems.filter(
        (item) => item.warrantyStatus === "EXPIRING_SOON" || (item.riskTags || []).includes("WARRANTY_EXPIRING")
      ).length,
      pendingAuditProjectCount: filteredItems.filter(
        (item) => Number(item.totalPendingAuditCount || 0) > 0 || (item.riskTags || []).includes("PENDING_AUDIT")
      ).length,
      totalReceivableAmount: filteredItems.reduce((sum, item) => sum + Number(item.receivableAmount || 0), 0),
    }),
    [filteredItems]
  );

  const swimlanes = useMemo(
    () => groupCardsIntoSwimlanes(board.stages || [], filteredItems),
    [board.stages, filteredItems]
  );

  const handleQuickAdvance = useCallback(async (card) => {
    if (!card?.uuid || !card?.nextStageId) {
      return;
    }
    setAdvancingUuid(card.uuid);
    try {
      await projectAPI.createProjectStatusHistory({
        projectId: card.uuid,
        stageId: card.nextStageId,
        createdBy: currentUser?.id || null,
      });
      message.success(`项目 ${card.projectNumber} 已推进至“${card.nextStageName || "下一阶段"}”`);
      await loadBoard();
    } catch (error) {
      message.error(error?.message || "推进项目阶段失败");
    } finally {
      setAdvancingUuid(null);
    }
  }, [currentUser?.id, loadBoard]);

  const handleExportExcel = () => {
    const headers = [
      "项目编号",
      "项目名称",
      "客户名称",
      "负责人",
      "项目类型",
      "当前阶段",
      "阶段停留天数",
      "计划结束日期",
      "质保状态",
      "待审核数",
      "风险标签",
    ];
    if (permissions.canViewContracts) {
      headers.push("合同金额");
    }
    if (permissions.canViewFinance) {
      headers.push("已回款金额", "已支出金额", "应收金额", "回款率(%)", "估算毛利");
    }

    const rows = filteredItems.map((item) => {
      const base = [
        item.projectNumber || "",
        item.projectName || "",
        item.customerName || "",
        item.managerName || "",
        item.projectTypeName || "",
        item.currentStageCode ? `${item.currentStageCode} ${item.currentStageName || ""}` : item.currentStageName || "未开始",
        item.stageStayDays ?? "",
        item.planEndTime ? dayjs(item.planEndTime).format("YYYY-MM-DD") : "",
        getWarrantyMeta(item.warrantyStatus).label,
        item.totalPendingAuditCount ?? 0,
        (item.riskTags || []).map((tag) => getRiskMeta(tag).label).join(" / "),
      ];
      if (permissions.canViewContracts) {
        base.push(item.contractAmount ?? "");
      }
      if (permissions.canViewFinance) {
        base.push(
          item.receivedAmount ?? "",
          item.expenseAmount ?? "",
          item.receivableAmount ?? "",
          item.collectionRate ?? "",
          item.estimatedGrossProfit ?? ""
        );
      }
      return base;
    });

    downloadExcel(`project-tracking-board-${dayjs().format("YYYY-MM-DD")}.xls`, "项目跟踪看板", headers, rows);
  };

  const toggleRiskFilter = (riskCode) => {
    setFilters((prev) => ({
      ...prev,
      riskTag: prev.riskTag === riskCode ? undefined : riskCode,
    }));
  };

  const tableColumns = useMemo(() => {
    const cols = [
      {
        title: "项目",
        key: "project",
        fixed: "left",
        width: 220,
        render: (_, record) => (
          <Space direction="vertical" size={0}>
            <Button
              type="link"
              style={{ padding: 0, height: "auto", fontWeight: 600 }}
              onClick={() => navigate(`/project/dashboard?projectUuid=${record.uuid}`)}
            >
              {record.projectNumber} · {record.projectName}
            </Button>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {record.customerName || "-"}
            </Text>
          </Space>
        ),
      },
      { title: "负责人", dataIndex: "managerName", key: "managerName", width: 100, render: (v) => v || "-" },
      {
        title: "当前阶段",
        key: "stage",
        width: 160,
        sorter: (a, b) => (a.currentStageSortOrder || 0) - (b.currentStageSortOrder || 0),
        render: (_, record) => (
          <Space direction="vertical" size={2}>
            <Tag color={record.completed ? "success" : "blue"}>
              {record.currentStageCode ? `${record.currentStageCode} ${record.currentStageName}` : record.currentStageName || "未开始"}
            </Tag>
            {record.stageStayDays != null ? (
              <Text type="secondary" style={{ fontSize: 12 }}>
                已停留 {record.stageStayDays} 天
              </Text>
            ) : null}
          </Space>
        ),
      },
      {
        title: "计划 / 质保",
        key: "schedule",
        width: 170,
        render: (_, record) => {
          const warrantyMeta = getWarrantyMeta(record.warrantyStatus);
          return (
            <Space direction="vertical" size={2}>
              <Text style={{ fontSize: 12 }}>
                截止：{record.planEndTime ? dayjs(record.planEndTime).format("YYYY-MM-DD") : "-"}
              </Text>
              <Tag color={warrantyMeta.color}>{warrantyMeta.label}</Tag>
            </Space>
          );
        },
      },
    ];

    if (permissions.canViewContracts) {
      cols.push({
        title: "合同总额",
        dataIndex: "contractAmount",
        key: "contractAmount",
        width: 120,
        align: "right",
        sorter: (a, b) => Number(a.contractAmount || 0) - Number(b.contractAmount || 0),
        render: (v) => formatMoney(v),
      });
    }

    if (permissions.canViewFinance) {
      cols.push(
        {
          title: "已回款 / 回款率",
          key: "receivedAmount",
          width: 150,
          align: "right",
          render: (_, record) => (
            <Space direction="vertical" size={0} style={{ width: "100%", textAlign: "right" }}>
              <Text>{formatMoney(record.receivedAmount)}</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>
                {formatPercent(record.collectionRate)}
              </Text>
            </Space>
          ),
        },
        {
          title: "应收金额",
          dataIndex: "receivableAmount",
          key: "receivableAmount",
          width: 120,
          align: "right",
          render: (v) => <Text type={Number(v || 0) > 0 ? "danger" : undefined}>{formatMoney(v)}</Text>,
        },
        {
          title: "估算毛利",
          dataIndex: "estimatedGrossProfit",
          key: "estimatedGrossProfit",
          width: 120,
          align: "right",
          render: (v) => <Text type={Number(v || 0) < 0 ? "danger" : "success"}>{formatMoney(v)}</Text>,
        }
      );
    }

    cols.push(
      {
        title: "待审单据",
        dataIndex: "totalPendingAuditCount",
        key: "totalPendingAuditCount",
        width: 100,
        align: "center",
        render: (count) =>
          count > 0 ? (
            hasApprovalAccess ? (
              <Tooltip title="点击前往待审核工作台">
                <Tag color="processing" style={{ cursor: "pointer" }} onClick={() => navigate("/approvals/pending")}>
                  {count} 项待审
                </Tag>
              </Tooltip>
            ) : (
              <Tag color="processing">{count} 项待审</Tag>
            )
          ) : (
            <Tag>0</Tag>
          ),
      },
      {
        title: "风险预警",
        dataIndex: "riskTags",
        key: "riskTags",
        width: 210,
        render: (tags = []) =>
          tags.length ? (
            <Space wrap size={[4, 4]}>
              {tags.map((tag) => {
                const meta = getRiskMeta(tag);
                return (
                  <Tag key={tag} color={meta.color}>
                    {meta.label}
                  </Tag>
                );
              })}
            </Space>
          ) : (
            <Tag color="success">正常</Tag>
          ),
      },
      {
        title: "操作",
        key: "actions",
        fixed: "right",
        width: 210,
        render: (_, record) => (
          <Space size={4}>
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/project/dashboard?projectUuid=${record.uuid}`)}
            >
              总览
            </Button>
            <Button
              size="small"
              icon={<ReconciliationOutlined />}
              onClick={() => navigate(`/project/reconciliation?projectUuid=${record.uuid}`)}
            >
              对账
            </Button>
            {permissions.canManageProject && record.nextStageId ? (
              <Popconfirm
                title={`确认推进至“${record.nextStageName}”？`}
                okText="确认推进"
                cancelText="取消"
                onConfirm={() => handleQuickAdvance(record)}
              >
                <Button size="small" type="primary" loading={advancingUuid === record.uuid}>
                  推进
                </Button>
              </Popconfirm>
            ) : null}
          </Space>
        ),
      }
    );

    return cols;
  }, [
    advancingUuid,
    handleQuickAdvance,
    hasApprovalAccess,
    navigate,
    permissions.canManageProject,
    permissions.canViewContracts,
    permissions.canViewFinance,
  ]);

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space direction="vertical" size={4}>
              <Space>
                <DashboardOutlined style={{ fontSize: 20, color: "#1677ff" }} />
                <Title level={4} style={{ margin: 0 }}>
                  项目跟踪看板
                </Title>
                <Tag color="blue">阶段滞留阈值 {board.stalledDaysThreshold || 30} 天</Tag>
              </Space>
              <Text type="secondary">
                按阶段泳道实时监控全部项目进度、停留天数、质保临期、回款进度与待审核单据（跨模块数据严格按当前账号权限展示）。
              </Text>
            </Space>
          </Col>
          <Col>
            <Space wrap>
              <Segmented
                value={viewMode}
                onChange={setViewMode}
                options={[
                  { label: "看板视图", value: "kanban", icon: <AppstoreOutlined /> },
                  { label: "列表视图", value: "table", icon: <UnorderedListOutlined /> },
                ]}
              />
              <Button icon={<DownloadOutlined />} onClick={handleExportExcel} disabled={!filteredItems.length}>
                导出 Excel
              </Button>
              <Button icon={<ReloadOutlined />} onClick={loadBoard} loading={loading}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={[12, 12]}>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small" hoverable onClick={() => setFilters((prev) => ({ ...prev, riskTag: undefined }))}>
            <Statistic title="项目总数" value={displaySummary.totalProjects || 0} suffix={`进行中 ${displaySummary.activeProjects || 0}`} />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small" hoverable onClick={() => toggleRiskFilter("OVERDUE")}>
            <Statistic
              title="计划超期"
              value={displaySummary.overdueCount || 0}
              valueStyle={{ color: (displaySummary.overdueCount || 0) > 0 ? "#cf1322" : undefined }}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small" hoverable onClick={() => toggleRiskFilter("STALLED")}>
            <Statistic
              title="阶段滞留"
              value={displaySummary.stalledCount || 0}
              valueStyle={{ color: (displaySummary.stalledCount || 0) > 0 ? "#d46b08" : undefined }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small" hoverable onClick={() => toggleRiskFilter("WARRANTY_EXPIRING")}>
            <Statistic
              title="质保临期"
              value={displaySummary.warrantyExpiringCount || 0}
              valueStyle={{ color: (displaySummary.warrantyExpiringCount || 0) > 0 ? "#d48806" : undefined }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={8} lg={4}>
          <Card size="small" hoverable onClick={() => toggleRiskFilter("PENDING_AUDIT")}>
            <Statistic
              title="有待审单据项目"
              value={displaySummary.pendingAuditProjectCount || 0}
              valueStyle={{ color: (displaySummary.pendingAuditProjectCount || 0) > 0 ? "#1677ff" : undefined }}
            />
          </Card>
        </Col>
        {permissions.canViewFinance ? (
          <Col xs={12} sm={8} lg={4}>
            <Card size="small">
              <Statistic
                title="项目应收合计"
                value={Number(displaySummary.totalReceivableAmount || 0)}
                precision={2}
                valueStyle={{ color: Number(displaySummary.totalReceivableAmount || 0) > 0 ? "#cf1322" : "#3f8600" }}
              />
            </Card>
          </Col>
        ) : null}
      </Row>

      {!permissions.canViewContracts || !permissions.canViewFinance || !permissions.canViewInventory ? (
        <Alert
          type="info"
          showIcon
          message="当前账号部分关联模块未授权"
          description="未授予合同、财务或库存访问权限时，对应金额与单据指标将自动隐藏，不影响查看所属项目进度。"
        />
      ) : null}

      <Card size="small">
        <Space wrap size={12} style={{ width: "100%" }}>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="搜索项目编号 / 名称 / 客户 / 负责人"
            style={{ width: 280 }}
            value={filters.keyword}
            onChange={(event) => setFilters((prev) => ({ ...prev, keyword: event.target.value }))}
          />
          <Select
            allowClear
            placeholder="按负责人筛选"
            style={{ width: 160 }}
            value={filters.managerId}
            onChange={(value) => setFilters((prev) => ({ ...prev, managerId: value }))}
          >
            {managerOptions.map((item) => (
              <Option key={item.id} value={item.id}>
                {item.name}
              </Option>
            ))}
          </Select>
          <Select
            allowClear
            placeholder="按项目类型筛选"
            style={{ width: 160 }}
            value={filters.projectTypeId}
            onChange={(value) => setFilters((prev) => ({ ...prev, projectTypeId: value }))}
          >
            {projectTypeOptions.map((item) => (
              <Option key={item.id} value={item.id}>
                {item.name}
              </Option>
            ))}
          </Select>
          <Select
            allowClear
            placeholder="按风险预警筛选"
            style={{ width: 180 }}
            value={filters.riskTag}
            onChange={(value) => setFilters((prev) => ({ ...prev, riskTag: value }))}
          >
            {RISK_OPTIONS.map((item) => (
              <Option key={item.value} value={item.value}>
                {item.label}
              </Option>
            ))}
          </Select>
          <Button
            onClick={() =>
              setFilters({
                keyword: "",
                managerId: undefined,
                projectTypeId: undefined,
                riskTag: undefined,
              })
            }
          >
            重置筛选
          </Button>
          <Text type="secondary">共匹配 {filteredItems.length} 个项目</Text>
        </Space>
      </Card>

      {loading && !(board.items || []).length ? (
        <Card>
          <div style={{ padding: 48, textAlign: "center" }}>
            <Spin size="large" />
          </div>
        </Card>
      ) : filteredItems.length === 0 ? (
        <Card>
          <Empty description="暂无匹配的项目记录" />
        </Card>
      ) : viewMode === "table" ? (
        <Card>
          <Table
            rowKey="uuid"
            columns={tableColumns}
            dataSource={filteredItems}
            scroll={{ x: "max-content" }}
            pagination={{ defaultPageSize: 15, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
          />
        </Card>
      ) : (
        <div
          style={{
            display: "flex",
            gap: 14,
            overflowX: "auto",
            paddingBottom: 12,
            alignItems: "flex-start",
          }}
        >
          {swimlanes.map((lane) => {
            const laneContractTotal = lane.cards.reduce(
              (sum, item) => sum + Number(item.contractAmount || 0),
              0
            );
            return (
              <div
                key={lane.key}
                style={{
                  minWidth: 300,
                  maxWidth: 320,
                  flex: "0 0 300px",
                  background: "#f5f7fa",
                  borderRadius: 10,
                  border: "1px solid #e5e7eb",
                  padding: 12,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: 10,
                    paddingBottom: 8,
                    borderBottom: "1px solid #e5e7eb",
                  }}
                >
                  <Space size={6}>
                    <Tag color={lane.stageId ? "blue" : "default"} style={{ marginRight: 0 }}>
                      {lane.stageCode}
                    </Tag>
                    <Text strong>{lane.description}</Text>
                  </Space>
                  <Space size={6}>
                    {permissions.canViewContracts && laneContractTotal > 0 ? (
                      <Tooltip title="泳道合同金额合计">
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          ¥{formatMoney(laneContractTotal)}
                        </Text>
                      </Tooltip>
                    ) : null}
                    <Badge count={lane.cards.length} showZero color={lane.cards.length > 0 ? "#1677ff" : "#bfbfbf"} />
                  </Space>
                </div>

                {lane.cards.length === 0 ? (
                  <div style={{ padding: "24px 0", textAlign: "center", color: "#9ca3af", fontSize: 12 }}>
                    当前阶段暂无项目
                  </div>
                ) : (
                  <Space direction="vertical" size={10} style={{ width: "100%" }}>
                    {lane.cards.map((card) => (
                      <Card
                        key={card.uuid}
                        size="small"
                        hoverable
                        styles={{ body: { padding: 12 } }}
                      >
                        <Space direction="vertical" size={6} style={{ width: "100%" }}>
                          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 6 }}>
                            <Button
                              type="link"
                              style={{ padding: 0, height: "auto", textAlign: "left", fontWeight: 600, whiteSpace: "normal" }}
                              onClick={() => navigate(`/project/dashboard?projectUuid=${card.uuid}`)}
                            >
                              {card.projectNumber} · {card.projectName}
                            </Button>
                            {card.totalPendingAuditCount > 0 ? (
                              hasApprovalAccess ? (
                                <Tooltip title="点击前往待审核工作台">
                                  <Tag
                                    color="processing"
                                    style={{ cursor: "pointer", marginRight: 0, flexShrink: 0 }}
                                    onClick={() => navigate("/approvals/pending")}
                                  >
                                    审 {card.totalPendingAuditCount}
                                  </Tag>
                                </Tooltip>
                              ) : (
                                <Tag color="processing" style={{ marginRight: 0, flexShrink: 0 }}>
                                  审 {card.totalPendingAuditCount}
                                </Tag>
                              )
                            ) : null}
                          </div>

                          <Text type="secondary" style={{ fontSize: 12 }}>
                            客户：{card.customerName || "-"} · 负责人：{card.managerName || "-"}
                          </Text>

                          <Space wrap size={[4, 4]}>
                            {card.stageStayDays != null ? (
                              <Tag color={card.riskTags.includes("STALLED") ? "warning" : "default"}>
                                停留 {card.stageStayDays} 天
                              </Tag>
                            ) : null}
                            {card.warrantyStatus && card.warrantyStatus !== "NONE" ? (
                              <Tag color={getWarrantyMeta(card.warrantyStatus).color}>
                                {getWarrantyMeta(card.warrantyStatus).label}
                              </Tag>
                            ) : null}
                            {card.riskTags
                              .filter((tag) => !["STALLED", "WARRANTY_EXPIRING", "WARRANTY_EXPIRED"].includes(tag))
                              .map((tag) => {
                                const meta = getRiskMeta(tag);
                                return (
                                  <Tag key={tag} color={meta.color}>
                                    {meta.label}
                                  </Tag>
                                );
                              })}
                          </Space>

                          {permissions.canViewContracts || permissions.canViewFinance ? (
                            <div style={{ background: "#fafafa", padding: "6px 8px", borderRadius: 6, fontSize: 12 }}>
                              {permissions.canViewContracts ? (
                                <div style={{ display: "flex", justifyContent: "space-between" }}>
                                  <Text type="secondary">合同额</Text>
                                  <Text strong>¥{formatMoney(card.contractAmount)}</Text>
                                </div>
                              ) : null}
                              {permissions.canViewFinance ? (
                                <>
                                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                                    <Text type="secondary">已回 / 毛利</Text>
                                    <Text>
                                      ¥{formatMoney(card.receivedAmount)} /{" "}
                                      <Text type={Number(card.estimatedGrossProfit || 0) < 0 ? "danger" : "success"}>
                                        ¥{formatMoney(card.estimatedGrossProfit)}
                                      </Text>
                                    </Text>
                                  </div>
                                  {card.collectionRate != null ? (
                                    <Progress
                                      percent={Math.min(100, Math.max(0, Number(card.collectionRate || 0)))}
                                      size="small"
                                      style={{ marginTop: 2, marginBottom: 0 }}
                                    />
                                  ) : null}
                                </>
                              ) : null}
                            </div>
                          ) : null}

                          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingTop: 4 }}>
                            <Space size={4}>
                              <Button
                                size="small"
                                onClick={() => navigate(`/project/dashboard?projectUuid=${card.uuid}`)}
                              >
                                详情
                              </Button>
                              <Button
                                size="small"
                                onClick={() => navigate(`/project/reconciliation?projectUuid=${card.uuid}`)}
                              >
                                对账
                              </Button>
                            </Space>
                            {permissions.canManageProject && card.nextStageId ? (
                              <Popconfirm
                                title={`推进至“${card.nextStageName}”？`}
                                okText="确认"
                                cancelText="取消"
                                onConfirm={() => handleQuickAdvance(card)}
                              >
                                <Button
                                  size="small"
                                  type="primary"
                                  ghost
                                  icon={<ArrowRightOutlined />}
                                  loading={advancingUuid === card.uuid}
                                >
                                  推进
                                </Button>
                              </Popconfirm>
                            ) : null}
                          </div>
                        </Space>
                      </Card>
                    ))}
                  </Space>
                )}
              </div>
            );
          })}
        </div>
      )}
    </Space>
  );
}
