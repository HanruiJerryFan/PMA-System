import { hasAnyAuthority } from "./authorities";
import { resolvePagePermissions } from "./pagePermissions";

describe("RBAC page permissions mapping", () => {
  test("distinguishes exact /inventory/warehouse route from /inventory/warehouses", () => {
    const docPermissions = resolvePagePermissions("/inventory/warehouse");
    expect(docPermissions.manageAuthorities).toEqual(["inventory.warehouse-doc.entry", "inventory.manage"]);
    expect(docPermissions.exportAuthorities).toEqual([
      "inventory.access",
      "inventory.manage",
      "inventory.warehouse-doc.entry",
      "inventory.warehouse-doc.audit",
    ]);

    const warehousePermissions = resolvePagePermissions("/inventory/warehouses");
    expect(warehousePermissions.manageAuthorities).toEqual(["inventory.manage"]);
    expect(warehousePermissions.exportAuthorities).toEqual(["inventory.access", "inventory.manage"]);
    expect(
      hasAnyAuthority({ authorities: ["inventory.warehouse-doc.entry"] }, warehousePermissions.manageAuthorities)
    ).toBe(false);
  });

  test("allows project list entry and audit authorities on /project/list-dashboard", () => {
    const dashboardPerms = resolvePagePermissions("/project/list-dashboard");
    expect(dashboardPerms.manageAuthorities).toEqual(["project.list.entry", "project.manage"]);
    expect(dashboardPerms.exportAuthorities).toEqual([
      "project.access",
      "project.manage",
      "project.list.entry",
      "project.list.audit",
    ]);
    expect(hasAnyAuthority({ authorities: ["project.list.audit"] }, dashboardPerms.exportAuthorities)).toBe(true);
    expect(hasAnyAuthority({ authorities: ["project.list.entry"] }, dashboardPerms.manageAuthorities)).toBe(true);
  });

  test("*.manage grants both manage and export permissions across core modules", () => {
    const cases = [
      ["/customer/info", "customer.manage"],
      ["/product/info", "product.manage"],
      ["/contract/info", "contract.manage"],
      ["/finance/vouchers", "finance.manage"],
      ["/attachment/center", "attachment.manage"],
      ["/logs/audit", "logs.manage"],
    ];

    cases.forEach(([pathname, manageAuthority]) => {
      const permissions = resolvePagePermissions(pathname);
      const user = { authorities: [manageAuthority] };
      expect(hasAnyAuthority(user, permissions.manageAuthorities)).toBe(true);
      expect(hasAnyAuthority(user, permissions.exportAuthorities)).toBe(true);
    });
  });
});
