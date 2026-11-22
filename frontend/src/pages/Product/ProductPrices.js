import React, { useEffect, useMemo, useState } from "react";
import { Form, InputNumber, Select, message } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI, permissionAPI, productAPI } from "../../api/modules";
import { sortSelectItems } from "../../utils/selectSorting";

const { Option } = Select;
const EMPTY_MODEL_SENTINEL = "__EMPTY_MODEL__";

function normalizeResponseData(response) {
  return response?.data ?? response ?? [];
}

function MaterialCascadeFields({ form, products, categories, subcategories, bands, editingRecord }) {
  const selectedCategoryId = Form.useWatch("categoryId", form);
  const selectedSubcategoryId = Form.useWatch("subcategoryId", form);
  const selectedProductName = Form.useWatch("selectedProductName", form);
  const selectedProductModel = Form.useWatch("selectedProductModel", form);
  const selectedBandCode = Form.useWatch("selectedBandCode", form);

  const editingMaterial = useMemo(
    () =>
      editingRecord?.productBasicInfoUuid
        ? products.find((item) => item.uuid === editingRecord.productBasicInfoUuid) || null
        : null,
    [editingRecord, products]
  );

  const activeCategories = useMemo(
    () =>
      categories.filter(
        (item) =>
          item.isActive !== false ||
          (editingMaterial && Number(item.id) === Number(editingMaterial.categoryId))
      ),
    [categories, editingMaterial]
  );
  const activeSubcategories = useMemo(
    () =>
      subcategories.filter(
        (item) =>
          item.isActive !== false ||
          (editingMaterial && Number(item.id) === Number(editingMaterial.subcategoryId))
      ),
    [editingMaterial, subcategories]
  );
  const activeBands = useMemo(
    () =>
      bands.filter(
        (item) =>
          item.isActive !== false ||
          (editingMaterial &&
            item.code === (editingMaterial.bandCode || editingMaterial.frequency))
      ),
    [bands, editingMaterial]
  );

  const filteredProducts = useMemo(
    () =>
      products.filter(
        (item) =>
          (item.isActive !== false || (editingMaterial && item.uuid === editingMaterial.uuid)) &&
          (selectedCategoryId == null || Number(item.categoryId) === Number(selectedCategoryId)) &&
          (selectedSubcategoryId == null || Number(item.subcategoryId) === Number(selectedSubcategoryId))
      ),
    [editingMaterial, products, selectedCategoryId, selectedSubcategoryId]
  );

  const productNameOptions = useMemo(
    () => [...new Set(filteredProducts.map((item) => item.productName).filter(Boolean))],
    [filteredProducts]
  );

  const filteredByName = useMemo(
    () => filteredProducts.filter((item) => !selectedProductName || item.productName === selectedProductName),
    [filteredProducts, selectedProductName]
  );

  const productModelOptions = useMemo(
    () => [...new Set(filteredByName.map((item) => item.productModel || EMPTY_MODEL_SENTINEL))],
    [filteredByName]
  );

  const filteredByModel = useMemo(
    () =>
      filteredByName.filter(
        (item) =>
          !selectedProductModel ||
          (item.productModel || EMPTY_MODEL_SENTINEL) === selectedProductModel
      ),
    [filteredByName, selectedProductModel]
  );

  const bandOptions = useMemo(
    () => [...new Set(filteredByModel.map((item) => item.bandCode || item.frequency || "").filter(Boolean))],
    [filteredByModel]
  );

  const materialOptions = useMemo(
    () =>
      filteredByModel.filter(
        (item) => !selectedBandCode || (item.bandCode || item.frequency || "") === selectedBandCode
      ),
    [filteredByModel, selectedBandCode]
  );

  return (
    <>
      <Form.Item name="categoryId" label="大类" rules={[{ required: true, message: "请选择大类" }]}>
        <Select
          placeholder="请选择大类"
          showSearch
          optionFilterProp="children"
          onChange={() =>
            form.setFieldsValue({
              subcategoryId: undefined,
              selectedProductName: undefined,
              selectedProductModel: undefined,
              selectedBandCode: undefined,
              productBasicInfoUuid: undefined,
            })
          }
        >
          {sortSelectItems(activeCategories, ["sortOrder","code"]).map((item) => (
            <Option key={item.id} value={item.id}>
              {item.name}{item.isActive === false ? "（已停用）" : ""}
            </Option>
          ))}
        </Select>
      </Form.Item>

      <Form.Item name="subcategoryId" label="分项" rules={[{ required: true, message: "请选择分项" }]}>
        <Select
          placeholder="请选择分项"
          showSearch
          optionFilterProp="children"
          onChange={() =>
            form.setFieldsValue({
              selectedProductName: undefined,
              selectedProductModel: undefined,
              selectedBandCode: undefined,
              productBasicInfoUuid: undefined,
            })
          }
        >
          {sortSelectItems(activeSubcategories, ["sortOrder","code"]).map((item) => (
            <Option key={item.id} value={item.id}>
              {item.name}{item.isActive === false ? "（已停用）" : ""}
            </Option>
          ))}
        </Select>
      </Form.Item>

      <Form.Item name="selectedProductName" label="名称" rules={[{ required: true, message: "请选择名称" }]}>
        <Select
          placeholder="请选择名称"
          showSearch
          optionFilterProp="children"
          onChange={() =>
            form.setFieldsValue({
              selectedProductModel: undefined,
              selectedBandCode: undefined,
              productBasicInfoUuid: undefined,
            })
          }
        >
          {productNameOptions.map((name) => (
            <Option key={name} value={name}>
              {name}
            </Option>
          ))}
        </Select>
      </Form.Item>

      <Form.Item name="selectedProductModel" label="型号" rules={[{ required: true, message: "请选择型号" }]}>
        <Select
          placeholder="请选择型号"
          showSearch
          optionFilterProp="children"
          onChange={() =>
            form.setFieldsValue({
              selectedBandCode: undefined,
              productBasicInfoUuid: undefined,
            })
          }
        >
          {sortSelectItems(productModelOptions).map((model) => (
            <Option key={model} value={model}>
              {model === EMPTY_MODEL_SENTINEL ? "未填写型号" : model}
            </Option>
          ))}
        </Select>
      </Form.Item>

      <Form.Item name="selectedBandCode" label="频段">
        <Select
          placeholder="请选择频段"
          allowClear
          showSearch
          optionFilterProp="children"
          onChange={() => form.setFieldsValue({ productBasicInfoUuid: undefined })}
        >
          {sortSelectItems(bandOptions, [
            (code) => activeBands.find((item) => item.code === code)?.sortOrder,
            (code) => code,
          ]).map((bandCode) => {
            const band = activeBands.find((item) => item.code === bandCode);
            return (
              <Option key={bandCode} value={bandCode}>
                {band?.description || bandCode}{band?.isActive === false ? "（已停用）" : ""}
              </Option>
            );
          })}
        </Select>
      </Form.Item>

      <Form.Item name="productBasicInfoUuid" label="物料" rules={[{ required: true, message: "请选择物料" }]}>
        <Select placeholder="请选择物料" showSearch optionFilterProp="children">
          {sortSelectItems(materialOptions, ["materialCode"]).map((item) => (
            <Option key={item.uuid} value={item.uuid}>
              {item.materialCode} / {item.productModel || "未填写型号"}{item.isActive === false ? "（已停用）" : ""}
            </Option>
          ))}
        </Select>
      </Form.Item>
    </>
  );
}

