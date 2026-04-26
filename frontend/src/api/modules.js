import api from "./index";

export const permissionAPI = {
  getUsers: () => api.get("/users").then((response) => response?.data ?? response),
  getUserOptions: () => api.get("/users/options").then((response) => response?.data ?? response),
  createUser: (data) => api.post("/users", data),
  updateUser: (id, data) => api.put(`/users/${id}`, data),
  resetUserPassword: (id, operatorId) =>
    api.post(`/users/${id}/reset-password`, null, { params: { operatorId } }).then((response) => response?.data ?? response),
  deleteUser: (id) => api.delete(`/users/${id}`),

  getRoles: () => api.get("/roles").then((response) => response?.data ?? response),
  createRole: (data) => api.post("/roles", data),
  updateRole: (id, data) => api.put(`/roles/${id}`, data),
  deleteRole: (id) => api.delete(`/roles/${id}`),

  getPermissions: () => api.get("/permissions").then((response) => response?.data ?? response),
  createPermission: (data) => api.post("/permissions", data),
  updatePermission: (id, data) => api.put(`/permissions/${id}`, data),
  deletePermission: (id) => api.delete(`/permissions/${id}`),

  getUserRoles: (userId) => api.get(`/sysuserrole/byUser/${userId}`).then((response) => response?.data ?? response),
  assignUserRole: (data) => api.post("/sysuserrole", data),
  removeUserRole: (userId, roleId) => api.delete("/sysuserrole", { params: { userId, roleId } }),

  getRolePermissions: (roleId) =>
    api.get(`/sysrolepermission/byRole/${roleId}`).then((response) => response?.data ?? response),
  assignRolePermission: (data) => api.post("/sysrolepermission", data),
  removeRolePermission: (roleId, permissionId) =>
    api.delete("/sysrolepermission", { params: { roleId, permissionId } }),
};

export const customerAPI = {
  getCustomers: () => api.get("/customers"),
  getCustomerOptions: () => api.get("/customers/options"),
  createCustomer: (data) => api.post("/customers", data),
  updateCustomer: (uuid, data) => api.put(`/customers/${uuid}`, data),
  deleteCustomer: (uuid) => api.delete(`/customers/${uuid}`),

  getCustomerTypes: () => api.get("/customer-types"),
  getCustomerTypeOptions: () => api.get("/customer-types/options"),
  getCustomerIndustryDicts: () => api.get("/customer-industry-dicts"),
  createCustomerIndustryDict: (data) => api.post("/customer-industry-dicts", data),
  updateCustomerIndustryDict: (id, data) => api.put(`/customer-industry-dicts/${id}`, data),
  deleteCustomerIndustryDict: (id) => api.delete(`/customer-industry-dicts/${id}`),
  getCustomerActivityRules: () => api.get("/customer-activity-rules"),
  createCustomerActivityRule: (data) => api.post("/customer-activity-rules", data),
  updateCustomerActivityRule: (id, data) => api.put(`/customer-activity-rules/${id}`, data),
  deleteCustomerActivityRule: (id) => api.delete(`/customer-activity-rules/${id}`),
  refreshCustomerActivityStatuses: () => api.post("/customer-activity-rules/refresh"),

  getCustomerContacts: () => api.get("/customer-contacts"),
  getCustomerContactOptions: () => api.get("/customer-contacts/options"),
  createCustomerContact: (data) => api.post("/customer-contacts", data),
  updateCustomerContact: (uuid, data) => api.put(`/customer-contacts/${uuid}`, data),
  deleteCustomerContact: (uuid) => api.delete(`/customer-contacts/${uuid}`),
};

