import React, { useEffect, useState } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import { getCurrentUser, heartbeat, sendLogoutBeacon } from "./api/auth";
import MainLayout from "./components/Layout/MainLayout";
import PrivateRoute from "./components/PrivateRoute";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import UserManagement from "./pages/Permission/UserManagement";
import RoleManagement from "./pages/Permission/RoleManagement";
import PermissionManagement from "./pages/Permission/PermissionManagement";
import UserRoleAssignments from "./pages/Permission/UserRoleAssignments";
import RolePermissionAssignments from "./pages/Permission/RolePermissionAssignments";
import PermissionDeniedPage from "./pages/PermissionDeniedPage";
import CustomerInfo from "./pages/Customer/CustomerInfo";
import CustomerContacts from "./pages/Customer/CustomerContacts";
import RegionManagement from "./pages/Customer/RegionManagement";
import CustomerTypes from "./pages/Customer/CustomerTypes";
import CustomerIndustries from "./pages/Customer/CustomerIndustries";
import CustomerActivityRules from "./pages/Customer/CustomerActivityRules";
import ProjectInfo from "./pages/Project/ProjectInfo";
import ProjectDashboard from "./pages/Project/ProjectDashboard";
import ProjectListDashboard from "./pages/Project/ProjectListDashboard";
import ProjectTypes from "./pages/Project/ProjectTypes";
import ProjectLists from "./pages/Project/ProjectLists";
import ProjectStatusHistory from "./pages/Project/ProjectStatusHistory";
import StageManagement from "./pages/Project/StageManagement";
import ContractInfo from "./pages/Contract/ContractInfo";
import ContractClauses from "./pages/Contract/ContractClauses";
import ProductInfo from "./pages/Product/ProductInfo";
import ProductCategories from "./pages/Product/ProductCategories";
import ProductBrands from "./pages/Product/ProductBrands";
import ProductSubcategories from "./pages/Product/ProductSubcategories";
import ProductBands from "./pages/Product/ProductBands";
import ProductPrices from "./pages/Product/ProductPrices";
import InventoryStock from "./pages/Inventory/InventoryStock";
import InventoryTransactions from "./pages/Inventory/InventoryTransactions";
import WarehouseDoc from "./pages/Inventory/WarehouseDoc";
import Warehouses from "./pages/Inventory/Warehouses";
import FinanceVouchers from "./pages/Finance/FinanceVouchers";
import TaxRateManagement from "./pages/Finance/TaxRateManagement";
import AttachmentCenter from "./pages/Attachment/AttachmentCenter";
import LoginLogs from "./pages/Logs/LoginLogs";
import AuditTrails from "./pages/Logs/AuditTrails";

