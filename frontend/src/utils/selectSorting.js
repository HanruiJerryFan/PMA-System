const collator = new Intl.Collator("zh-CN", { numeric: true });

function compareValues(left, right) {
  const leftText = String(left ?? "").trim();
  const rightText = String(right ?? "").trim();
  if (!leftText || !rightText) {
    return leftText ? -1 : rightText ? 1 : 0;
  }
  if (typeof left === "number" && typeof right === "number") {
    return left - right;
  }
  return collator.compare(leftText, rightText);
}

// Fields are compared in priority order. Copy the list to preserve API data and selected values.
export function sortSelectItems(items = [], fields = [(item) => item]) {
  return [...items].sort((left, right) => {
    for (const field of fields) {
      const readValue = typeof field === "function" ? field : (item) => item?.[field];
      const result = compareValues(readValue(left), readValue(right));
      if (result !== 0) {
        return result;
      }
    }
    return 0;
  });
}
