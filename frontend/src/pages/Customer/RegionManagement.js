import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Input,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Tree,
  Typography,
} from "antd";
import { EnvironmentOutlined, GlobalOutlined, SearchOutlined } from "@ant-design/icons";
import { regionAPI } from "../../api/modules";
import { formatRegionLabel } from "../../utils/regions";

const { Search } = Input;
const { Text, Title } = Typography;

function renderLevel(level) {
  if (Number(level) === 1) {
    return <Tag color="blue">省</Tag>;
  }
  if (Number(level) === 2) {
    return <Tag color="green">市</Tag>;
  }
  return <Tag color="gold">区县</Tag>;
}

function normalizeKeyword(value) {
  return String(value || "").trim().toLowerCase();
}

function formatInternalRegionId(value) {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return String(value || "").padStart(4, "0");
  }
  return String(Math.trunc(numericValue)).padStart(4, "0");
}

function renderMatchedContent(value, keyword) {
  const source = String(value ?? "");
  if (!keyword) {
    return source || "-";
  }

  const normalizedSource = source.toLowerCase();
  const matchIndex = normalizedSource.indexOf(keyword);
  if (matchIndex < 0) {
    return source || "-";
  }

  const endIndex = matchIndex + keyword.length;
  return (
    <>
      {source.slice(0, matchIndex)}
      <span style={{ backgroundColor: "#fde68a", color: "#92400e", padding: "0 2px", borderRadius: 2 }}>
        {source.slice(matchIndex, endIndex)}
      </span>
      {source.slice(endIndex)}
    </>
  );
}

function matchesKeyword(region, keyword) {
  if (!keyword) {
    return true;
  }

  return [region.id, formatInternalRegionId(region.id), region.name, region.shortName, region.pinyin, region.mergerName, region.areaCode, region.cityCode]
    .filter(Boolean)
    .some((item) => String(item).toLowerCase().includes(keyword));
}

