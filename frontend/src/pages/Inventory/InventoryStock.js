import React, { useEffect, useState } from "react";
import dayjs from "dayjs";
import { Button } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { inventoryAPI } from "../../api/modules";
import { downloadExcel } from "../../utils/exporters";

const BUSINESS_CATEGORY_LABELS = {
  PROJECT_PURCHASE: "项目采购",
  PROJECT_SALES: "项目销售",
  CENTRALIZED_PURCHASE: "集中采购",
  WAREHOUSE_TRANSFER_TO_PROJECT: "仓库调拨到项目",
  PROJECT_TRANSFER_TO_WAREHOUSE: "项目退回仓库",
};

function getBusinessCategoryLabel(value) {
  return BUSINESS_CATEGORY_LABELS[value] || value || "-";
}

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
      `monthly-stock-detail-summary-${dayjs().format("YYYY-MM")}.xls`,
      "当月盘点明细汇总表",
      ["物料编码", "物料名称", "型号", "品牌", "业务类别", "仓库", "项目", "累计入库", "累计出库", "当前库存", "最后业务时间"],
      rows.map((item) => [
        item.materialCode || "",
        item.materialName || "",
        item.model || "",
        item.brand || "",
        getBusinessCategoryLabel(item.businessCategory),
        item.warehouseName || "",
        item.projectName || "",
        item.inboundQuantity ?? "",
        item.outboundQuantity ?? "",
        item.balanceQuantity ?? "",
        item.lastTxnTime ? dayjs(item.lastTxnTime).format("YYYY-MM-DD HH:mm:ss") : "",
      ])
    );
  };

  return (
    <CRUDTable
      title="库存汇总"
      columns={[
        { title: "物料编码", dataIndex: "materialCode", key: "materialCode", width: 140 },
        { title: "物料名称", dataIndex: "materialName", key: "materialName", width: 220 },
        { title: "型号", dataIndex: "model", key: "model", width: 180 },
        { title: "品牌", dataIndex: "brand", key: "brand", width: 140 },
        {
          title: "业务类别",
          dataIndex: "businessCategory",
          key: "businessCategory",
          width: 180,
          render: (value) => getBusinessCategoryLabel(value),
        },
        { title: "仓库", dataIndex: "warehouseName", key: "warehouseName", width: 180 },
        { title: "项目", dataIndex: "projectName", key: "projectName", width: 220 },
        { title: "累计入库", dataIndex: "inboundQuantity", key: "inboundQuantity", width: 120 },
        { title: "累计出库", dataIndex: "outboundQuantity", key: "outboundQuantity", width: 120 },
        { title: "当前库存", dataIndex: "balanceQuantity", key: "balanceQuantity", width: 120 },
        {
          title: "最后业务时间",
          dataIndex: "lastTxnTime",
          key: "lastTxnTime",
          width: 180,
          render: (value) => (value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "-"),
        },
      ]}
      dataSource={inventories}
      loading={loading}
      rowKey="uuid"
      extraActions={({ filteredData, canExport }) => (
        <Button
          icon={<DownloadOutlined />}
          onClick={() => exportMonthlyStockSummary(filteredData)}
          disabled={!canExport || !filteredData.length}
        >
          导出当月盘点明细汇总表
        </Button>
      )}
      searchFields={[
        { name: "materialCode", label: "物料编码" },
        { name: "materialName", label: "物料名称" },
        { name: "warehouseName", label: "仓库" },
        { name: "projectName", label: "项目" },
      ]}
    />
  );
}
