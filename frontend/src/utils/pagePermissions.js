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
    return { manageAuthorities: ["customer.manage"], exportAuthorities: ["customer.access"] };
  }
  if (pathname.startsWith("/project")) {
    return { manageAuthorities: ["project.manage"], exportAuthorities: ["project.access"] };
  }
  if (pathname.startsWith("/contract")) {
    return { manageAuthorities: ["contract.manage"], exportAuthorities: ["contract.access"] };
  }
  if (pathname.startsWith("/product")) {
    return { manageAuthorities: ["product.manage"], exportAuthorities: ["product.access"] };
  }
  if (pathname.startsWith("/inventory")) {
    return { manageAuthorities: ["inventory.manage"], exportAuthorities: ["inventory.access"] };
  }
  if (pathname.startsWith("/finance")) {
    return { manageAuthorities: ["finance.manage"], exportAuthorities: ["finance.access"] };
  }
  if (pathname.startsWith("/attachment")) {
    return { manageAuthorities: ["attachment.manage"], exportAuthorities: ["attachment.access"] };
  }
  if (pathname.startsWith("/logs")) {
    return { manageAuthorities: ["logs.manage"], exportAuthorities: ["logs.view"] };
  }
  return { manageAuthorities: [], exportAuthorities: [] };
}
