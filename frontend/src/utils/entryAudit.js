import { hasAnyAuthority } from "./authorities";

export const ENTRY_AUDIT_ADMIN_AUTHORITIES = [
  "document.entry-audit.manage",
];

export function canMaintainEntryAuditUsers(user) {
  return (
    String(user?.username || "").trim().toLowerCase() === "system" ||
    hasAnyAuthority(user, ENTRY_AUDIT_ADMIN_AUTHORITIES)
  );
}

export function isSelfEntryRecord(user, record) {
  return Boolean(
    user?.id != null &&
    record?.entryUser != null &&
    Number(record.entryUser) === Number(user.id)
  );
}

export function getUserLabel(user) {
  return user?.realName || user?.username || "";
}
