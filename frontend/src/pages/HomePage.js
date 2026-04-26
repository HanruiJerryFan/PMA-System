import React, { useEffect, useMemo, useState } from "react";
import { Alert, Button, Card, Col, List, Progress, Row, Space, Statistic, Tag, Typography } from "antd";
import {
  ApiOutlined,
  DatabaseOutlined,
  EnvironmentOutlined,
  FileTextOutlined,
  ProjectOutlined,
  ReloadOutlined,
  ShoppingOutlined,
  TeamOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { getCurrentUser } from "../api/auth";
import {
  contractAPI,
  customerAPI,
  inventoryAPI,
  logAPI,
  permissionAPI,
  productAPI,
  projectAPI,
  regionAPI,
} from "../api/modules";
import { hasAnyAuthority } from "../utils/authorities";

const { Paragraph, Text, Title } = Typography;

function sortRecent(items, fields = ["updateTime", "createTime", "entryDate"]) {
  return [...items].sort((left, right) => {
    const leftValue = fields.map((field) => left?.[field]).find(Boolean);
    const rightValue = fields.map((field) => right?.[field]).find(Boolean);
    return new Date(rightValue || 0).getTime() - new Date(leftValue || 0).getTime();
  });
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString() : "-";
}

export default function HomePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [dashboard, setDashboard] = useState({
    users: [],
    customers: [],
    projects: [],
    projectLists: [],
    contracts: [],
    materials: [],
    warehouses: [],
    inventoryRows: [],
    loginLogs: [],
    provinces: [],
  });
  const [apiStatus, setApiStatus] = useState({});

  const permissions = useMemo(
    () => ({
      canViewUsers: hasAnyAuthority(currentUser, ["permission.users.manage"]),
      canViewCustomers: hasAnyAuthority(currentUser, ["customer.access"]),
      canViewProjects: hasAnyAuthority(currentUser, ["project.access"]),
      canManageProjects: hasAnyAuthority(currentUser, ["project.manage"]),
      canViewContracts: hasAnyAuthority(currentUser, ["contract.access"]),
      canViewProducts: hasAnyAuthority(currentUser, ["product.access"]),
      canManageProducts: hasAnyAuthority(currentUser, ["product.manage"]),
      canViewInventory: hasAnyAuthority(currentUser, ["inventory.access"]),
      canManageInventory: hasAnyAuthority(currentUser, ["inventory.manage"]),
      canViewLogs: hasAnyAuthority(currentUser, ["logs.view"]),
    }),
    [currentUser]
  );

  const loadDashboard = async () => {
    setLoading(true);
    try {
      const user = await getCurrentUser();
      setCurrentUser(user);

      const loaders = [];
      if (hasAnyAuthority(user, ["permission.users.manage"])) {
        loaders.push({ key: "users", label: "用户", request: permissionAPI.getUsers });
      }
      if (hasAnyAuthority(user, ["customer.access"])) {
        loaders.push({ key: "customers", label: "客户", request: customerAPI.getCustomers });
        loaders.push({ key: "provinces", label: "地区", request: regionAPI.getProvinces });
      }
      if (hasAnyAuthority(user, ["project.access"])) {
        loaders.push({ key: "projects", label: "项目", request: projectAPI.getProjects });
        loaders.push({ key: "projectLists", label: "项目清单", request: projectAPI.getProjectLists });
      }
      if (hasAnyAuthority(user, ["contract.access"])) {
        loaders.push({ key: "contracts", label: "合同", request: contractAPI.getContracts });
      }
      if (hasAnyAuthority(user, ["product.access"])) {
        loaders.push({ key: "materials", label: "物料", request: productAPI.getProducts });
      }
      if (hasAnyAuthority(user, ["inventory.access"])) {
        loaders.push({ key: "warehouses", label: "仓库", request: inventoryAPI.getWarehouses });
        loaders.push({ key: "inventoryRows", label: "库存", request: inventoryAPI.getInventory });
      }
      if (hasAnyAuthority(user, ["logs.view"])) {
        loaders.push({ key: "loginLogs", label: "日志", request: logAPI.getLoginRecords });
      }

      const results = await Promise.allSettled(loaders.map((item) => item.request()));
      const nextDashboard = {
        users: [],
        customers: [],
        projects: [],
        projectLists: [],
        contracts: [],
        materials: [],
        warehouses: [],
        inventoryRows: [],
        loginLogs: [],
        provinces: [],
      };
      const nextApiStatus = {};

      loaders.forEach((item, index) => {
        const result = results[index];
        if (result.status === "fulfilled") {
          nextDashboard[item.key] = Array.isArray(result.value?.data) ? result.value.data : [];
          nextApiStatus[item.label] = { status: "success", message: "正常" };
        } else {
          nextApiStatus[item.label] = { status: "error", message: result.reason?.message || "请求失败" };
        }
      });

      setDashboard(nextDashboard);
      setApiStatus(nextApiStatus);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  const overviewCards = useMemo(() => {
    const items = [];
    if (permissions.canViewCustomers) {
      items.push({ title: "客户", value: dashboard.customers.length, icon: <TeamOutlined />, route: "/customer/info" });
    }
    if (permissions.canViewProjects) {
      items.push({
        title: "项目",
        value: dashboard.projects.length,
        icon: <ProjectOutlined />,
        route: permissions.canManageProjects ? "/project/info" : "/project/lists",
      });
    }
    if (permissions.canViewContracts) {
      items.push({ title: "合同", value: dashboard.contracts.length, icon: <FileTextOutlined />, route: "/contract/info" });
    }
    if (permissions.canManageProducts) {
      items.push({ title: "物料", value: dashboard.materials.length, icon: <ShoppingOutlined />, route: "/product/info" });
    }
    if (permissions.canViewInventory) {
      items.push({
        title: "仓库",
        value: dashboard.warehouses.length,
        icon: <DatabaseOutlined />,
        route: permissions.canManageInventory ? "/inventory/warehouses" : "/inventory/stock",
      });
    }
    if (permissions.canViewUsers) {
      items.push({ title: "用户", value: dashboard.users.length, icon: <UserOutlined />, route: "/permission/users" });
    }
    return items;
  }, [dashboard, permissions]);

  const quickActions = useMemo(() => {
    const items = [];
    if (permissions.canViewCustomers) {
      items.push({ label: "客户管理", route: "/customer/info" });
      items.push({ label: "地区查找", route: "/customer/regions" });
    }
    if (permissions.canViewProjects) {
      items.push({
        label: permissions.canManageProjects ? "项目信息" : "项目清单",
        route: permissions.canManageProjects ? "/project/info" : "/project/lists",
      });
    }
    if (permissions.canManageProducts) {
      items.push({ label: "物料主数据", route: "/product/info" });
    }
    if (permissions.canViewInventory) {
      items.push({
        label: permissions.canManageInventory ? "仓库管理" : "库存汇总",
        route: permissions.canManageInventory ? "/inventory/warehouses" : "/inventory/stock",
      });
    }
    if (permissions.canViewLogs) {
      items.push({ label: "登录日志", route: "/logs/login" });
    }
    return items.slice(0, 6);
  }, [permissions]);

  const recentProjects = useMemo(
    () =>
      sortRecent(dashboard.projects)
        .slice(0, 5)
        .map((item) => ({
          key: item.uuid,
          title: item.projectName || item.projectNumber || "未命名项目",
          subtitle: item.projectNumber || item.uuid,
          time: formatDateTime(item.updateTime || item.createTime),
        })),
    [dashboard.projects]
  );

  const recentCustomers = useMemo(
    () =>
      sortRecent(dashboard.customers, ["lastActiveAt", "updateTime", "createTime"])
        .slice(0, 5)
        .map((item) => ({
          key: item.uuid,
          title: item.customerName || item.customerCode || "未命名客户",
          subtitle: item.customerCode || item.industry || "-",
          time: formatDateTime(item.lastActiveAt || item.updateTime || item.createTime),
        })),
    [dashboard.customers]
  );

  const operationPanels = useMemo(() => {
    const items = [];
    if (permissions.canViewCustomers) {
      items.push({
        title: "客户覆盖",
        count: dashboard.customers.length,
        detail: `已覆盖 ${dashboard.provinces.length} 个地区基础节点`,
        percent: dashboard.provinces.length ? Math.min(100, Math.round((dashboard.customers.length / dashboard.provinces.length) * 100)) : 0,
      });
    }
    if (permissions.canViewProjects) {
      items.push({
        title: "项目执行准备",
        count: dashboard.projects.length,
        detail: `当前已建立 ${dashboard.projectLists.length} 份项目清单`,
        percent: dashboard.projects.length ? Math.min(100, Math.round((dashboard.projectLists.length / dashboard.projects.length) * 100)) : 0,
      });
    }
    if (permissions.canViewInventory) {
      items.push({
        title: "库存可视化",
        count: dashboard.inventoryRows.length,
        detail: `覆盖 ${dashboard.warehouses.length} 个仓库、${dashboard.materials.length} 条物料`,
        percent: dashboard.materials.length ? Math.min(100, Math.round((dashboard.inventoryRows.length / dashboard.materials.length) * 100)) : 0,
      });
    }
    if (permissions.canViewLogs) {
      items.push({
        title: "审计与登录活跃度",
        count: dashboard.loginLogs.length,
        detail: "用于追踪最近登录行为和运行态活跃程度",
        percent: dashboard.loginLogs.length ? 100 : 0,
      });
    }
    return items;
  }, [dashboard, permissions]);

  const healthSummary = useMemo(() => {
    const total = Object.keys(apiStatus).length;
    const healthy = Object.values(apiStatus).filter((item) => item.status === "success").length;
    return { total, healthy };
  }, [apiStatus]);

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card
        variant="borderless"
        style={{
          background: "linear-gradient(135deg, #1f2937 0%, #0f766e 45%, #22c55e 100%)",
          color: "#fff",
          overflow: "hidden",
        }}
        styles={{ body: { padding: 24 } }}
      >
        <Row gutter={[24, 24]} align="middle">
          <Col xs={24} lg={16}>
            <Space direction="vertical" size={8}>
              <Tag color="rgba(255,255,255,0.22)" style={{ border: "none", color: "#fff", width: "fit-content" }}>
                权限感知首页
              </Tag>
              <Title level={2} style={{ color: "#fff", margin: 0 }}>
                销售管理系统运营总览
              </Title>
              <Paragraph style={{ color: "rgba(255,255,255,0.86)", margin: 0, maxWidth: 760 }}>
                首页会根据当前账号权限动态显示模块概览、快捷入口和接口状态。没有权限的模块不会发请求，也不会出现在卡片和快捷操作中。
              </Paragraph>
            </Space>
          </Col>
          <Col xs={24} lg={8}>
            <Card
              size="small"
              variant="borderless"
              style={{ background: "rgba(255,255,255,0.12)" }}
              styles={{ body: { padding: 18 } }}
            >
              <Space direction="vertical" size={10} style={{ width: "100%" }}>
                <Text style={{ color: "#fff", fontSize: 16, fontWeight: 600 }}>当前可见模块</Text>
                <Row gutter={[12, 12]}>
                  <Col span={12}>
                    <Statistic title="概览卡片" value={overviewCards.length} valueStyle={{ color: "#fff" }} />
                  </Col>
                  <Col span={12}>
                    <Statistic title="快捷入口" value={quickActions.length} valueStyle={{ color: "#fff" }} />
                  </Col>
                </Row>
                <Button icon={<ReloadOutlined />} onClick={loadDashboard} loading={loading}>
                  刷新首页
                </Button>
              </Space>
            </Card>
          </Col>
        </Row>
      </Card>

      {overviewCards.length > 0 ? (
        <Row gutter={[16, 16]}>
          {overviewCards.map((item) => (
            <Col key={item.title} xs={24} sm={12} lg={8} xl={4}>
              <Card hoverable loading={loading} onClick={() => navigate(item.route)} styles={{ body: { paddingBottom: 18 } }}>
                <Statistic title={item.title} value={item.value} prefix={item.icon} />
              </Card>
            </Col>
          ))}
        </Row>
      ) : (
        <Alert
          type="info"
          showIcon
          message="当前账号暂未分配业务模块访问权限"
          description="你仍然可以使用修改密码等基础功能。如需业务访问，请联系管理员分配对应 access 权限。"
        />
      )}

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={16}>
          <Card title="运营面板" extra={<Tag color="blue">{operationPanels.length} 个模块</Tag>}>
            {operationPanels.length > 0 ? (
              <Space direction="vertical" size={18} style={{ width: "100%" }}>
                {operationPanels.map((item) => (
                  <div key={item.title}>
                    <Row justify="space-between" align="middle" style={{ marginBottom: 6 }}>
                      <Col>
                        <Text strong>{item.title}</Text>
                      </Col>
                      <Col>
                        <Tag color={item.count > 0 ? "success" : "default"}>{item.count}</Tag>
                      </Col>
                    </Row>
                    <Paragraph type="secondary" style={{ marginBottom: 8 }}>
                      {item.detail}
                    </Paragraph>
                    <Progress percent={item.percent} status={item.percent > 0 ? "active" : "normal"} showInfo={false} />
                  </div>
                ))}
              </Space>
            ) : (
              <Alert type="info" showIcon message="暂无可展示运营指标" />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={8}>
          <Card
            title={
              <Space>
                <ApiOutlined />
                <span>接口状态</span>
              </Space>
            }
            extra={
              healthSummary.total > 0 ? (
                <Tag color={healthSummary.healthy === healthSummary.total ? "success" : "warning"}>
                  {healthSummary.healthy}/{healthSummary.total} 正常
                </Tag>
              ) : null
            }
          >
            {healthSummary.total > 0 ? (
              <>
                <Alert
                  type={healthSummary.healthy === healthSummary.total ? "success" : "warning"}
                  showIcon
                  message="仅检测当前账号有权访问的接口"
                  style={{ marginBottom: 16 }}
                />
                <Space direction="vertical" size={10} style={{ width: "100%" }}>
                  {Object.entries(apiStatus).map(([name, detail]) => (
                    <Row key={name} justify="space-between">
                      <Col>
                        <Text>{name}</Text>
                      </Col>
                      <Col>
                        <Tag color={detail.status === "success" ? "success" : "error"}>{detail.message}</Tag>
                      </Col>
                    </Row>
                  ))}
                </Space>
              </>
            ) : (
              <Alert type="info" showIcon message="当前没有可检测的业务接口" />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card title="快捷入口">
            {quickActions.length > 0 ? (
              <Space direction="vertical" size={10} style={{ width: "100%" }}>
                {quickActions.map((item) => (
                  <Button key={item.route} block onClick={() => navigate(item.route)}>
                    {item.label}
                  </Button>
                ))}
              </Space>
            ) : (
              <Alert type="info" showIcon message="暂无快捷入口" />
            )}
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card title="最近项目">
            <List
              dataSource={recentProjects}
              locale={{ emptyText: "当前无可见项目" }}
              renderItem={(item) => (
                <List.Item key={item.key}>
                  <Space direction="vertical" size={0}>
                    <Text strong>{item.title}</Text>
                    <Text type="secondary">{item.subtitle}</Text>
                    <Text type="secondary">{item.time}</Text>
                  </Space>
                </List.Item>
              )}
            />
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card title="最近客户">
            <List
              dataSource={recentCustomers}
              locale={{ emptyText: "当前无可见客户" }}
              renderItem={(item) => (
                <List.Item key={item.key}>
                  <Space direction="vertical" size={0}>
                    <Text strong>{item.title}</Text>
                    <Text type="secondary">{item.subtitle}</Text>
                    <Text type="secondary">{item.time}</Text>
                  </Space>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {permissions.canViewCustomers && (
          <Col xs={24} md={8}>
            <Card>
              <Statistic title="省级地区数" value={dashboard.provinces.length} prefix={<EnvironmentOutlined />} />
            </Card>
          </Col>
        )}
        {permissions.canViewInventory && (
          <Col xs={24} md={8}>
            <Card>
              <Statistic title="库存记录数" value={dashboard.inventoryRows.length} prefix={<DatabaseOutlined />} />
            </Card>
          </Col>
        )}
        {permissions.canViewLogs && (
          <Col xs={24} md={8}>
            <Card>
              <Statistic title="登录日志数" value={dashboard.loginLogs.length} prefix={<ApiOutlined />} />
            </Card>
          </Col>
        )}
      </Row>
    </Space>
  );
}
