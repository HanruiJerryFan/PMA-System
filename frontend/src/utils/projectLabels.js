export function formatProjectOptionLabel(project) {
  return [project?.projectNumber, project?.projectName]
    .map((value) => String(value ?? "").trim())
    .filter(Boolean)
    .join(" - ") || "-";
}
