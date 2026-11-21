export const RISK_OPTIONS = [
  { value: "OVERDUE", label: "计划超期", color: "error" },
  { value: "STALLED", label: "阶段滞留", color: "warning" },
  { value: "WARRANTY_EXPIRING", label: "质保临期", color: "gold" },
  { value: "WARRANTY_EXPIRED", label: "质保已过期", color: "default" },
  { value: "LOW_COLLECTION", label: "低回款率", color: "orange" },
  { value: "NEGATIVE_MARGIN", label: "负毛利风险", color: "magenta" },
  { value: "PENDING_AUDIT", label: "有待审单据", color: "processing" },
];

export const RISK_META_MAP = Object.fromEntries(RISK_OPTIONS.map((item) => [item.value, item]));

export const WARRANTY_META_MAP = {
  NONE: { label: "未设质保", color: "default" },
  ACTIVE: { label: "质保期内", color: "success" },
  EXPIRING_SOON: { label: "质保临期", color: "warning" },
  EXPIRED: { label: "质保已过期", color: "error" },
};

export function formatMoney(value) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return "-";
  }
  return num.toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export function formatPercent(value) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return "-";
  }
  return `${num.toFixed(2)}%`;
}

export function getRiskMeta(tag) {
  return RISK_META_MAP[tag] || { value: tag, label: tag, color: "default" };
}

export function getWarrantyMeta(status) {
  return WARRANTY_META_MAP[status] || WARRANTY_META_MAP.NONE;
}

export function filterTrackingCards(items = [], filters = {}) {
  const keyword = String(filters.keyword || "").trim().toLowerCase();
  const managerId = filters.managerId ?? null;
  const projectTypeId = filters.projectTypeId ?? null;
  const riskTag = String(filters.riskTag || "").trim().toUpperCase();

  return items.filter((item) => {
    if (managerId != null && item.managerId !== managerId) {
      return false;
    }
    if (projectTypeId != null && item.projectTypeId !== projectTypeId) {
      return false;
    }
    if (riskTag && (!Array.isArray(item.riskTags) || !item.riskTags.includes(riskTag))) {
      return false;
    }
    if (keyword) {
      const haystacks = [
        item.projectNumber,
        item.projectName,
        item.customerName,
        item.managerName,
        item.currentStageName,
        item.regionName,
      ];
      const hit = haystacks.some((field) => String(field || "").toLowerCase().includes(keyword));
      if (!hit) {
        return false;
      }
    }
    return true;
  });
}

export function groupCardsIntoSwimlanes(stages = [], cards = []) {
  const unstartedCards = cards.filter((card) => !card.currentStageId);
  const swimlanes = [];

  if (unstartedCards.length > 0) {
    swimlanes.push({
      key: "UNSTARTED",
      stageId: null,
      stageCode: "0.0",
      description: "未开始",
      cards: unstartedCards,
    });
  }

  const stageIds = new Set(stages.map((stage) => stage.id));
  stages.forEach((stage) => {
    const stageCards = cards.filter((card) => card.currentStageId === stage.id);
    swimlanes.push({
      key: String(stage.id),
      stageId: stage.id,
      stageCode: stage.stageCode,
      description: stage.description,
      cards: stageCards,
    });
  });

  const orphanCards = cards.filter((card) => card.currentStageId && !stageIds.has(card.currentStageId));
  if (orphanCards.length > 0) {
    swimlanes.push({
      key: "OTHER_STAGE",
      stageId: null,
      stageCode: "-",
      description: "其他/停用阶段",
      cards: orphanCards,
    });
  }

  return swimlanes;
}
