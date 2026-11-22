export function resolvePagePermissions(pathname = "") {
  if (pathname.startsWith("/permission/users")) {
    return { manageAuthorities: ["permission.users.manage"], exportAuthorities: [] };
  }
  if (pathname.startsWith("/permission/roles")) {
    return { manageAuthorities: ["permission.roles.manage"], exportAuthorities: [] };
  }
  if (pathname.startsWith("/permission/permissions")) {
    return { manageAuthorities: ["permission.items.manage"], exportAuthorities: [] };
  }
  if (pathname.startsWith("/permission/user-roles")) {
    return { manageAuthorities: ["permission.user-roles.manage"], exportAuthorities: [] };
  }
  if (pathname.startsWith("/permission/role-permissions")) {
    return { manageAuthorities: ["permission.role-permissions.manage"], exportAuthorities: [] };
  }
  if (pathname.startsWith("/customer")) {
    return { manageAuthorities: ["customer.manage"], exportAuthorities: ["customer.access", "customer.manage"] };
  }
  if (pathname.startsWith("/project/lists") || pathname.startsWith("/project/list-dashboard")) {
    return {
      manageAuthorities: ["project.list.entry", "project.manage"],
      exportAuthorities: ["project.access", "project.manage", "project.list.entry", "project.list.audit"],
    };
  }
  if (pathname.startsWith("/project")) {
    return { manageAuthorities: ["project.manage"], exportAuthorities: ["project.access", "project.manage"] };
  }
  if (pathname.startsWith("/contract")) {
    return { manageAuthorities: ["contract.manage"], exportAuthorities: ["contract.access", "contract.manage"] };
  }
  if (pathname.startsWith("/product")) {
    return { manageAuthorities: ["product.manage"], exportAuthorities: ["product.access", "product.manage"] };
  }
  if (pathname === "/inventory/warehouse" || pathname.startsWith("/inventory/warehouse/")) {
    return {
      manageAuthorities: ["inventory.warehouse-doc.entry", "inventory.manage"],
      exportAuthorities: [
        "inventory.access",
        "inventory.manage",
        "inventory.warehouse-doc.entry",
        "inventory.warehouse-doc.audit",
      ],
    };
  }
  if (pathname.startsWith("/inventory")) {
    return { manageAuthorities: ["inventory.manage"], exportAuthorities: ["inventory.access", "inventory.manage"] };
  }
  if (pathname.startsWith("/finance/vouchers")) {
    return {
      manageAuthorities: ["finance.voucher.entry", "finance.manage"],
      exportAuthorities: [
        "finance.access",
        "finance.manage",
        "finance.voucher.entry",
        "finance.voucher.audit",
      ],
    };
  }
  if (pathname.startsWith("/finance")) {
    return { manageAuthorities: ["finance.manage"], exportAuthorities: ["finance.access", "finance.manage"] };
  }
  if (pathname.startsWith("/attachment")) {
    return { manageAuthorities: ["attachment.manage"], exportAuthorities: ["attachment.access", "attachment.manage"] };
  }
  if (pathname.startsWith("/logs")) {
    return { manageAuthorities: ["logs.manage"], exportAuthorities: ["logs.view", "logs.manage"] };
  }
  if (pathname.startsWith("/approvals")) {
    return {
      manageAuthorities: [
        "project.list.audit",
        "project.manage",
        "inventory.warehouse-doc.audit",
        "inventory.manage",
        "finance.voucher.audit",
        "finance.manage",
      ],
      exportAuthorities: [],
    };
  }
  return { manageAuthorities: [], exportAuthorities: [] };
}
