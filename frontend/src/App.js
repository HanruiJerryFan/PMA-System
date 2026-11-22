import React, { useEffect } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import { heartbeat } from "./api/auth";
import { AuthProvider, useAuth } from "./context/AuthContext";
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
import ProjectTrackingBoard from "./pages/Project/ProjectTrackingBoard";
import ProjectMaterialReconciliation from "./pages/Project/ProjectMaterialReconciliation";
import ApprovalWorkbench from "./pages/Approval/ApprovalWorkbench";
import FinanceAnalytics from "./pages/Finance/FinanceAnalytics";
import AiAnalytics from "./pages/Analytics/AiAnalytics";
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
import FinanceSubjectManagement from "./pages/Finance/FinanceSubjectManagement";
import AttachmentCenter from "./pages/Attachment/AttachmentCenter";
import LoginLogs from "./pages/Logs/LoginLogs";
import AuditTrails from "./pages/Logs/AuditTrails";

function AppContent() {
  const { currentUser, loggedIn, loading, clearAuth } = useAuth();

  useEffect(() => {
    if (!loggedIn) {
      return undefined;
    }

    const heartbeatInterval = window.setInterval(() => {
      heartbeat()
        .catch((error) => {
          if (error.response?.status === 401) {
            clearAuth();
          }
        });
    }, 2 * 60 * 1000);

    return () => {
      window.clearInterval(heartbeatInterval);
    };
  }, [clearAuth, loggedIn]);

  if (loading) {
    return <div>加载中...</div>;
  }

  return (
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
              <Route
                path="/approvals/pending"
                element={
                  <PrivateRoute
                    requiredAuthorities={[
                      "project.list.audit",
                      "project.manage",
                      "inventory.warehouse-doc.audit",
                      "inventory.manage",
                      "finance.voucher.audit",
                      "finance.manage",
                    ]}
                  >
                    <ApprovalWorkbench />
                  </PrivateRoute>
                }
              />
              <Route
                path="/analytics/ai"
                element={
                  <PrivateRoute
                    requiredAuthorities={[
                      "project.access",
                      "project.manage",
                      "finance.access",
                      "finance.manage",
                      "inventory.access",
                      "inventory.manage",
                    ]}
                  >
                    <AiAnalytics />
                  </PrivateRoute>
                }
              />
              <Route path="/project/tracking-board" element={<PrivateRoute requiredAuthorities={["project.access", "project.manage"]}><ProjectTrackingBoard /></PrivateRoute>} />
              <Route path="/project/reconciliation" element={<PrivateRoute requiredAuthorities={["project.access", "project.manage"]}><ProjectMaterialReconciliation /></PrivateRoute>} />
              <Route path="/permission-denied" element={<PermissionDeniedPage />} />
              <Route path="/change-password" element={<ChangePasswordPage />} />
              <Route path="/permission/users" element={<PrivateRoute requiredAuthorities={["permission.users.manage"]}><UserManagement /></PrivateRoute>} />
              <Route path="/permission/roles" element={<PrivateRoute requiredAuthorities={["permission.roles.manage"]}><RoleManagement /></PrivateRoute>} />
              <Route path="/permission/permissions" element={<PrivateRoute requiredAuthorities={["permission.items.manage"]}><PermissionManagement /></PrivateRoute>} />
              <Route path="/permission/user-roles" element={<PrivateRoute requiredAuthorities={["permission.user-roles.manage"]}><UserRoleAssignments /></PrivateRoute>} />
              <Route path="/permission/role-permissions" element={<PrivateRoute requiredAuthorities={["permission.role-permissions.manage"]}><RolePermissionAssignments /></PrivateRoute>} />
              <Route path="/customer/info" element={<PrivateRoute requiredAuthorities={["customer.access", "customer.manage"]}><CustomerInfo /></PrivateRoute>} />
              <Route path="/customer/contacts" element={<PrivateRoute requiredAuthorities={["customer.access", "customer.manage"]}><CustomerContacts /></PrivateRoute>} />
              <Route path="/customer/regions" element={<PrivateRoute requiredAuthorities={["customer.access", "customer.manage"]}><RegionManagement /></PrivateRoute>} />
              <Route path="/customer/types" element={<PrivateRoute requiredAuthorities={["customer.manage"]}><CustomerTypes /></PrivateRoute>} />
              <Route path="/customer/industries" element={<PrivateRoute requiredAuthorities={["customer.manage"]}><CustomerIndustries /></PrivateRoute>} />
              <Route path="/customer/activity-rules" element={<PrivateRoute requiredAuthorities={["customer.manage"]}><CustomerActivityRules /></PrivateRoute>} />
              <Route path="/project/dashboard" element={<PrivateRoute requiredAuthorities={["project.access", "project.manage"]}><ProjectDashboard /></PrivateRoute>} />
              <Route path="/project/list-dashboard" element={<PrivateRoute requiredAuthorities={["project.access", "project.manage", "project.list.entry", "project.list.audit"]}><ProjectListDashboard /></PrivateRoute>} />
              <Route path="/project/info" element={<PrivateRoute requiredAuthorities={["project.access", "project.manage"]}><ProjectInfo /></PrivateRoute>} />
              <Route path="/project/types" element={<PrivateRoute requiredAuthorities={["project.manage"]}><ProjectTypes /></PrivateRoute>} />
              <Route path="/project/stages" element={<PrivateRoute requiredAuthorities={["project.manage"]}><StageManagement /></PrivateRoute>} />
              <Route path="/project/lists" element={<PrivateRoute requiredAuthorities={["project.access", "project.manage", "project.list.entry", "project.list.audit"]}><ProjectLists /></PrivateRoute>} />
              <Route path="/project/status-history" element={<PrivateRoute requiredAuthorities={["project.manage"]}><ProjectStatusHistory /></PrivateRoute>} />
              <Route path="/contract/info" element={<PrivateRoute requiredAuthorities={["contract.access", "contract.manage"]}><ContractInfo /></PrivateRoute>} />
              <Route path="/contract/clauses" element={<PrivateRoute requiredAuthorities={["contract.manage"]}><ContractClauses /></PrivateRoute>} />
              <Route path="/product/info" element={<PrivateRoute requiredAuthorities={["product.access", "product.manage"]}><ProductInfo /></PrivateRoute>} />
              <Route path="/product/categories" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductCategories /></PrivateRoute>} />
              <Route path="/product/brands" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductBrands /></PrivateRoute>} />
              <Route path="/product/subcategories" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductSubcategories /></PrivateRoute>} />
              <Route path="/product/bands" element={<PrivateRoute requiredAuthorities={["product.manage"]}><ProductBands /></PrivateRoute>} />
              <Route path="/product/prices" element={<PrivateRoute requiredAuthorities={["product.access", "product.manage"]}><ProductPrices /></PrivateRoute>} />
              <Route path="/inventory/stock" element={<PrivateRoute requiredAuthorities={["inventory.access", "inventory.manage"]}><InventoryStock /></PrivateRoute>} />
              <Route path="/inventory/transactions" element={<PrivateRoute requiredAuthorities={["inventory.access", "inventory.manage"]}><InventoryTransactions /></PrivateRoute>} />
              <Route path="/inventory/warehouses" element={<PrivateRoute requiredAuthorities={["inventory.manage"]}><Warehouses /></PrivateRoute>} />
              <Route path="/inventory/warehouse" element={<PrivateRoute requiredAuthorities={["inventory.access", "inventory.manage", "inventory.warehouse-doc.entry", "inventory.warehouse-doc.audit"]}><WarehouseDoc /></PrivateRoute>} />
              <Route path="/finance/analytics" element={<PrivateRoute requiredAuthorities={["finance.access", "finance.manage"]}><FinanceAnalytics /></PrivateRoute>} />
              <Route path="/finance/vouchers" element={<PrivateRoute requiredAuthorities={["finance.access", "finance.manage", "finance.voucher.entry", "finance.voucher.audit"]}><FinanceVouchers /></PrivateRoute>} />
              <Route path="/finance/tax-rates" element={<PrivateRoute requiredAuthorities={["finance.manage"]}><TaxRateManagement /></PrivateRoute>} />
              <Route path="/finance/subjects" element={<PrivateRoute requiredAuthorities={["finance.manage"]}><FinanceSubjectManagement /></PrivateRoute>} />
              <Route path="/attachment/center" element={<PrivateRoute requiredAuthorities={["attachment.access", "attachment.manage"]}><AttachmentCenter /></PrivateRoute>} />
              <Route path="/logs/login" element={<PrivateRoute requiredAuthorities={["logs.view", "logs.manage"]}><LoginLogs /></PrivateRoute>} />
              <Route path="/logs/audit" element={<PrivateRoute requiredAuthorities={["logs.view", "logs.manage"]}><AuditTrails /></PrivateRoute>} />
              <Route path="/" element={<Navigate to="/home" replace />} />
              <Route path="*" element={<Navigate to="/home" replace />} />
            </Routes>
          </MainLayout>
        ) : (
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        )}
      </BrowserRouter>
  );
}

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ConfigProvider>
  );
}

export default App;