export const projectAPI = {
  getProjects: () => api.get("/projects"),
  getProjectOptions: () => api.get("/projects/options"),
  createProject: (data) => api.post("/projects", data),
  updateProject: (uuid, data) => api.put(`/projects/${uuid}`, data),
  deleteProject: (uuid) => api.delete(`/projects/${uuid}`),

  getProjectLists: () => api.get("/project-lists"),
  getProjectListOptions: () => api.get("/project-lists/options"),
  getProjectListsByProject: (projectId) => api.get(`/project-lists/byProject/${projectId}`),
  getProjectListAggregate: (projectId, aggregateType) =>
    api.get(`/project-lists/aggregate/${projectId}`, { params: { aggregateType } }),
  createProjectList: (data) => api.post("/project-lists", data),
  updateProjectList: (uuid, data) => api.put(`/project-lists/${uuid}`, data),
  deleteProjectList: (uuid) => api.delete(`/project-lists/${uuid}`),

  getProjectListItems: (projectListId) => api.get(`/project-list-items/byList/${projectListId}`),
  createProjectListItem: (data) => api.post("/project-list-items", data),
  updateProjectListItem: (uuid, data) => api.put(`/project-list-items/${uuid}`, data),
  deleteProjectListItem: (uuid) => api.delete(`/project-list-items/${uuid}`),
  downloadProjectListItemsImportTemplate: () =>
    api.get("/project-list-items/import-template", {
      responseType: "blob",
    }),
  importProjectListItems: (projectListId, formData, replaceExisting = false, importMode = "PARTIAL_SUCCESS") =>
    api.post(`/project-list-items/import/${projectListId}`, formData, {
      params: { replaceExisting, importMode },
      headers: { "Content-Type": "multipart/form-data" },
    }),

  getProjectStatusHistory: (projectId) =>
    api.get("/project-status-history", { params: projectId ? { projectId } : undefined }),
  getProjectStatusHistoryItem: (uuid) => api.get(`/project-status-history/${uuid}`),
  createProjectStatusHistory: (data) => api.post("/project-status-history", data),
  updateProjectStatusHistory: (uuid, data) => api.put(`/project-status-history/${uuid}`, data),
  deleteProjectStatusHistory: (uuid) => api.delete(`/project-status-history/${uuid}`),

  getProjectTypes: () => api.get("/project-types"),
  getProjectType: (id) => api.get(`/project-types/${id}`),
  createProjectType: (data) => api.post("/project-types", data),
  updateProjectType: (id, data) => api.put(`/project-types/${id}`, data),
  deleteProjectType: (id) => api.delete(`/project-types/${id}`),
  getStages: () => api.get("/project-stages"),
  getStage: (id) => api.get(`/project-stages/${id}`),
  createStage: (data) => api.post("/project-stages", data),
  updateStage: (id, data) => api.put(`/project-stages/${id}`, data),
  deleteStage: (id) => api.delete(`/project-stages/${id}`),
};

export const contractAPI = {
  getContracts: () => api.get("/contracts"),
  getContractOptions: () => api.get("/contracts/options"),
  createContract: (data) => api.post("/contracts", data),
  updateContract: (uuid, data) => api.put(`/contracts/${uuid}`, data),
  deleteContract: (uuid) => api.delete(`/contracts/${uuid}`),

  getContractTypes: () => api.get("/contract-types"),
  getContractClauses: (contractUuid) => api.get(`/contract-clauses/byContract/${contractUuid}`),
  createContractClause: (data) => api.post("/contract-clauses", data),
  updateContractClause: (uuid, data) => api.put(`/contract-clauses/${uuid}`, data),
  deleteContractClause: (uuid) => api.delete(`/contract-clauses/${uuid}`),

  getClauseTypes: () => api.get("/clausetype"),
};

export const productAPI = {
  getProducts: () => api.get("/materials"),
  getProductOptions: () => api.get("/materials/options"),
  createProduct: (data) => api.post("/materials", data),
  updateProduct: (uuid, data) => api.put(`/materials/${uuid}`, data),
  deleteProduct: (uuid) => api.delete(`/materials/${uuid}`),
  downloadImportTemplate: () =>
    api.get("/materials/import-template", {
      responseType: "blob",
    }),
  importProducts: (formData, importMode = "PARTIAL_SUCCESS") =>
    api.post("/materials/import", formData, {
      params: { importMode },
      headers: { "Content-Type": "multipart/form-data" },
    }),

  getProductCategories: () => api.get("/material-category-dicts"),
  getProductCategoryOptions: () => api.get("/material-category-dicts/options"),
  createProductCategory: (data) => api.post("/material-category-dicts", data),
  updateProductCategory: (id, data) => api.put(`/material-category-dicts/${id}`, data),
  deleteProductCategory: (id) => api.delete(`/material-category-dicts/${id}`),

  getProductBrands: () => api.get("/material-brand-dicts"),
  getProductBrandOptions: () => api.get("/material-brand-dicts/options"),
  createProductBrand: (data) => api.post("/material-brand-dicts", data),
  updateProductBrand: (id, data) => api.put(`/material-brand-dicts/${id}`, data),
  deleteProductBrand: (id) => api.delete(`/material-brand-dicts/${id}`),

  getProductSubcategories: () => api.get("/material-subcategory-dicts"),
  getProductSubcategoryOptions: () => api.get("/material-subcategory-dicts/options"),
  createProductSubcategory: (data) => api.post("/material-subcategory-dicts", data),
  updateProductSubcategory: (id, data) => api.put(`/material-subcategory-dicts/${id}`, data),
  deleteProductSubcategory: (id) => api.delete(`/material-subcategory-dicts/${id}`),

  getProductBands: () => api.get("/material-band-dicts"),
  createProductBand: (data) => api.post("/material-band-dicts", data),
  updateProductBand: (id, data) => api.put(`/material-band-dicts/${id}`, data),
  deleteProductBand: (id) => api.delete(`/material-band-dicts/${id}`),

  getAllProductPrices: () => api.get("/product-prices"),
  getProductPrices: (productUuid) => api.get(`/product-prices/byProduct/${productUuid}`),
  getProductPrice: (id) => api.get(`/product-prices/${id}`),
  createProductPrice: (data) => api.post("/product-prices", data),
  updateProductPrice: (id, data) => api.put(`/product-prices/${id}`, data),
  deleteProductPrice: (id) => api.delete(`/product-prices/${id}`),
};

