import { sortSelectItems } from "./selectSorting";

export const SUBJECT_LEVEL_OPTIONS = [
  { value: 1, label: "一级科目" },
  { value: 2, label: "二级科目" },
];

export function getFinanceSubjectOptions(subjects, level) {
  return sortSelectItems(subjects.filter((item) => item.subjectLevel === level), ["sortOrder", "id"])
    .map((item) => ({ value: item.id, label: item.subjectName }));
}
