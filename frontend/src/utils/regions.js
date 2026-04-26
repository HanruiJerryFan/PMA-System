export function buildRegionOptions(regions) {
  const childrenByParentCode = regions.reduce((map, item) => {
    const parentCode = String(item.parentCode ?? 0);
    if (!map[parentCode]) {
      map[parentCode] = [];
    }
    map[parentCode].push(item);
    return map;
  }, {});

  const buildNode = (region) => {
    const children = (childrenByParentCode[String(region.areaCode)] || []).map(buildNode);
    return {
      value: region.id,
      label: region.name,
      children: children.length ? children : undefined,
      isLeaf: children.length === 0,
    };
  };

  return regions
    .filter((item) => Number(item.level) === 1)
    .map(buildNode);
}

export function buildRegionPath(regionId, regionById, regionByAreaCode) {
  const path = [];
  let current = regionById[regionId];

  while (current) {
    path.unshift(current.id);
    if (!current.parentCode || Number(current.parentCode) === 0) {
      break;
    }
    current = regionByAreaCode[current.parentCode];
  }

  return path;
}

export function formatRegionLabel(region) {
  if (!region) {
    return "-";
  }
  return region.mergerName ? region.mergerName.replaceAll(",", " / ") : region.name;
}