export const inventoryAPI = {
  getWarehouses: () => api.get("/warehouses"),
  createWarehouse: (data) => api.post("/warehouses", data),
  updateWarehouse: (id, data) => api.put(`/warehouses/${id}`, data),
  deleteWarehouse: (id) => api.delete(`/warehouses/${id}`),

  getInventory: () => api.get("/inventory-stock"),

  getTransactions: () => api.get("/inventory-transactions"),
  createTransaction: (data) => api.post("/inventory-transactions", data),
  updateTransaction: (uuid, data) => api.put(`/inventory-transactions/${uuid}`, data),
  deleteTransaction: (uuid) => api.delete(`/inventory-transactions/${uuid}`),

  getWarehouseDocs: () => api.get("/warehouse-documents"),
  getWarehouseDocOptions: () => api.get("/warehouse-documents/options"),
  getWarehouseDocMaterialOptions: () => api.get("/warehouse-documents/material-options"),
  getWarehouseDocProjectOptions: () => api.get("/warehouse-documents/project-options"),
  getWarehouseDocCustomerOptions: () => api.get("/warehouse-documents/customer-options"),
  createWarehouseDoc: (data) => api.post("/warehouse-documents", data),
  updateWarehouseDoc: (docNumber, data) => api.put(`/warehouse-documents/${docNumber}`, data),
  deleteWarehouseDoc: (docNumber) => api.delete(`/warehouse-documents/${docNumber}`),
  downloadWarehouseDocPdf: (docNumber) =>
    api.get(`/warehouse-documents/${docNumber}/export-pdf`, {
      responseType: "blob",
    }),
};

export const financeAPI = {
  getFinanceVouchers: () => api.get("/finance-vouchers"),
  getFinanceVoucherOptions: () => api.get("/finance-vouchers/options"),
  getFinanceVoucher: (uuid) => api.get(`/finance-vouchers/${uuid}`),
  createFinanceVoucher: (data) => api.post("/finance-vouchers", data),
  updateFinanceVoucher: (uuid, data) => api.put(`/finance-vouchers/${uuid}`, data),
  deleteFinanceVoucher: (uuid) => api.delete(`/finance-vouchers/${uuid}`),
  getTaxRateDicts: () => api.get("/tax-rate-dicts"),
  createTaxRateDict: (data) => api.post("/tax-rate-dicts", data),
  updateTaxRateDict: (id, data) => api.put(`/tax-rate-dicts/${id}`, data),
  deleteTaxRateDict: (id) => api.delete(`/tax-rate-dicts/${id}`),
};

export const attachmentAPI = {
  getAttachments: () => api.get("/attachments"),
  getAttachment: (uuid) => api.get(`/attachments/${uuid}`),
  getAttachmentsByBusiness: (businessType, businessUuid) =>
    api.get("/attachments/byBusiness", { params: { businessType, businessUuid } }),
  uploadAttachment: (formData) =>
    api.post("/attachments/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),
  downloadAttachment: (uuid) =>
    api.get(`/attachments/download/${uuid}`, {
      responseType: "blob",
    }),
  createAttachment: (data) => api.post("/attachments", data),
  updateAttachment: (uuid, data) => api.put(`/attachments/${uuid}`, data),
  deleteAttachment: (uuid) => api.delete(`/attachments/${uuid}`),
};

export const regionAPI = {
  getRegions: () => api.get("/region"),
  getRegionOptions: () => api.get("/region/options"),
  getProvinces: () => api.get("/provinces"),
  getChildrenByParentCode: (parentCode) => api.get(`/region/byParentCode/${parentCode}`),
  getCitiesByProvinceCode: (provinceAreaCode) => api.get("/cities", { params: { provinceAreaCode } }),
  getDistrictsByCityCode: (cityAreaCode) => api.get(`/region/districts/${cityAreaCode}`),
  getCities: (provinceAreaCode) => api.get("/cities", { params: { provinceAreaCode } }),
  getDistricts: (cityAreaCode) => api.get(`/region/districts/${cityAreaCode}`),
};

export const logAPI = {
  getLoginRecords: (params) => api.get("/login-logs", { params }),
  getLoginRecord: (id) => api.get(`/login-logs/${id}`),
  getAuditTrails: (params) => api.get("/audit-trails", { params }),
  getAuditTrail: (id) => api.get(`/audit-trails/${id}`),
};

export const exportAPI = {
  downloadTablePdf: (data) =>
    api.post("/pdf-exports/table", data, {
      responseType: "blob",
    }),
};