export default function RegionManagement() {
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [selectedRegionId, setSelectedRegionId] = useState(null);
  const [expandedKeys, setExpandedKeys] = useState([]);

  const fetchRegions = useCallback(async () => {
    setLoading(true);
    try {
      const response = await regionAPI.getRegions();
      const nextRegions = response.data || [];
      setRegions(nextRegions);

      if (nextRegions.length > 0) {
        const firstProvince = nextRegions.find((item) => Number(item.level) === 1) || nextRegions[0];
        setSelectedRegionId((currentValue) => currentValue || firstProvince?.id || null);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRegions();
  }, [fetchRegions]);

  const regionById = useMemo(
    () => Object.fromEntries(regions.map((item) => [String(item.id), item])),
    [regions]
  );

  const childrenByParentCode = useMemo(
    () =>
      regions.reduce((accumulator, item) => {
        const parentCode = String(item.parentCode ?? 0);
        if (!accumulator[parentCode]) {
          accumulator[parentCode] = [];
        }
        accumulator[parentCode].push(item);
        return accumulator;
      }, {}),
    [regions]
  );

  const levelStats = useMemo(
    () => ({
      provinces: regions.filter((item) => Number(item.level) === 1).length,
      cities: regions.filter((item) => Number(item.level) === 2).length,
      districts: regions.filter((item) => Number(item.level) === 3).length,
    }),
    [regions]
  );

  const normalizedKeyword = useMemo(() => normalizeKeyword(keyword), [keyword]);

  const filteredRegions = useMemo(
    () => regions.filter((item) => matchesKeyword(item, normalizedKeyword)),
    [regions, normalizedKeyword]
  );

  const treeData = useMemo(() => {
    const buildTreeNodes = (parentCode = 0) => {
      const children = childrenByParentCode[String(parentCode)] || [];

      return children
        .map((item) => {
          const childNodes = buildTreeNodes(item.areaCode);
          const selfMatched = matchesKeyword(item, normalizedKeyword);

          if (normalizedKeyword && !selfMatched && childNodes.length === 0) {
            return null;
          }

          return {
            key: String(item.id),
            title: normalizedKeyword && selfMatched
              ? (
                  <span
                    style={{
                      display: "inline-block",
                      width: "100%",
                      backgroundColor: "#fde68a",
                      color: "#92400e",
                      borderRadius: 4,
                      padding: "2px 6px",
                    }}
                  >
                    {item.name} ({item.areaCode})
                  </span>
                )
              : `${item.name} (${item.areaCode})`,
            children: childNodes.length ? childNodes : undefined,
          };
        })
        .filter(Boolean);
    };

    return buildTreeNodes();
  }, [childrenByParentCode, normalizedKeyword]);

  const visibleTreeKeys = useMemo(() => {
    const collectKeys = (nodes) =>
      nodes.reduce((accumulator, node) => {
        accumulator.push(node.key);
        if (node.children?.length) {
          accumulator.push(...collectKeys(node.children));
        }
        return accumulator;
      }, []);

    return collectKeys(treeData);
  }, [treeData]);

  const secondLevelCount = useMemo(() => {
    if (treeData.length !== 1) {
      return null;
    }
    return treeData[0]?.children?.length ?? 0;
  }, [treeData]);

  useEffect(() => {
    if (!normalizedKeyword) {
      setExpandedKeys([]);
      return;
    }

    if (filteredRegions.length <= 10 || (treeData.length === 1 && secondLevelCount <= 2)) {
      setExpandedKeys(visibleTreeKeys);
      return;
    }

    setExpandedKeys([]);
  }, [filteredRegions.length, normalizedKeyword, secondLevelCount, treeData.length, visibleTreeKeys]);

  const selectedRegion = useMemo(() => {
    const explicit = selectedRegionId ? regionById[String(selectedRegionId)] : null;
    if (explicit) {
      return explicit;
    }
    return filteredRegions[0] || null;
  }, [filteredRegions, regionById, selectedRegionId]);

  const selectedChildren = useMemo(
    () => (selectedRegion ? childrenByParentCode[String(selectedRegion.areaCode)] || [] : []),
    [childrenByParentCode, selectedRegion]
  );

  const provinceSummaries = useMemo(() => {
    return regions
      .filter((item) => Number(item.level) === 1)
      .map((province) => {
        const cities = childrenByParentCode[String(province.areaCode)] || [];
        const districts = cities.reduce(
          (count, city) => count + ((childrenByParentCode[String(city.areaCode)] || []).length || 0),
          0
        );

        return {
          id: province.id,
          name: province.name,
          areaCode: province.areaCode,
          cityCount: cities.length,
          districtCount: districts,
          mergerName: formatRegionLabel(province),
        };
      })
      .sort((left, right) => left.name.localeCompare(right.name));
  }, [childrenByParentCode, regions]);

  const regionColumns = [
    {
      title: "内部 ID",
      dataIndex: "id",
      key: "id",
      width: 110,
      render: (value) => renderMatchedContent(formatInternalRegionId(value), normalizedKeyword),
    },
    {
      title: "地区",
      dataIndex: "name",
      key: "name",
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{renderMatchedContent(record.name, normalizedKeyword)}</Text>
          <Text type="secondary">{renderMatchedContent(record.shortName || "-", normalizedKeyword)}</Text>
        </Space>
      ),
    },
    {
      title: "层级",
      dataIndex: "level",
      key: "level",
      width: 120,
      render: renderLevel,
    },
    {
      title: "区划编码",
      dataIndex: "areaCode",
      key: "areaCode",
      width: 160,
      render: (value) => renderMatchedContent(value, normalizedKeyword),
    },
    {
      title: "上级编码",
      dataIndex: "parentCode",
      key: "parentCode",
      width: 160,
      render: (value) => renderMatchedContent(value || "-", normalizedKeyword),
    },
    {
      title: "合并路径",
      dataIndex: "mergerName",
      key: "mergerName",
      render: (value, record) => renderMatchedContent(formatRegionLabel({ ...record, mergerName: value }), normalizedKeyword),
    },
  ];

  const provinceColumns = [
    {
      title: "省份",
      dataIndex: "name",
      key: "name",
      width: 180,
      render: (value, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{value}</Text>
          <Text type="secondary">{record.areaCode}</Text>
        </Space>
      ),
    },
    {
      title: "城市数",
      dataIndex: "cityCount",
      key: "cityCount",
      width: 120,
    },
    {
      title: "区县数",
      dataIndex: "districtCount",
      key: "districtCount",
      width: 120,
    },
    {
      title: "覆盖路径",
      dataIndex: "mergerName",
      key: "mergerName",
      render: (value) => value || "-",
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card
        variant="borderless"
        style={{
          background: "linear-gradient(135deg, #0f172a 0%, #1d4ed8 55%, #38bdf8 100%)",
          color: "#fff",
          overflow: "hidden",
        }}
        styles={{ body: { padding: 24 } }}
      >
        <Row gutter={[24, 24]} align="middle">
          <Col xs={24} lg={16}>
            <Space direction="vertical" size={8}>
              <Tag color="rgba(255,255,255,0.2)" style={{ border: "none", color: "#fff", width: "fit-content" }}>
                独立目录
              </Tag>
              <Title level={2} style={{ color: "#fff", margin: 0 }}>
                地区目录
              </Title>
            </Space>
          </Col>
          <Col xs={24} lg={8}>
            <Space direction="vertical" size={12} style={{ width: "100%" }}>
              <Alert
                type="info"
                showIcon
                message="覆盖说明"
                description="地区主数据仅在数据库层维护，本页面只提供查询和覆盖核对，不提供导入编辑。"
              />
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="省份数" value={levelStats.provinces} prefix={<GlobalOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="城市数" value={levelStats.cities} prefix={<EnvironmentOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="区县数" value={levelStats.districts} prefix={<SearchOutlined />} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={9}>
          <Card
            title="地区树"
            extra={
              <Search
                allowClear
                placeholder="按名称、拼音或区划编码搜索"
                onChange={(event) => setKeyword(event.target.value)}
                style={{ width: 280 }}
              />
            }
          >
            {treeData.length ? (
              <Tree
                blockNode
                height={560}
                treeData={treeData}
                expandedKeys={expandedKeys}
                selectedKeys={selectedRegion ? [String(selectedRegion.id)] : []}
                onExpand={(keys) => setExpandedKeys(keys)}
                onSelect={(keys) => {
                  if (keys[0]) {
                    setSelectedRegionId(Number(keys[0]));
                  }
                }}
              />
            ) : (
              <Empty description="当前关键字下没有匹配的地区" />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={15}>
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Card title="当前选中地区">
              {selectedRegion ? (
                <>
                <Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
                  <Descriptions.Item label="内部 ID">{formatInternalRegionId(selectedRegion.id)}</Descriptions.Item>
                  <Descriptions.Item label="名称">{selectedRegion.name}</Descriptions.Item>
                  <Descriptions.Item label="层级">{renderLevel(selectedRegion.level)}</Descriptions.Item>
                  <Descriptions.Item label="区划编码">{selectedRegion.areaCode}</Descriptions.Item>
                  <Descriptions.Item label="上级编码">{selectedRegion.parentCode || "-"}</Descriptions.Item>
                  <Descriptions.Item label="城市编码">{selectedRegion.cityCode || "-"}</Descriptions.Item>
                  <Descriptions.Item label="邮政编码">{selectedRegion.zipCode || "-"}</Descriptions.Item>
                  <Descriptions.Item label="拼音">{selectedRegion.pinyin || "-"}</Descriptions.Item>
                  <Descriptions.Item label="坐标">
                    {selectedRegion.lng != null && selectedRegion.lat != null
                      ? `${selectedRegion.lng}, ${selectedRegion.lat}`
                      : "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="合并路径" span={2}>
                    {formatRegionLabel(selectedRegion)}
                  </Descriptions.Item>
                </Descriptions>
                {normalizedKeyword ? (
                  <div style={{ marginTop: 16 }}>
                    <Divider style={{ margin: "0 0 12px" }} />
                    <Space direction="vertical" size={8} style={{ width: "100%" }}>
                      {[
                        { key: "id", label: "内部 ID", value: formatInternalRegionId(selectedRegion.id) },
                        { key: "name", label: "地区名称", value: selectedRegion.name },
                        { key: "areaCode", label: "地区编码", value: selectedRegion.areaCode },
                        { key: "parentCode", label: "上级编码", value: selectedRegion.parentCode || "-" },
                        { key: "cityCode", label: "城市编码", value: selectedRegion.cityCode || "-" },
                        { key: "zipCode", label: "邮政编码", value: selectedRegion.zipCode || "-" },
                        { key: "pinyin", label: "拼音", value: selectedRegion.pinyin || "-" },
                        { key: "fullPath", label: "完整路径", value: formatRegionLabel(selectedRegion) },
                      ]
                        .filter((item) => String(item.value ?? "").toLowerCase().includes(normalizedKeyword))
                        .map((item) => (
                          <div
                            key={item.key}
                            style={{
                              padding: "8px 12px",
                              borderRadius: 8,
                              background: "#fff7ed",
                              border: "1px solid #fdba74",
                            }}
                          >
                            <Text strong>{item.label}：</Text>
                            <Text>{renderMatchedContent(item.value, normalizedKeyword)}</Text>
                          </div>
                        ))}
                    </Space>
                  </div>
                ) : null}
                </>
              ) : (
                <Empty description="请从左侧树中选择一个地区" />
              )}
            </Card>

            <Card title={`直属下级（${selectedChildren.length}）`}>
              <Table
                rowKey="id"
                loading={loading}
                dataSource={selectedChildren}
                columns={regionColumns}
                pagination={false}
                scroll={{ x: 900 }}
                locale={{ emptyText: "当前节点没有下级地区" }}
              />
            </Card>
          </Space>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={14}>
          <Card title={`匹配地区（${filteredRegions.length}）`}>
            <Table
              rowKey="id"
              loading={loading}
              dataSource={filteredRegions}
              columns={regionColumns}
              pagination={{ pageSize: 10, showSizeChanger: true }}
              scroll={{ x: 980 }}
              onRow={(record) => ({
                onClick: () => setSelectedRegionId(record.id),
                style: { cursor: "pointer" },
              })}
            />
          </Card>
        </Col>
        <Col xs={24} xl={10}>
          <Card title="省级覆盖统计">
            <Table
              rowKey="id"
              loading={loading}
              dataSource={provinceSummaries}
              columns={provinceColumns}
              pagination={{ pageSize: 8 }}
              scroll={{ x: 620 }}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
