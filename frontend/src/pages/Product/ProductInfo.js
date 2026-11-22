import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button, Descriptions, Input, Modal, Select, Space, Switch, Table, message } from "antd";
import { DownloadOutlined, UploadOutlined } from "@ant-design/icons";
import CRUDTable from "../../components/Common/CRUDTable";
import { productAPI } from "../../api/modules";
import { downloadApiFile, downloadExcel, resolveBlobErrorMessage } from "../../utils/exporters";
import { sortSelectItems } from "../../utils/selectSorting";

const { Option } = Select;

function normalizeTextFilter(value) {
  return typeof value === "string" ? value.trim() || undefined : undefined;
}

function buildEditableDictList(items = [], currentValue, keyField = "id") {
  return sortSelectItems(
    items.filter(
      (item) =>
        item.isActive ||
        (currentValue != null && String(item[keyField]) === String(currentValue))
    ),
    ["sortOrder", "code"]
  );
}

export default function ProductInfo() {
  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [categories, setCategories] = useState([]);
  const [subcategories, setSubcategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [bands, setBands] = useState([]);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [query, setQuery] = useState({ page: 1, pageSize: 10, filters: {}, reloadKey: 0 });
  const [importVisible, setImportVisible] = useState(false);
  const [pendingImportFile, setPendingImportFile] = useState(null);
  const [importMode, setImportMode] = useState("PARTIAL_SUCCESS");
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [importResultVisible, setImportResultVisible] = useState(false);
  const importInputRef = useRef(null);

  const categoryMap = useMemo(
    () => Object.fromEntries(categories.map((item) => [item.id, `${item.code || "--"} ${item.name}`])),
    [categories]
  );
  const subcategoryMap = useMemo(
    () => Object.fromEntries(subcategories.map((item) => [item.id, `${item.code || "--"} ${item.name}`])),
    [subcategories]
  );
  const brandMap = useMemo(
    () => Object.fromEntries(brands.map((item) => [item.id, `${item.code || "--"} ${item.name}`])),
    [brands]
  );
  const bandMap = useMemo(
    () => Object.fromEntries(bands.map((item) => [item.code, `${item.code || "---"} ${item.description}`])),
    [bands]
  );

  const decorateRows = useCallback(
    (rows) =>
      rows.map((item) => ({
        ...item,
        categoryLabel: categoryMap[item.categoryId] || item.categoryCode || "-",
        subcategoryLabel: subcategoryMap[item.subcategoryId] || item.subcategoryCode || "-",
        brandLabel: brandMap[item.brandId] || item.brandCode || "-",
        bandLabel: bandMap[item.bandCode || item.frequency] || item.bandCode || item.frequency || "-",
      })),
    [bandMap, brandMap, categoryMap, subcategoryMap]
  );

  const decoratedProducts = useMemo(() => decorateRows(products), [decorateRows, products]);

  const fetchProductsPage = useCallback(async ({ page, pageSize, filters }) => {
    setLoading(true);
    try {
      const response = await productAPI.getProductsPage({ page, pageSize, ...filters });
      const pageData = response?.data || {};
      const nextRecords = Array.isArray(pageData.records) ? pageData.records : [];
      const nextTotal = Number(pageData.total ?? 0);
      if (nextRecords.length === 0 && page > 1 && nextTotal > 0) {
        const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize));
        setQuery((prev) => ({ ...prev, page: lastPage }));
        return;
      }
      setProducts(nextRecords);
      setTotal(nextTotal);
    } catch (error) {
      message.error(error?.message || "加载物料列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProductsPage(query);
  }, [fetchProductsPage, query]);

  useEffect(() => {
    async function fetchOptions() {
      try {
        const [categoryResponse, subcategoryResponse, brandResponse, bandResponse] = await Promise.all([
          productAPI.getProductCategories(),
          productAPI.getProductSubcategories(),
          productAPI.getProductBrands(),
          productAPI.getProductBands(),
        ]);
        setCategories(categoryResponse.data || []);
        setSubcategories(subcategoryResponse.data || []);
        setBrands(brandResponse.data || []);
        setBands(bandResponse.data || []);
      } catch (error) {
        message.error(error?.message || "加载物料字典失败");
      }
    }

    fetchOptions();
  }, []);

  const reloadCurrentPage = () => {
    setQuery((prev) => ({ ...prev, reloadKey: prev.reloadKey + 1 }));
  };

  const handleCreate = async (values) => {
    await productAPI.createProduct(values);
    reloadCurrentPage();
  };

  const handleUpdate = async (uuid, values) => {
    await productAPI.updateProduct(uuid, values);
    reloadCurrentPage();
  };

  const handleDelete = async (uuid) => {
    await productAPI.deleteProduct(uuid);
    reloadCurrentPage();
  };

  const handleExportExcel = async () => {
    setExporting(true);
    try {
      const response = await productAPI.getProducts();
      const filters = query.filters || {};
      const filteredRows = (response.data || []).filter((item) => {
        if (filters.materialCode && !String(item.materialCode || "").toLowerCase().includes(filters.materialCode.toLowerCase())) {
          return false;
        }
        if (filters.productName && !String(item.productName || "").toLowerCase().includes(filters.productName.toLowerCase())) {
          return false;
        }
        if (filters.productModel && !String(item.productModel || "").toLowerCase().includes(filters.productModel.toLowerCase())) {
          return false;
        }
        if (filters.brandId != null && Number(item.brandId) !== Number(filters.brandId)) {
          return false;
        }
        if (filters.isActive !== undefined && Boolean(item.isActive) !== Boolean(filters.isActive)) {
          return false;
        }
        return true;
      });
      const rows = decorateRows(filteredRows);
      downloadExcel(
        `materials-${new Date().toISOString().slice(0, 10)}.xls`,
        "物料主数据",
        ["序号", "品牌", "大类", "分项", "名称", "型号", "规格参数", "物料编码", "其他说明", "启用状态"],
        rows.map((item, index) => [
          index + 1,
          item.brandLabel || "",
          item.categoryLabel || "",
          item.subcategoryLabel || "",
          item.productName || "",
          item.productModel || "",
          item.specification || "",
          item.materialCode || "",
          item.otherNote || "",
          item.isActive ? "启用" : "停用",
        ])
      );
    } catch (error) {
      message.error(error?.message || "导出物料数据失败");
    } finally {
      setExporting(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      await downloadApiFile(
        productAPI.downloadImportTemplate(),
        "materials-import-template.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      );
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "下载模板失败"));
    }
  };

  const handleImportClick = () => importInputRef.current?.click();

  const handleImportFileChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setPendingImportFile(file);
    setImportVisible(true);
  };

  const resetImportState = () => {
    setImportVisible(false);
    setPendingImportFile(null);
    setImportMode("PARTIAL_SUCCESS");
    if (importInputRef.current) {
      importInputRef.current.value = "";
    }
  };

  const handleImport = async () => {
    if (!pendingImportFile) {
      message.error("请选择导入文件");
      return;
    }
    const formData = new FormData();
    formData.append("file", pendingImportFile);
    setImporting(true);
    try {
      const response = await productAPI.importProducts(formData, importMode);
      reloadCurrentPage();
      setImportResult(response.data || null);
      setImportResultVisible(true);
      const imported = response.data?.importedRows ?? 0;
      const failed = response.data?.failedRows ?? 0;
      if (failed > 0) {
        message.warning(`导入成功 ${imported} 行，失败 ${failed} 行`);
      } else {
        message.success(`导入成功 ${imported} 行，失败 0 行`);
      }
      resetImportState();
    } catch (error) {
      message.error(error?.message || "导入失败");
    } finally {
      setImporting(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <CRUDTable
        title="物料主数据"
        columns={[
          {
            title: "序号",
            key: "serialNo",
            width: 80,
            render: (_, __, index) => (query.page - 1) * query.pageSize + index + 1,
            sorter: (left, right) => (left.id || 0) - (right.id || 0),
          },
          { title: "品牌", dataIndex: "brandLabel", key: "brandLabel", width: 180 },
          { title: "大类", dataIndex: "categoryLabel", key: "categoryLabel", width: 180 },
          { title: "分项", dataIndex: "subcategoryLabel", key: "subcategoryLabel", width: 180 },
          { title: "名称", dataIndex: "productName", key: "productName", width: 220 },
          { title: "型号", dataIndex: "productModel", key: "productModel", width: 180 },
          { title: "规格参数", dataIndex: "specification", key: "specification", width: 220 },
          { title: "物料编码", dataIndex: "materialCode", key: "materialCode", width: 160 },
          { title: "频段", dataIndex: "bandLabel", key: "bandLabel", width: 140 },
          { title: "其他说明", dataIndex: "otherNote", key: "otherNote", width: 220 },
          { title: "启用状态", dataIndex: "isActive", key: "isActive", width: 100, render: (value) => (value ? "启用" : "停用") },
        ]}
        dataSource={decoratedProducts}
        loading={loading}
        onCreate={handleCreate}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
        rowKey="uuid"
        serverPagination={{
          current: query.page,
          pageSize: query.pageSize,
          total,
          onChange: (nextPage, nextPageSize) => setQuery((prev) => ({ ...prev, page: nextPage, pageSize: nextPageSize })),
          onSearch: (values) => {
            setQuery((prev) => ({
              ...prev,
              page: 1,
              filters: {
                materialCode: normalizeTextFilter(values.materialCode),
                productName: normalizeTextFilter(values.productName),
                productModel: normalizeTextFilter(values.productModel),
                brandId: values.brandId != null ? Number(values.brandId) : undefined,
                isActive: values.isActive !== undefined ? values.isActive : undefined,
              },
            }));
          },
          onReset: () => setQuery((prev) => ({ ...prev, page: 1, filters: {} })),
        }}
        extraActions={({ canManage, canExport }) => (
          <Space>
            <input ref={importInputRef} type="file" accept=".xlsx,.xls" style={{ display: "none" }} onChange={handleImportFileChange} />
            <Button icon={<UploadOutlined />} onClick={handleImportClick} disabled={!canManage}>导入 Excel</Button>
            <Button onClick={handleDownloadTemplate} disabled={!canManage}>下载模板</Button>
            <Button
              icon={<DownloadOutlined />}
              loading={exporting}
              onClick={handleExportExcel}
              disabled={!canExport || total === 0}
            >
              导出 Excel
            </Button>
          </Space>
        )}
        searchFields={[
          { name: "materialCode", label: "物料编码" },
          { name: "productName", label: "名称" },
          { name: "productModel", label: "型号" },
          {
            name: "brandId",
            label: "品牌",
            component: (
              <Select placeholder="请选择品牌" allowClear style={{ width: 160 }}>
                {sortSelectItems(brands, ["sortOrder", "code"]).map((item) => (
                  <Option key={item.id} value={item.id}>
                    {item.code || "--"} {item.name}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "isActive",
            label: "启用状态",
            component: (
              <Select placeholder="全部" allowClear style={{ width: 110 }}>
                <Option value={true}>启用</Option>
                <Option value={false}>停用</Option>
              </Select>
            ),
          },
        ]}
        detailFields={[
          { key: "materialCode", label: "物料编码" },
          { key: "brandLabel", label: "品牌" },
          { key: "categoryLabel", label: "大类" },
          { key: "subcategoryLabel", label: "分项" },
          { key: "productName", label: "名称" },
          { key: "productModel", label: "型号" },
          { key: "specification", label: "规格参数" },
          { key: "otherNote", label: "其他说明" },
          { key: "manufacturer", label: "厂家" },
          { key: "unit", label: "单位" },
          { key: "bandLabel", label: "频段" },
          { key: "isActive", label: "启用状态" },
        ]}
        formFields={[
          { name: "productName", label: "物料名称", rules: [{ required: true, message: "请输入物料名称" }], component: <Input placeholder="请输入物料名称" /> },
          { name: "productModel", label: "型号", component: <Input placeholder="请输入型号" /> },
          {
            name: "categoryId",
            label: "大类",
            rules: [{ required: true, message: "请选择大类" }],
            component: ({ editingRecord }) => (
              <Select placeholder="请选择大类" disabled={Boolean(editingRecord)}>
                {buildEditableDictList(categories, editingRecord?.categoryId).map((item) => (
                  <Option key={item.id} value={item.id}>
                    {item.code || "--"} {item.name}{item.isActive === false ? "（已停用）" : ""}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "subcategoryId",
            label: "分项",
            rules: [{ required: true, message: "请选择分项" }],
            component: ({ editingRecord }) => (
              <Select placeholder="请选择分项" disabled={Boolean(editingRecord)}>
                {buildEditableDictList(subcategories, editingRecord?.subcategoryId).map((item) => (
                  <Option key={item.id} value={item.id}>
                    {item.code || "--"} {item.name}{item.isActive === false ? "（已停用）" : ""}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "brandId",
            label: "品牌",
            rules: [{ required: true, message: "请选择品牌" }],
            component: ({ editingRecord }) => (
              <Select placeholder="请选择品牌" disabled={Boolean(editingRecord)}>
                {buildEditableDictList(brands, editingRecord?.brandId).map((item) => (
                  <Option key={item.id} value={item.id}>
                    {item.code || "--"} {item.name}{item.isActive === false ? "（已停用）" : ""}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "frequency",
            label: "频段",
            rules: [{ required: true, message: "请选择频段" }],
            component: ({ editingRecord }) => (
              <Select placeholder="请选择频段" disabled={Boolean(editingRecord)}>
                {buildEditableDictList(
                  bands,
                  editingRecord ? editingRecord.bandCode || editingRecord.frequency : null,
                  "code"
                ).map((item) => (
                  <Option key={item.id} value={item.code}>
                    {item.code || "---"} {item.description}{item.isActive === false ? "（已停用）" : ""}
                  </Option>
                ))}
              </Select>
            ),
          },
          { name: "manufacturer", label: "厂家", component: <Input placeholder="请输入厂家" /> },
          { name: "unit", label: "单位", rules: [{ required: true, message: "请输入单位" }], component: <Input placeholder="请输入单位" /> },
          { name: "specification", label: "规格参数", component: <Input.TextArea rows={3} placeholder="请输入规格参数" /> },
          { name: "otherNote", label: "其他说明", component: <Input.TextArea rows={3} placeholder="请输入其他说明" /> },
          { name: "isActive", label: "启用状态", valuePropName: "checked", getDefaultValue: () => true, component: <Switch checkedChildren="启用" unCheckedChildren="停用" /> },
        ]}
        mapRecordToFormValues={(record) => ({
          ...record,
          frequency: record.bandCode || record.frequency || undefined,
          isActive: Boolean(record.isActive),
        })}
        transformValues={(values) => ({ ...values, isActive: Boolean(values.isActive) })}
      />

      <Modal title="导入物料主数据" open={importVisible} onOk={handleImport} confirmLoading={importing} onCancel={resetImportState} destroyOnHidden>
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <div><strong>已选文件：</strong> {pendingImportFile?.name || "-"}</div>
          <div>必填列：名称、大类编码、分项编码、品牌编码、频段编码、单位。</div>
          <Space direction="vertical" size={4} style={{ width: "100%" }}>
            <span>导入模式</span>
            <Select value={importMode} onChange={setImportMode} style={{ width: "100%" }}>
              <Option value="PARTIAL_SUCCESS">部分成功</Option>
              <Option value="FAIL_FAST">遇错即停</Option>
            </Select>
          </Space>
        </Space>
      </Modal>

      <Modal title="导入结果" open={importResultVisible} onCancel={() => setImportResultVisible(false)} footer={null} width={900}>
        {importResult ? (
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="导入模式">{importResult.importMode}</Descriptions.Item>
              <Descriptions.Item label="总行数">{importResult.totalRows}</Descriptions.Item>
              <Descriptions.Item label="成功行数">{importResult.importedRows}</Descriptions.Item>
              <Descriptions.Item label="跳过行数">{importResult.skippedRows}</Descriptions.Item>
              <Descriptions.Item label="失败行数">{importResult.failedRows}</Descriptions.Item>
              <Descriptions.Item label="资源类型">{importResult.resourceType}</Descriptions.Item>
            </Descriptions>
            <Table
              rowKey={(record) => `${record.rowNumber}-${record.message}`}
              dataSource={importResult.errors || []}
              pagination={{ pageSize: 5 }}
              columns={[
                { title: "行号", dataIndex: "rowNumber", key: "rowNumber", width: 80 },
                { title: "错误信息", dataIndex: "message", key: "message", width: 260 },
                { title: "失败行数据", dataIndex: "rowData", key: "rowData", render: (value) => value ? Object.entries(value).filter(([, item]) => item).map(([key, item]) => `${key}: ${item}`).join(" | ") : "-" },
              ]}
            />
          </Space>
        ) : null}
      </Modal>
    </Space>
  );
}
