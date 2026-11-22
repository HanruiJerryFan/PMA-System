import { resolvePagePermissions } from "./pagePermissions";
import { canMaintainEntryAuditUsers, isSelfEntryRecord } from "./entryAudit";

describe("RBAC permissions helpers", () => {
  test("resolvePagePermissions grants *.manage and *.entry/*.audit export/manage authorities consistently", () => {
    expect(resolvePagePermissions("/customer/info").exportAuthorities).toEqual([
      "customer.access",
      "customer.manage",
    ]);
    expect(resolvePagePermissions("/project/info").exportAuthorities).toEqual([
      "project.access",
      "project.manage",
    ]);
    expect(resolvePagePermissions("/project/lists")).toEqual({
      manageAuthorities: ["project.list.entry", "project.manage"],
      exportAuthorities: ["project.access", "project.manage", "project.list.entry", "project.list.audit"],
    });
    expect(resolvePagePermissions("/contract/info").exportAuthorities).toEqual([
      "contract.access",
      "contract.manage",
    ]);
    expect(resolvePagePermissions("/product/info").exportAuthorities).toEqual([
      "product.access",
      "product.manage",
    ]);
    expect(resolvePagePermissions("/inventory/stock").exportAuthorities).toEqual([
      "inventory.access",
      "inventory.manage",
    ]);
    expect(resolvePagePermissions("/inventory/warehouse")).toEqual({
      manageAuthorities: ["inventory.warehouse-doc.entry", "inventory.manage"],
      exportAuthorities: [
        "inventory.access",
        "inventory.manage",
        "inventory.warehouse-doc.entry",
        "inventory.warehouse-doc.audit",
      ],
    });
    expect(resolvePagePermissions("/finance/analytics").exportAuthorities).toEqual([
      "finance.access",
      "finance.manage",
    ]);
    expect(resolvePagePermissions("/finance/vouchers")).toEqual({
      manageAuthorities: ["finance.voucher.entry", "finance.manage"],
      exportAuthorities: [
        "finance.access",
        "finance.manage",
        "finance.voucher.entry",
        "finance.voucher.audit",
      ],
    });
    expect(resolvePagePermissions("/attachment/center").exportAuthorities).toEqual([
      "attachment.access",
      "attachment.manage",
    ]);
    expect(resolvePagePermissions("/logs/login").exportAuthorities).toEqual([
      "logs.view",
      "logs.manage",
    ]);
  });

  test("canMaintainEntryAuditUsers respects explicit authority and System user without role bypass", () => {
    expect(canMaintainEntryAuditUsers({ username: "System", authorities: [] })).toBe(true);
    expect(canMaintainEntryAuditUsers({ username: "admin", authorities: ["document.entry-audit.manage"] })).toBe(true);
    expect(canMaintainEntryAuditUsers({ username: "admin", authorities: ["ROLE_管理员"] })).toBe(false);
    expect(canMaintainEntryAuditUsers({ username: "auditor", authorities: ["finance.voucher.audit"] })).toBe(false);
  });

  test("isSelfEntryRecord detects matching entryUser id", () => {
    expect(isSelfEntryRecord({ id: 7 }, { entryUser: 7 })).toBe(true);
    expect(isSelfEntryRecord({ id: "7" }, { entryUser: 7 })).toBe(true);
    expect(isSelfEntryRecord({ id: 7 }, { entryUser: 8 })).toBe(false);
    expect(isSelfEntryRecord({ id: 7 }, { entryUser: null })).toBe(false);
    expect(isSelfEntryRecord(null, { entryUser: 7 })).toBe(false);
  });
});