export default function ProductPrices() {
  const [prices, setPrices] = useState([]);
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [subcategories, setSubcategories] = useState([]);
  const [bands, setBands] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [customerTypes, setCustomerTypes] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);

  const productMap = useMemo(() => Object.fromEntries(products.map((item) => [item.uuid, item])), [products]);
  const userMap = useMemo(
    () => Object.fromEntries(users.map((item) => [item.id, item.realName || item.username || `用户${item.id}`])),
    [users]
  );

  const decoratedPrices = useMemo(
    () =>
      prices.map((item) => {
        const material = productMap[item.productBasicInfoUuid];
        return {
          ...item,
          materialCode: material?.materialCode || "-",
          productName: material?.productName || "-",
          productModel: material?.productModel || "-",
          categoryName: categories.find((category) => category.id === material?.categoryId)?.name || "-",
          subcategoryName:
            subcategories.find((subcategory) => subcategory.id === material?.subcategoryId)?.name || "-",
          bandName: material?.bandCode || material?.frequency || "-",
          createUserLabel: item.createUser ? userMap[item.createUser] || item.createUser : "-",
        };
      }),
    [prices, productMap, categories, subcategories, userMap]
  );

  const supplierTypeIds = useMemo(() => {
    return new Set(
      customerTypes
        .filter((item) => item.typeName === "合作伙伴" || item.typeName === "供应商")
        .map((item) => item.id)
    );
  }, [customerTypes]);

  const supplierOptions = useMemo(
    () =>
      customers.filter(
        (item) => item.customerTypeId != null && supplierTypeIds.has(item.customerTypeId)
      ),
    [customers, supplierTypeIds]
  );

  const fetchData = async () => {
    setLoading(true);
    try {
      const [
        priceResponse,
        productResponse,
        categoryResponse,
        subcategoryResponse,
        bandResponse,
        customerResponse,
        customerTypeResponse,
        userResponse,
      ] = await Promise.all([
        productAPI.getAllProductPrices(),
        productAPI.getProducts(),
        productAPI.getProductCategories(),
        productAPI.getProductSubcategories(),
        productAPI.getProductBands(),
        customerAPI.getCustomerOptions(),
        customerAPI.getCustomerTypeOptions(),
        permissionAPI.getUserOptions(),
      ]);
      setPrices(normalizeResponseData(priceResponse));
      setProducts(normalizeResponseData(productResponse));
      setCategories(normalizeResponseData(categoryResponse));
      setSubcategories(normalizeResponseData(subcategoryResponse));
      setBands(normalizeResponseData(bandResponse));
      setCustomers(normalizeResponseData(customerResponse));
      setCustomerTypes(normalizeResponseData(customerTypeResponse));
      setUsers(Array.isArray(userResponse) ? userResponse : normalizeResponseData(userResponse));
    } catch (error) {
      message.error(error?.message || "加载物料价格数据失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreate = async (values) => {
    await productAPI.createProductPrice(values);
    await fetchData();
  };

  const handleUpdate = async (id, values) => {
    await productAPI.updateProductPrice(id, values);
    await fetchData();
  };

  const handleDelete = async (id) => {
    await productAPI.deleteProductPrice(id);
    await fetchData();
  };

  const mapRecordToFormValues = (record) => {
    const material = productMap[record.productBasicInfoUuid];
    return {
      ...record,
      categoryId: material?.categoryId ?? undefined,
      subcategoryId: material?.subcategoryId ?? undefined,
      selectedProductName: material?.productName ?? undefined,
      selectedProductModel: material ? (material.productModel || EMPTY_MODEL_SENTINEL) : undefined,
      selectedBandCode: material?.bandCode || material?.frequency || undefined,
      productBasicInfoUuid: material?.uuid ?? record.productBasicInfoUuid,
      supplierCustomerId: record.supplierCustomerId ?? undefined,
    };
  };

  const transformValues = (values) => ({
    productBasicInfoUuid: values.productBasicInfoUuid,
    referencePrice: values.referencePrice,
    supplierCustomerId: values.supplierCustomerId || null,
  });

  return (
    <CRUDTable
      title="物料价格记录"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        { title: "物料编码", dataIndex: "materialCode", key: "materialCode", width: 140 },
        { title: "大类", dataIndex: "categoryName", key: "categoryName", width: 140 },
        { title: "分项", dataIndex: "subcategoryName", key: "subcategoryName", width: 140 },
        { title: "名称", dataIndex: "productName", key: "productName", width: 220 },
        { title: "型号", dataIndex: "productModel", key: "productModel", width: 180 },
        { title: "频段", dataIndex: "bandName", key: "bandName", width: 120 },
        { title: "参考价", dataIndex: "referencePrice", key: "referencePrice", width: 140 },
        { title: "供应商", dataIndex: "supplier", key: "supplier", width: 220 },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 180,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
        { title: "创建人", dataIndex: "createUserLabel", key: "createUserLabel", width: 160 },
      ]}
      dataSource={decoratedPrices}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      rowKey="id"
      searchFields={[
        { name: "materialCode", label: "物料编码" },
        { name: "productName", label: "名称" },
        { name: "productModel", label: "型号" },
        { name: "supplier", label: "供应商" },
      ]}
      mapRecordToFormValues={mapRecordToFormValues}
      transformValues={transformValues}
      formFields={[
        {
          key: "materialCascade",
          renderOnly: true,
          render: ({ form, editingRecord }) => (
            <MaterialCascadeFields
              form={form}
              products={products}
              categories={categories}
              subcategories={subcategories}
              bands={bands}
              editingRecord={editingRecord}
            />
          ),
        },
        {
          name: "referencePrice",
          label: "参考价",
          rules: [{ required: true, message: "请输入参考价" }],
          component: <InputNumber min={0} precision={2} style={{ width: "100%" }} />,
        },
        {
          name: "supplierCustomerId",
          label: "供应商",
          rules: [{ required: true, message: "请选择供应商" }],
          component: ({ editingRecord }) => {
            const extraSupplier =
              editingRecord?.supplierCustomerId &&
              !supplierOptions.some((item) => item.uuid === editingRecord.supplierCustomerId)
                ? customers.find((item) => item.uuid === editingRecord.supplierCustomerId) || {
                    uuid: editingRecord.supplierCustomerId,
                    customerName: editingRecord.supplier || editingRecord.supplierCustomerId,
                  }
                : null;
            const mergedSuppliers = extraSupplier
              ? [extraSupplier, ...sortSelectItems(supplierOptions, ["customerCode"])]
              : sortSelectItems(supplierOptions, ["customerCode"]);
            return (
              <Select placeholder="请选择供应商" showSearch optionFilterProp="children">
                {mergedSuppliers.map((item) => (
                  <Option key={item.uuid} value={item.uuid}>
                    {item.customerName}
                  </Option>
                ))}
              </Select>
            );
          },
        },
      ]}
    />
  );
}