function App() {
  const [loggedIn, setLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function checkLoginStatus() {
      try {
        const user = await getCurrentUser();
        setCurrentUser(user);
        setLoggedIn(Boolean(user));
      } catch (error) {
        setCurrentUser(null);
        setLoggedIn(false);
      } finally {
        setLoading(false);
      }
    }

    checkLoginStatus();
  }, []);

  const handleLoggedInChange = (nextLoggedIn, user = null) => {
    setLoggedIn(nextLoggedIn);
    setCurrentUser(nextLoggedIn ? user : null);
  };

  useEffect(() => {
    if (!loggedIn) {
      return undefined;
    }

    const heartbeatInterval = window.setInterval(() => {
      heartbeat()
        .then(() => getCurrentUser())
        .then((user) => {
          setCurrentUser(user);
          setLoggedIn(Boolean(user));
        })
        .catch(() => {});
    }, 2 * 60 * 1000);

    const handlePageHide = () => {
      sendLogoutBeacon();
    };

    window.addEventListener("pagehide", handlePageHide);
    window.addEventListener("beforeunload", handlePageHide);

    return () => {
      window.clearInterval(heartbeatInterval);
      window.removeEventListener("pagehide", handlePageHide);
      window.removeEventListener("beforeunload", handlePageHide);
    };
  }, [loggedIn]);

  if (loading) {
    return <div>加载中...</div>;
  }

  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        {loggedIn && currentUser?.forcePasswordChange ? (
          <Routes>
            <Route path="/change-password" element={<ChangePasswordPage forced />} />
            <Route path="*" element={<Navigate to="/change-password" replace />} />
          </Routes>
        ) : loggedIn ? (
          <MainLayout>
            <Routes>
              <Route path="/home" element={<HomePage />} />
              <Route path="/permission-denied" element={<PermissionDeniedPage />} />
              <Route path="/change-password" element={<ChangePasswordPage />} />
              <Route path="/permission/users" element={<PrivateRoute requiredAuthorities={["permission.users.manage"]}><UserManagement /></PrivateRoute>} />
              <Route path="/permission/roles" element={<PrivateRoute requiredAuthorities={["permission.roles.manage"]}><RoleManagement /></PrivateRoute>} />
              <Route path="/permission/permissions" element={<PrivateRoute requiredAuthorities={["permission.items.manage"]}><PermissionManagement /></PrivateRoute>} />
              <Route path="/permission/user-roles" element={<PrivateRoute requiredAuthorities={["permission.user-roles.manage"]}><UserRoleAssignments /></PrivateRoute>} />
              <Route path="/permission/role-permissions" element={<PrivateRoute requiredAuthorities={["permission.role-permissions.manage"]}><RolePermissionAssignments /></PrivateRoute>} />
              <Route path="/customer/info" element={<PrivateRoute requiredAuthorities={["customer.access"]}><CustomerInfo /></PrivateRoute>} />
              <Route path="/customer/contacts" element={<PrivateRoute requiredAuthorities={["customer.access"]}><CustomerContacts /></PrivateRoute>} />
              <Route path="/customer/regions" element={<PrivateRoute requiredAuthorities={["customer.access"]}><RegionManagement /></PrivateRoute>} />
              <Route path="/customer/types" element={<PrivateRoute requiredAuthorities={["customer.manage"]}><CustomerTypes /></PrivateRoute>} />
              <Route path="/customer/industries" element={<PrivateRoute requiredAuthorities={["customer.manage"]}><CustomerIndustries /></PrivateRoute>} />
              <Route path="/customer/activity-rules" element={<PrivateRoute requiredAuthorities={["customer.manage"]}><CustomerActivityRules /></PrivateRoute>} />
              <Route path="/project/dashboard" element={<PrivateRoute requiredAuthorities={["project.access"]}><ProjectDashboard /></PrivateRoute>} />
              <Route path="/project/list-dashboard" element={<PrivateRoute requiredAuthorities={["project.access"]}><ProjectListDashboard /></PrivateRoute>} />
              <Route path="/project/info" element={<PrivateRoute requiredAuthorities={["project.access"]}><ProjectInfo /></PrivateRoute>} />
              <Route path="/project/types" element={<PrivateRoute requiredAuthorities={["project.manage"]}><ProjectTypes /></PrivateRoute>} />
              <Route path="/project/stages" element={<PrivateRoute requiredAuthorities={["project.manage"]}><StageManagement /></PrivateRoute>} />
              <Route path="/project/lists" element={<PrivateRoute requiredAuthorities={["project.access"]}><ProjectLists /></PrivateRoute>} />
              <Route path="/project/status-history" element={<PrivateRoute requiredAuthorities={["project.manage"]}><ProjectStatusHistory /></PrivateRoute>} />
              <Route path="/contract/info" element={<PrivateRoute requiredAuthorities={["contract.access"]}><ContractInfo /></PrivateRoute>} />
              <Route path="/contract/clauses" element={<PrivateRoute requiredAuthorities={["contract.manage"]}><ContractClauses /></PrivateRoute>} />
              <Route path="/product/info" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductInfo /></PrivateRoute>} />
              <Route path="/product/categories" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductCategories /></PrivateRoute>} />
              <Route path="/product/brands" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductBrands /></PrivateRoute>} />
              <Route path="/product/subcategories" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductSubcategories /></PrivateRoute>} />
              <Route path="/product/bands" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductBands /></PrivateRoute>} />
              <Route path="/product/prices" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductPrices /></PrivateRoute>} />
              <Route path="/inventory/stock" element={<PrivateRoute requiredAuthorities={["inventory.access"]}><InventoryStock /></PrivateRoute>} />
              <Route path="/inventory/transactions" element={<PrivateRoute requiredAuthorities={["inventory.access"]}><InventoryTransactions /></PrivateRoute>} />
              <Route path="/inventory/warehouses" element={<PrivateRoute requiredAuthorities={["inventory.manage"]}><Warehouses /></PrivateRoute>} />
              <Route path="/inventory/warehouse" element={<PrivateRoute requiredAuthorities={["inventory.access"]}><WarehouseDoc /></PrivateRoute>} />
              <Route path="/finance/vouchers" element={<PrivateRoute requiredAuthorities={["finance.access"]}><FinanceVouchers /></PrivateRoute>} />
              <Route path="/finance/tax-rates" element={<PrivateRoute requiredAuthorities={["finance.manage"]}><TaxRateManagement /></PrivateRoute>} />
              <Route path="/attachment/center" element={<PrivateRoute requiredAuthorities={["attachment.access"]}><AttachmentCenter /></PrivateRoute>} />
              <Route path="/logs/login" element={<PrivateRoute requiredAuthorities={["logs.view"]}><LoginLogs /></PrivateRoute>} />
              <Route path="/logs/audit" element={<PrivateRoute requiredAuthorities={["logs.view"]}><AuditTrails /></PrivateRoute>} />
              <Route path="/" element={<Navigate to="/home" replace />} />
              <Route path="*" element={<Navigate to="/home" replace />} />
            </Routes>
          </MainLayout>
        ) : (
          <Routes>
            <Route path="/login" element={<LoginPage setLoggedIn={handleLoggedInChange} />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        )}
      </BrowserRouter>
    </ConfigProvider>
  );
}

export default App;
