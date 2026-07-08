import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Avatar, Button, Dropdown, Layout, Menu, Space, Tooltip, Typography } from "antd";
import {
  DatabaseOutlined,
  DollarOutlined,
  FileTextOutlined,
  HomeOutlined,
  LockOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ProjectOutlined,
  SafetyOutlined,
  ShoppingOutlined,
  TeamOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { getCurrentUser, logout } from "../../api/auth";
import { hasAnyAuthority } from "../../utils/authorities";

const { Sider } = Layout;
const { Text } = Typography;

export default function Sidebar() {
  const [currentUser, setCurrentUser] = useState(null);
  const [collapsed, setCollapsed] = useState(false);
  const [openKeys, setOpenKeys] = useState([]);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    async function fetchUser() {
      try {
        setCurrentUser(await getCurrentUser());
      } catch (error) {
        setCurrentUser(null);
      }
    }

    fetchUser();
  }, []);

  const displayName = useMemo(() => {
    if (!currentUser) {
      return "未登录用户";
    }
    if (typeof currentUser === "string") {
      const parts = currentUser.split(":");
      return parts[2] || currentUser;
    }
    return currentUser.realName || currentUser.username || "未命名用户";
  }, [currentUser]);

  const handleLogout = async () => {
    await logout();
    navigate("/login");
    window.location.reload();
  };

  const userMenuItems = [
    {
      key: "change-password",
      icon: <LockOutlined />,
      label: "修改密码",
      onClick: () => navigate("/change-password"),
    },
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "退出登录",
      onClick: handleLogout,
    },
  ];

  function stripPermissionFields(items = []) {
    return items.map(({ requiredAuthorities, onClick, children, label, key, ...item }) => {
      const hasChildren = Array.isArray(children) && children.length > 0;
      return {
        ...item,
        key,
        label:
          !hasChildren && typeof key === "string" && key.startsWith("/")
            ? (
                <Link to={key} style={{ display: "block", color: "inherit", textDecoration: "none" }}>
                  {label}
                </Link>
              )
            : label,
        ...(hasChildren ? { children: stripPermissionFields(children) } : {}),
      };
    });
  }

  const rawMenuItems = useMemo(
    () => [
      { key: "/home", icon: <HomeOutlined />, label: "首页", onClick: () => navigate("/home") },
      {
        key: "customer",
        icon: <TeamOutlined />,
        label: "客户管理",
        requiredAuthorities: ["customer.access"],
        children: [
          { key: "/customer/info", label: "客户信息", onClick: () => navigate("/customer/info"), requiredAuthorities: ["customer.access"] },
          { key: "/customer/contacts", label: "客户联系人", onClick: () => navigate("/customer/contacts"), requiredAuthorities: ["customer.access"] },
          { key: "/customer/types", label: "客户类型", onClick: () => navigate("/customer/types"), requiredAuthorities: ["customer.manage"] },
          { key: "/customer/industries", label: "行业字典", onClick: () => navigate("/customer/industries"), requiredAuthorities: ["customer.manage"] },
          { key: "/customer/activity-rules", label: "客户活跃规则", onClick: () => navigate("/customer/activity-rules"), requiredAuthorities: ["customer.manage"] },
          { key: "/customer/regions", label: "地区查找", onClick: () => navigate("/customer/regions"), requiredAuthorities: ["customer.access"] },
        ],
      },
      {
        key: "project",
        icon: <ProjectOutlined />,
        label: "项目管理",
        requiredAuthorities: ["project.access"],
        children: [
          { key: "/project/info", label: "项目信息", onClick: () => navigate("/project/info"), requiredAuthorities: ["project.access"] },
          { key: "/project/status-history", label: "项目记录", onClick: () => navigate("/project/status-history"), requiredAuthorities: ["project.manage"] },
          { key: "/project/lists", label: "项目清单", onClick: () => navigate("/project/lists"), requiredAuthorities: ["project.access", "project.list.entry", "project.list.audit"] },
          { key: "/project/types", label: "项目类型", onClick: () => navigate("/project/types"), requiredAuthorities: ["project.manage"] },
          { key: "/project/stages", label: "项目阶段", onClick: () => navigate("/project/stages"), requiredAuthorities: ["project.manage"] },
        ],
      },
      {
        key: "contract",
        icon: <FileTextOutlined />,
        label: "合同管理",
        requiredAuthorities: ["contract.access"],
        children: [
          { key: "/contract/info", label: "合同信息", onClick: () => navigate("/contract/info"), requiredAuthorities: ["contract.access"] },
          { key: "/contract/clauses", label: "合同条款", onClick: () => navigate("/contract/clauses"), requiredAuthorities: ["contract.manage"] },
        ],
      },
      {
        key: "product",
        icon: <ShoppingOutlined />,
        label: "物料管理",
        requiredAuthorities: ["product.access"],
        children: [
          { key: "/product/info", label: "物料主数据", onClick: () => navigate("/product/info"), requiredAuthorities: ["product.manage"] },
          { key: "/product/prices", label: "价格管理", onClick: () => navigate("/product/prices"), requiredAuthorities: ["product.manage"] },
          { key: "/product/categories", label: "物料类别", onClick: () => navigate("/product/categories"), requiredAuthorities: ["product.manage"] },
          { key: "/product/subcategories", label: "物料分项", onClick: () => navigate("/product/subcategories"), requiredAuthorities: ["product.manage"] },
          { key: "/product/brands", label: "品牌字典", onClick: () => navigate("/product/brands"), requiredAuthorities: ["product.manage"] },
          { key: "/product/bands", label: "频段字典", onClick: () => navigate("/product/bands"), requiredAuthorities: ["product.manage"] },
        ],
      },
      {
        key: "inventory",
        icon: <DatabaseOutlined />,
        label: "库存管理",
        requiredAuthorities: ["inventory.access"],
        children: [
          { key: "/inventory/warehouses", label: "仓库管理", onClick: () => navigate("/inventory/warehouses"), requiredAuthorities: ["inventory.manage"] },
          { key: "/inventory/stock", label: "库存汇总", onClick: () => navigate("/inventory/stock"), requiredAuthorities: ["inventory.access"] },
          { key: "/inventory/transactions", label: "库存流水", onClick: () => navigate("/inventory/transactions"), requiredAuthorities: ["inventory.access"] },
          { key: "/inventory/warehouse", label: "出入库单", onClick: () => navigate("/inventory/warehouse"), requiredAuthorities: ["inventory.access", "inventory.warehouse-doc.entry", "inventory.warehouse-doc.audit"] },
        ],
      },
      {
        key: "finance",
        icon: <DollarOutlined />,
        label: "财务管理",
        requiredAuthorities: ["finance.access"],
        children: [
          { key: "/finance/vouchers", label: "财务凭证", onClick: () => navigate("/finance/vouchers"), requiredAuthorities: ["finance.access", "finance.voucher.entry", "finance.voucher.audit"] },
          { key: "/finance/tax-rates", label: "税率字典", onClick: () => navigate("/finance/tax-rates"), requiredAuthorities: ["finance.manage"] },
        ],
      },
      {
        key: "attachment",
        icon: <FileTextOutlined />,
        label: "附件管理",
        requiredAuthorities: ["attachment.access"],
        children: [
          { key: "/attachment/center", label: "附件中心", onClick: () => navigate("/attachment/center"), requiredAuthorities: ["attachment.access"] },
        ],
      },
      {
        key: "logs",
        icon: <FileTextOutlined />,
        label: "日志管理",
        requiredAuthorities: ["logs.view"],
        children: [
          { key: "/logs/login", label: "登录日志", onClick: () => navigate("/logs/login"), requiredAuthorities: ["logs.view"] },
          { key: "/logs/audit", label: "审计日志", onClick: () => navigate("/logs/audit"), requiredAuthorities: ["logs.view"] },
        ],
      },
      {
        key: "permission",
        icon: <SafetyOutlined />,
        label: "权限管理",
        requiredAuthorities: [
          "permission.users.manage",
          "permission.roles.manage",
          "permission.items.manage",
          "permission.user-roles.manage",
          "permission.role-permissions.manage",
        ],
        children: [
          { key: "/permission/users", label: "用户管理", onClick: () => navigate("/permission/users"), requiredAuthorities: ["permission.users.manage"] },
          { key: "/permission/roles", label: "角色管理", onClick: () => navigate("/permission/roles"), requiredAuthorities: ["permission.roles.manage"] },
          { key: "/permission/permissions", label: "权限项管理", onClick: () => navigate("/permission/permissions"), requiredAuthorities: ["permission.items.manage"] },
          { key: "/permission/user-roles", label: "用户角色分配", onClick: () => navigate("/permission/user-roles"), requiredAuthorities: ["permission.user-roles.manage"] },
          { key: "/permission/role-permissions", label: "角色权限分配", onClick: () => navigate("/permission/role-permissions"), requiredAuthorities: ["permission.role-permissions.manage"] },
        ],
      },
    ],
    [navigate]
  );

  const menuItems = useMemo(
    () =>
      rawMenuItems
        .map((item) => {
          if (Array.isArray(item.children)) {
            const visibleChildren = item.children.filter((child) => hasAnyAuthority(currentUser, child.requiredAuthorities));
            if (visibleChildren.length === 0) {
              return null;
            }
            return { ...item, children: visibleChildren };
          }
          return hasAnyAuthority(currentUser, item.requiredAuthorities) ? item : null;
        })
        .filter(Boolean),
    [currentUser, rawMenuItems]
  );

  const rootMenuKeys = useMemo(
    () => menuItems.filter((item) => Array.isArray(item.children) && item.children.length > 0).map((item) => item.key),
    [menuItems]
  );

  useEffect(() => {
    if (collapsed) {
      setOpenKeys((current) => (current.length === 0 ? current : []));
      return;
    }

    const activeParent = menuItems.find(
      (item) => Array.isArray(item.children) && item.children.some((child) => child.key === location.pathname)
    );

    if (activeParent) {
      setOpenKeys((current) => (current.includes(activeParent.key) ? current : [...current, activeParent.key]));
    }
  }, [collapsed, location.pathname, menuItems]);

  const normalizedMenuItems = stripPermissionFields(menuItems);
  const menuOpenKeys = collapsed ? undefined : openKeys;

  const handleMenuOpenChange = (keys) => {
    if (!collapsed) {
      setOpenKeys(keys);
    }
  };

  const toggleAllSubmenus = () => {
    if (openKeys.length === rootMenuKeys.length) {
      setOpenKeys([]);
    } else {
      setOpenKeys(rootMenuKeys);
    }
  };

  return (
    <Sider
      className="app-sidebar"
      collapsible
      collapsed={collapsed}
      onCollapse={setCollapsed}
      width={250}
      collapsedWidth={72}
      style={{
        height: "100vh",
        position: "fixed",
        left: 0,
        top: 0,
        bottom: 0,
        zIndex: 1000,
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          padding: 16,
          textAlign: "center",
          borderBottom: "1px solid rgba(255,255,255,0.08)",
          backgroundColor: "#001529",
        }}
      >
        {!collapsed ? (
          <Space direction="vertical" size="small" style={{ width: "100%" }}>
            <Text style={{ color: "#fff", fontSize: 16, fontWeight: 700 }}>销售管理系统</Text>
            <Space style={{ justifyContent: "space-between", width: "100%" }}>
              <Dropdown menu={{ items: userMenuItems }} placement="bottom">
                <Space style={{ cursor: "pointer" }}>
                  <Avatar icon={<UserOutlined />} />
                  <Text style={{ color: "#fff", fontSize: 12 }}>{displayName}</Text>
                </Space>
              </Dropdown>
              <Tooltip title={openKeys.length === rootMenuKeys.length ? "全部折叠" : "全部展开"}>
                <Button
                  type="text"
                  size="small"
                  icon={openKeys.length === rootMenuKeys.length ? <MenuFoldOutlined /> : <MenuUnfoldOutlined />}
                  onClick={toggleAllSubmenus}
                  style={{ color: "#fff" }}
                />
              </Tooltip>
            </Space>
          </Space>
        ) : (
          <Dropdown menu={{ items: userMenuItems }} placement="rightTop">
            <Avatar icon={<UserOutlined />} style={{ cursor: "pointer" }} />
          </Dropdown>
        )}
      </div>

      <div
        className="app-sidebar-menu-scroll"
        style={{
          flex: 1,
          minHeight: 0,
          overflowY: "auto",
          overflowX: "hidden",
        }}
      >
        <Menu
          theme="dark"
          mode="inline"
          triggerSubMenuAction="hover"
          selectedKeys={[location.pathname]}
          openKeys={menuOpenKeys}
          onOpenChange={handleMenuOpenChange}
          items={normalizedMenuItems}
          style={{ borderRight: 0 }}
        />
      </div>
    </Sider>
  );
}
