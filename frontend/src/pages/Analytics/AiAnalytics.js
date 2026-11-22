import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Collapse,
  Empty,
  Input,
  List,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from "antd";
import {
  ArrowRightOutlined,
  BulbOutlined,
  CheckCircleOutlined,
  RobotOutlined,
  SendOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { aiAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { formatProjectOptionLabel } from "../../utils/projectLabels";

const { Paragraph, Text, Title } = Typography;
const { Option } = Select;

const SCOPE_LABELS = {
  PROJECT: "项目管理",
  CONTRACT: "合同信息",
  INVENTORY: "库存与采发对账",
  FINANCE: "财务核算与账龄",
  APPROVAL: "审批工作台",
};

const SEVERITY_META = {
  HIGH: { label: "高风险", color: "error" },
  MEDIUM: { label: "中风险", color: "warning" },
  LOW: { label: "低风险", color: "processing" },
};

const PRESET_QUESTIONS = [
  { scope: "PROJECT", text: "目前有哪些超期、滞留或质保临期项目需要优先处理？" },
  { scope: "APPROVAL", text: "当前共有多少待审核单据？分布在哪几个模块？" },
  { scope: "INVENTORY", text: "各项目的采购缺口和发货履约情况如何？" },
  { scope: "FINANCE", text: "公司当前整体收入、毛利以及90天以上应收账款是多少？" },
];

export default function AiAnalytics() {
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const [statusLoading, setStatusLoading] = useState(false);
  const [briefingLoading, setBriefingLoading] = useState(false);
  const [askLoading, setAskLoading] = useState(false);

  const [status, setStatus] = useState({
    enabled: false,
    configured: false,
    localEndpoint: false,
    model: null,
    mode: "RULE_BASED",
    authorizedScopes: [],
  });
  const [projects, setProjects] = useState([]);
  const [selectedProjectUuid, setSelectedProjectUuid] = useState(undefined);
  const [briefing, setBriefing] = useState(null);
  const [question, setQuestion] = useState("");
  const [qaHistory, setQaHistory] = useState([]);

  const canViewProjects = useMemo(
    () => hasAnyAuthority(currentUser, ["project.access", "project.manage"]),
    [currentUser]
  );

  const loadBriefing = useCallback(async (projectUuid) => {
    setBriefingLoading(true);
    try {
      const response = await aiAPI.generateBriefing(projectUuid || null);
      setBriefing(response?.data ?? response ?? null);
    } catch (error) {
      message.error(error?.message || "生成经营简报失败");
    } finally {
      setBriefingLoading(false);
    }
  }, []);

  const loadInitial = useCallback(async () => {
    setStatusLoading(true);
    try {
      const [statusRes, projectRes] = await Promise.all([
        aiAPI.getStatus(),
        canViewProjects ? projectAPI.getProjectOptions().catch(() => []) : Promise.resolve([]),
      ]);
      const statusPayload = statusRes?.data ?? statusRes ?? {};
      setStatus({
        enabled: Boolean(statusPayload.enabled),
        configured: Boolean(statusPayload.configured),
        localEndpoint: Boolean(statusPayload.localEndpoint),
        model: statusPayload.model || null,
        mode: statusPayload.mode || "RULE_BASED",
        authorizedScopes: Array.isArray(statusPayload.authorizedScopes) ? statusPayload.authorizedScopes : [],
      });
      const projectRows = Array.isArray(projectRes?.data)
        ? projectRes.data
        : Array.isArray(projectRes)
          ? projectRes
          : [];
      setProjects(projectRows);
    } catch (error) {
      message.error(error?.message || "加载智能分析状态失败");
    } finally {
      setStatusLoading(false);
    }
  }, [canViewProjects]);

  useEffect(() => {
    loadInitial();
    loadBriefing(undefined);
  }, [loadBriefing, loadInitial]);

  const handleAsk = async (presetText) => {
    const promptText = String(typeof presetText === "string" ? presetText : question).trim();
    if (!promptText) {
      message.warning("请输入需要分析的问题");
      return;
    }
    setAskLoading(true);
    try {
      const response = await aiAPI.ask({
        question: promptText,
        projectUuid: selectedProjectUuid || null,
      });
      const payload = response?.data ?? response ?? {};
      setQaHistory((prev) => [
        {
          id: `${Date.now()}-${Math.random()}`,
          question: promptText,
          mode: payload.mode || "RULE_BASED",
          fallbackUsed: Boolean(payload.fallbackUsed),
          fallbackReason: payload.fallbackReason || null,
          model: payload.model || null,
          answer: payload.answer || "",
          evidence: payload.evidence || {},
        },
        ...prev,
      ]);
      if (typeof presetText !== "string") {
        setQuestion("");
      }
    } catch (error) {
      message.error(error?.message || "智能问答请求失败");
    } finally {
      setAskLoading(false);
    }
  };

  const availablePresets = useMemo(() => {
    const scopeSet = new Set(status.authorizedScopes || []);
    return PRESET_QUESTIONS.filter((item) => scopeSet.size === 0 || scopeSet.has(item.scope));
  }, [status.authorizedScopes]);

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space direction="vertical" size={4}>
              <Space wrap>
                <RobotOutlined style={{ fontSize: 20, color: "#1677ff" }} />
                <Title level={4} style={{ margin: 0 }}>
                  智能经营简报与受控问答
                </Title>
                <Tag color={status.configured ? "success" : "blue"}>
                  {status.configured
                    ? `AI 大模型模式${status.model ? ` (${status.model})` : ""}`
                    : "规则分析模式（无需外部 Key）"}
                </Tag>
                {status.configured && !status.localEndpoint ? (
                  <Tag color="cyan">客户名称已自动脱敏</Tag>
                ) : null}
              </Space>
              <Text type="secondary">
                所有分析严格基于当前账号有权访问的确定性后台汇总接口（RBAC 权限隔离），支持本地 Ollama 或 OpenAI 兼容模型。
              </Text>
              <Space wrap size={6}>
                <Text type="secondary" style={{ fontSize: 12 }}>当前数据域权限：</Text>
                {(status.authorizedScopes || []).map((scope) => (
                  <Tag key={scope}>{SCOPE_LABELS[scope] || scope}</Tag>
                ))}
              </Space>
            </Space>
          </Col>
          <Col>
            <Space wrap>
              {canViewProjects ? (
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="children"
                  placeholder="全公司项目总览（可选指定项目）"
                  style={{ width: 260 }}
                  value={selectedProjectUuid}
                  onChange={(value) => {
                    setSelectedProjectUuid(value);
                    loadBriefing(value);
                  }}
                >
                  {projects.map((item) => (
                    <Option key={item.uuid} value={item.uuid}>
                      {formatProjectOptionLabel(item)}
                    </Option>
                  ))}
                </Select>
              ) : null}
              <Button
                type="primary"
                icon={<BulbOutlined />}
                loading={briefingLoading || statusLoading}
                onClick={() => loadBriefing(selectedProjectUuid)}
              >
                生成经营简报
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {briefing?.fallbackUsed ? (
        <Alert
          type="warning"
          showIcon
          message="AI 模型降级提示"
          description={briefing.fallbackReason || "外部模型暂不可用，已切换为确定性规则引擎分析。"}
        />
      ) : null}

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={13}>
          <Card
            title="经营简报与风险预警"
            extra={
              briefing ? (
                <Tag color={briefing.mode === "LLM" ? "success" : "blue"}>
                  {briefing.mode === "LLM" ? `LLM 摘要 · ${briefing.model || ""}` : "确定性规则引擎"}
                </Tag>
              ) : null
            }
          >
            {briefingLoading && !briefing ? (
              <div style={{ padding: 48, textAlign: "center" }}>
                <Spin size="large" />
              </div>
            ) : !briefing ? (
              <Empty description="点击“生成经营简报”查看当前数据摘要" />
            ) : (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <Card size="small" style={{ background: "#fafafa" }}>
                  <Paragraph style={{ marginBottom: 0, whiteSpace: "pre-line" }}>
                    {briefing.summary}
                  </Paragraph>
                </Card>

                {briefing.highlights?.length ? (
                  <div>
                    <Text strong>关键指标快照</Text>
                    <List
                      size="small"
                      dataSource={briefing.highlights}
                      renderItem={(item) => (
                        <List.Item>
                          <Space>
                            <CheckCircleOutlined style={{ color: "#52c41a" }} />
                            <Text>{item}</Text>
                          </Space>
                        </List.Item>
                      )}
                    />
                  </div>
                ) : null}

                <div>
                  <Text strong>识别到的风险与待办 ({briefing.risks?.length || 0})</Text>
                  {briefing.risks?.length ? (
                    <List
                      size="small"
                      dataSource={briefing.risks}
                      renderItem={(risk) => {
                        const sev = SEVERITY_META[risk.severity] || SEVERITY_META.MEDIUM;
                        return (
                          <List.Item
                            actions={
                              risk.targetRoute
                                ? [
                                    <Button
                                      key="go"
                                      size="small"
                                      type="link"
                                      icon={<ArrowRightOutlined />}
                                      onClick={() => navigate(risk.targetRoute)}
                                    >
                                      处理
                                    </Button>,
                                  ]
                                : undefined
                            }
                          >
                            <List.Item.Meta
                              avatar={<WarningOutlined style={{ color: sev.color === "error" ? "#cf1322" : "#faad14", marginTop: 4 }} />}
                              title={
                                <Space wrap size={6}>
                                  <Tag color={sev.color}>{sev.label}</Tag>
                                  <Tag>{risk.category}</Tag>
                                  <Text strong>{risk.title}</Text>
                                </Space>
                              }
                              description={risk.detail}
                            />
                          </List.Item>
                        );
                      }}
                    />
                  ) : (
                    <div style={{ marginTop: 8 }}>
                      <Alert type="success" showIcon message="当前权限范围内暂无重点风险" />
                    </div>
                  )}
                </div>

                {briefing.recommendations?.length ? (
                  <div>
                    <Text strong>行动建议</Text>
                    <List
                      size="small"
                      dataSource={briefing.recommendations}
                      renderItem={(item, idx) => (
                        <List.Item>
                          <Text>
                            {idx + 1}. {item}
                          </Text>
                        </List.Item>
                      )}
                    />
                  </div>
                ) : null}

                <Collapse
                  size="small"
                  items={[
                    {
                      key: "evidence",
                      label: "查看数据依据（已授权后台聚合快照）",
                      children: (
                        <pre style={{ maxHeight: 280, overflow: "auto", fontSize: 12, margin: 0 }}>
                          {JSON.stringify(briefing.evidence || {}, null, 2)}
                        </pre>
                      ),
                    },
                  ]}
                />
              </Space>
            )}
          </Card>
        </Col>

        <Col xs={24} xl={11}>
          <Card title="受控经营问答（基于后台确定性数据）">
            <Space direction="vertical" size={12} style={{ width: "100%" }}>
              <div>
                <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 6 }}>
                  快捷问题（仅展示当前账号有权限查询的数据域）：
                </Text>
                <Space wrap size={[6, 6]}>
                  {availablePresets.map((preset) => (
                    <Button key={preset.scope} size="small" onClick={() => handleAsk(preset.text)} loading={askLoading}>
                      {preset.text}
                    </Button>
                  ))}
                </Space>
              </div>

              <Space.Compact style={{ width: "100%" }}>
                <Input
                  placeholder="例如：哪些项目超期了？90天以上应收有多少？"
                  value={question}
                  onChange={(event) => setQuestion(event.target.value)}
                  onPressEnter={() => handleAsk()}
                />
                <Button type="primary" icon={<SendOutlined />} loading={askLoading} onClick={() => handleAsk()}>
                  提问
                </Button>
              </Space.Compact>

              {qaHistory.length === 0 ? (
                <Empty description="暂无问答记录，请点击快捷问题或输入查询" />
              ) : (
                <Space direction="vertical" size={12} style={{ width: "100%" }}>
                  {qaHistory.map((item) => (
                    <Card key={item.id} size="small">
                      <Space direction="vertical" size={8} style={{ width: "100%" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8 }}>
                          <Text strong>问：{item.question}</Text>
                          <Tag color={item.mode === "LLM" ? "success" : "blue"}>
                            {item.mode === "LLM" ? `LLM${item.model ? ` · ${item.model}` : ""}` : "规则引擎"}
                          </Tag>
                        </div>
                        {item.fallbackUsed ? (
                          <Alert type="warning" showIcon message={item.fallbackReason || "已自动降级为规则引擎"} />
                        ) : null}
                        <Paragraph style={{ marginBottom: 0, whiteSpace: "pre-line" }}>
                          {item.answer}
                        </Paragraph>
                      </Space>
                    </Card>
                  ))}
                </Space>
              )}
            </Space>
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
