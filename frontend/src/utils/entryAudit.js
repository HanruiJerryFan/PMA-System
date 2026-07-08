import { hasAnyAuthority } from "./authorities";

export const ENTRY_AUDIT_ADMIN_AUTHORITIES = [
  "document.entry-audit.manage",
  "ROLE_System",
  "ROLE_管理员",
];

export function canMaintainEntryAuditUsers(user) {
  return hasAnyAuthority(user, ENTRY_AUDIT_ADMIN_AUTHORITIES);
}

export function getUserLabel(user) {
  return user?.realName || user?.username || "";
}
