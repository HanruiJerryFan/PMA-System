import React, { useEffect, useState } from "react";
import dayjs from "dayjs";
import { Button, Card, Space, Statistic } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { inventoryAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";
import { formatAmount, formatUnitPrice } from "./warehouseDocumentUtils";

const sumInventoryAmount = (rows) =>
  rows.reduce((total, item) => total + Math.round(Number(item.inventoryAmount ?? 0) * 100), 0) / 100;

export default function InventoryStock() {
  const [inventories, setInventories] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchInventory = async () => {
    setLoading(true);
    try {
      const response = await inventoryAPI.getInventory();
      setInventories(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
  }, []);

  const exportMonthlyStockSummary = (rows = inventories) => {
    downloadExcel(
      `inventory-stock-summary-${dayjs().format("YYYY-MM-DD")}.xls`,
      "库存汇总表",
      ["物料编码", "物料名称", "型号", "品牌", "累计入库", "累计出库", "当前库存", "采购均价", "库存金额"],
      [
        ...rows.map((item) => [
          item.materialCode || "",
          item.materialName || "",
          item.model || "",
          item.brand || "",
          item.inboundQuantity ?? "",
          item.outboundQuantity ?? "",
          item.balanceQuantity ?? "",
          item.purchaseAveragePrice ?? "",
          item.inventoryAmount ?? "",
        ]),
        ["设备材料库存总额", "", "", "", "", "", "", "", sumInventoryAmount(rows)],
      ]
    );
  };

  return (
    <Space direction="vertical" size="middle" style={{ width: "100%" }}>
      <Card>
        <Statistic title="设备材料库存总额" value={sumInventoryAmount(inventories)} precision={2} suffix="元" loading={loading} />
      </Card>
      <CRUDTable
        title="库存汇总"
        columns={[
          { title: "物料编码", dataIndex: "materialCode", key: "materialCode", width: 140 },
          { title: "物料名称", dataIndex: "materialName", key: "materialName", width: 220 },
          { title: "型号", dataIndex: "model", key: "model", width: 180 },
          { title: "品牌", dataIndex: "brand", key: "brand", width: 140 },
          { title: "累计入库", dataIndex: "inboundQuantity", key: "inboundQuantity", width: 120 },
          { title: "累计出库", dataIndex: "outboundQuantity", key: "outboundQuantity", width: 120 },
          { title: "当前库存", dataIndex: "balanceQuantity", key: "balanceQuantity", width: 120 },
          { title: "采购均价", dataIndex: "purchaseAveragePrice", key: "purchaseAveragePrice", width: 140, align: "right", render: formatUnitPrice },
          { title: "库存金额", dataIndex: "inventoryAmount", key: "inventoryAmount", width: 140, align: "right", render: formatAmount },
        ]}
        dataSource={inventories}
        loading={loading}
        rowKey={(item) => item.materialCode?.trim() || item.uuid}
        extraActions={({ filteredData, canExport }) => (
          <Button
            icon={<DownloadOutlined />}
            onClick={() => exportMonthlyStockSummary(filteredData)}
            disabled={!canExport || !filteredData.length}
          >
            导出库存汇总表
          </Button>
        )}
        searchFields={[
          { name: "materialCode", label: "物料编码" },
          { name: "materialName", label: "物料名称" },
          { name: "model", label: "型号" },
          { name: "brand", label: "品牌" },
        ]}
      />
    </Space>
  );
}
