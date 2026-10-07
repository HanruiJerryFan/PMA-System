import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import dayjs from "dayjs";
import {
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Tooltip,
  message,
} from "antd";
import { DatePicker } from "antd";
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FilePdfOutlined,
  LinkOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getCurrentUser } from "../../api/auth";
import BusinessAttachmentUpload from "../../components/Common/BusinessAttachmentUpload";
import { exportAPI, permissionAPI, productAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { resolvePagePermissions } from "../../utils/pagePermissions";
import { canMaintainEntryAuditUsers } from "../../utils/entryAudit";
import { uploadBusinessAttachments } from "../../utils/attachments";
import { downloadApiFile, downloadExcel, resolveBlobErrorMessage } from "../../utils/exporters";
import { sortSelectItems } from "../../utils/selectSorting";

const { Option } = Select;

const LIST_TYPE_OPTIONS = [
  { value: "INITIAL_SALES", label: "初始销售清单" },
  { value: "PROCUREMENT", label: "采购清单" },
  { value: "CHANGE", label: "销售变更清单" },
];

const IMPORT_MODE_OPTIONS = [
  { value: "PARTIAL_SUCCESS", label: "部分成功" },
  { value: "FAIL_FAST", label: "遇错即停" },
];

function normalizeResponseData(response) {
  return response?.data ?? response ?? [];
}

function getListTypeLabel(value) {
  return LIST_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || "-";
}

function isProcurementListType(value) {
  return value === "PROCUREMENT" || value === "FINAL_PROCUREMENT";
}

function getUnitPriceLabel(listType) {
  return isProcurementListType(listType) ? "采购价格" : "销售价格";
}

function getUserLabel(user) {
  return user?.realName || user?.username || (user?.id ? `用户${user.id}` : "");
}

function formatAmount(value) {
  if (value == null || value === "") return "-";
  return Number(value).toFixed(2);
}

function calculateItemAmount(item) {
  if (item?.totalAmount !== undefined && item?.totalAmount !== null && item?.totalAmount !== "") {
    return Number(Number(item.totalAmount).toFixed(2));
  }
  const quantity = Number(item?.quantity || 0);
  const unitPrice = Number(item?.unitPrice || 0);
  return Number((quantity * unitPrice).toFixed(2));
}

export default function ProjectLists() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const projectUuidFilter = searchParams.get("projectUuid") || "";
  const [currentUser, setCurrentUser] = useState(null);
  const [projects, setProjects] = useState([]);
  const [projectLists, setProjectLists] = useState([]);
  const [listItems, setListItems] = useState([]);
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [subcategories, setSubcategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedListUuid, setSelectedListUuid] = useState(null);
  const [loading, setLoading] = useState(false);
  const [itemsLoading, setItemsLoading] = useState(false);
  const [listModalVisible, setListModalVisible] = useState(false);
  const [editingList, setEditingList] = useState(null);
  const [pendingListAttachmentFiles, setPendingListAttachmentFiles] = useState([]);
  const [itemModalVisible, setItemModalVisible] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [importModalVisible, setImportModalVisible] = useState(false);
  const [pendingImportFile, setPendingImportFile] = useState(null);
  const [replaceExistingOnImport, setReplaceExistingOnImport] = useState(false);
  const [importMode, setImportMode] = useState("PARTIAL_SUCCESS");
  const [importing, setImporting] = useState(false);
  const [listForm] = Form.useForm();
  const [itemForm] = Form.useForm();
  const importInputRef = useRef(null);
  const listAttachmentInputRef = useRef(null);

  const pagePermissions = useMemo(() => resolvePagePermissions("/project/lists"), []);
  const canManage = hasAnyAuthority(currentUser, ["project.list.entry", ...pagePermissions.manageAuthorities]);
  const canAudit = hasAnyAuthority(currentUser, ["project.list.audit", ...pagePermissions.manageAuthorities]);
  const canMaintainEntryAudit = canMaintainEntryAuditUsers(currentUser);
  const canExport = hasAnyAuthority(currentUser, pagePermissions.exportAuthorities);
  const canAccessAttachments = hasAnyAuthority(currentUser, ["attachment.access"]);
  const canManageAttachments = hasAnyAuthority(currentUser, ["attachment.manage"]);
  const importDisabledReason = !canManage ? "当前账号没有项目管理权限" : !selectedListUuid ? "请先选择一个项目清单" : "";

  const selectedCategoryId = Form.useWatch("categoryId", itemForm);
  const selectedSubcategoryId = Form.useWatch("subcategoryId", itemForm);
  const selectedProductName = Form.useWatch("selectedProductName", itemForm);
  const selectedProductModel = Form.useWatch("selectedProductModel", itemForm);
  const selectedBandCode = Form.useWatch("selectedBandCode", itemForm);
  const selectedMaterialId = Form.useWatch("materialId", itemForm);

  const projectMap = useMemo(() => Object.fromEntries(projects.map((item) => [item.uuid, item])), [projects]);
  const productMap = useMemo(() => Object.fromEntries(products.map((item) => [item.uuid, item])), [products]);
  const brandMap = useMemo(() => Object.fromEntries(brands.map((item) => [item.id, item.name])), [brands]);
  const userMap = useMemo(() => Object.fromEntries(users.map((item) => [item.id, getUserLabel(item)])), [users]);
  const userOptions = useMemo(
    () =>
      users.map((item) => (
        <Option key={item.id} value={item.id}>
          {getUserLabel(item)}
        </Option>
      )),
    [users]
  );
  const activeCategories = useMemo(() => categories.filter((item) => item.isActive !== false), [categories]);
  const activeSubcategories = useMemo(() => subcategories.filter((item) => item.isActive !== false), [subcategories]);
  const currentProject = useMemo(() => (projectUuidFilter ? projectMap[projectUuidFilter] || null : null), [projectMap, projectUuidFilter]);

  const filteredSubcategories = useMemo(() => activeSubcategories, [activeSubcategories]);

  const filteredProducts = useMemo(
    () => products.filter((item) => item.isActive !== false && (selectedCategoryId == null || Number(item.categoryId) === Number(selectedCategoryId)) && (selectedSubcategoryId == null || Number(item.subcategoryId) === Number(selectedSubcategoryId))),
    [products, selectedCategoryId, selectedSubcategoryId]
  );

  const productNameOptions = useMemo(() => [...new Set(filteredProducts.map((item) => item.productName).filter(Boolean))], [filteredProducts]);
  const filteredByName = useMemo(() => filteredProducts.filter((item) => !selectedProductName || item.productName === selectedProductName), [filteredProducts, selectedProductName]);
  const productModelOptions = useMemo(() => [...new Set(filteredByName.map((item) => item.productModel || ""))], [filteredByName]);
  const filteredByModel = useMemo(() => filteredByName.filter((item) => !selectedProductModel || (item.productModel || "") === selectedProductModel), [filteredByName, selectedProductModel]);
  const bandOptions = useMemo(() => [...new Set(filteredByModel.map((item) => item.bandCode || item.frequency || "").filter(Boolean))], [filteredByModel]);
  const materialOptions = useMemo(() => filteredByModel.filter((item) => !selectedBandCode || (item.bandCode || item.frequency || "") === selectedBandCode), [filteredByModel, selectedBandCode]);
  const selectedMaterial = useMemo(() => selectedMaterialId ? productMap[selectedMaterialId] || null : null, [productMap, selectedMaterialId]);

  const decoratedLists = useMemo(() => projectLists.map((item) => ({
    ...item,
    projectLabel: projectMap[item.projectId]?.projectName || item.projectId,
    listTypeLabel: getListTypeLabel(item.listType),
    entryUserName: item.entryUserName || userMap[item.entryUser] || "",
    auditorUserName: item.auditorUserName || userMap[item.auditorUser] || "",
  })), [projectLists, projectMap, userMap]);

  const selectedList = useMemo(() => decoratedLists.find((item) => item.uuid === selectedListUuid) || null, [decoratedLists, selectedListUuid]);
  const selectedUnitPriceLabel = useMemo(() => getUnitPriceLabel(selectedList?.listType), [selectedList?.listType]);
  const decoratedListItems = useMemo(() => listItems.map((item) => ({
    ...item,
    entryUserName: item.entryUserName || userMap[item.entryUser] || "",
    auditorUserName: item.auditorUserName || userMap[item.auditorUser] || "",
  })), [listItems, userMap]);

  const summary = useMemo(() => decoratedLists.reduce((acc, item) => {
    acc.total += 1;
    if (item.listType === "INITIAL_SALES") acc.initial += 1;
    if (item.listType === "PROCUREMENT") acc.procurement += 1;
    if (item.listType === "CHANGE") acc.change += 1;
    return acc;
  }, { total: 0, initial: 0, procurement: 0, change: 0 }), [decoratedLists]);

  const itemSummary = useMemo(() => decoratedListItems.reduce((acc, item) => {
    acc.quantity += Number(item.quantity || 0);
    acc.amount += Number(item.totalAmount || 0);
    return acc;
  }, { quantity: 0, amount: 0 }), [decoratedListItems]);

  const selectedListAttachmentStatus = selectedList?.pdfAttachmentId ? "已上传 PDF 附件" : "未上传 PDF 附件";

  const fetchProjectLists = useCallback(async () => {
    setLoading(true);
    try {
      const response = projectUuidFilter ? await projectAPI.getProjectListsByProject(projectUuidFilter) : await projectAPI.getProjectLists();
      const data = normalizeResponseData(response);
      setProjectLists(data);
      setSelectedListUuid((current) => (current && data.some((item) => item.uuid === current) ? current : data[0]?.uuid || null));
    } finally {
      setLoading(false);
    }
  }, [projectUuidFilter]);

  const fetchOptions = useCallback(async () => {
    const [projectResponse, productResponse, categoryResponse, subcategoryResponse, brandResponse, userResponse] = await Promise.all([
      projectAPI.getProjectOptions(),
      productAPI.getProductOptions(),
      productAPI.getProductCategoryOptions(),
      productAPI.getProductSubcategoryOptions(),
      productAPI.getProductBrandOptions(),
      permissionAPI.getUserOptions().catch(() => []),
    ]);
    setProjects(normalizeResponseData(projectResponse));
    setProducts(normalizeResponseData(productResponse));
    setCategories(normalizeResponseData(categoryResponse));
    setSubcategories(normalizeResponseData(subcategoryResponse));
    setBrands(normalizeResponseData(brandResponse));
    setUsers(Array.isArray(userResponse) ? userResponse : normalizeResponseData(userResponse));
  }, []);

  const fetchItems = useCallback(async (projectListId) => {
    if (!projectListId) {
      setListItems([]);
      return;
    }
    setItemsLoading(true);
    try {
      const response = await projectAPI.getProjectListItems(projectListId);
      setListItems(normalizeResponseData(response));
    } finally {
      setItemsLoading(false);
    }
  }, []);

  useEffect(() => {
    (async () => {
      try { setCurrentUser(await getCurrentUser()); } catch { setCurrentUser(null); }
    })();
    fetchProjectLists();
    fetchOptions();
  }, [fetchOptions, fetchProjectLists]);

  useEffect(() => { fetchItems(selectedListUuid); }, [fetchItems, selectedListUuid]);

  const openListModal = (record = null) => {
    if (!canManage) return message.error("没有维护项目清单的权限");
    setEditingList(record);
    setPendingListAttachmentFiles([]);
    if (listAttachmentInputRef.current) {
      listAttachmentInputRef.current.value = "";
    }
    listForm.setFieldsValue({
      projectId: record?.projectId || projectUuidFilter || undefined,
      listName: record?.listName || "",
      listType: record?.listType || "INITIAL_SALES",
      entryDate: record?.entryDate ? dayjs(record.entryDate) : null,
      entryUser: record?.entryUser || undefined,
      auditorUser: record?.auditorUser || undefined,
    });
    setListModalVisible(true);
  };

  const closeListModal = () => {
    setListModalVisible(false);
    setEditingList(null);
    setPendingListAttachmentFiles([]);
    listForm.resetFields();
    if (listAttachmentInputRef.current) {
      listAttachmentInputRef.current.value = "";
    }
  };

  const uploadProjectListAttachments = async (projectListUuid) => {
    if (!pendingListAttachmentFiles.length || !projectListUuid) {
      return [];
    }
    return uploadBusinessAttachments("project-lists", projectListUuid, pendingListAttachmentFiles);
  };

  const submitList = async () => {
    try {
      const values = await listForm.validateFields();
      const payload = {
        projectId: values.projectId || projectUuidFilter || null,
        listName: values.listName,
        listType: values.listType,
        entryDate: values.entryDate ? values.entryDate.format("YYYY-MM-DD") : null,
        pdfAttachmentId: editingList?.pdfAttachmentId || null,
      };
      if (editingList && canMaintainEntryAudit) {
        payload.entryUser = values.entryUser ?? null;
        payload.auditorUser = values.auditorUser ?? null;
      }
      if (editingList) {
        let nextPayload = payload;
        const uploadedAttachments = await uploadProjectListAttachments(editingList.uuid);
        if (uploadedAttachments[0]?.uuid) {
          nextPayload = { ...payload, pdfAttachmentId: uploadedAttachments[0].uuid };
        }
        await projectAPI.updateProjectList(editingList.uuid, nextPayload);
        message.success(pendingListAttachmentFiles.length ? "项目清单及附件已更新" : "项目清单已更新");
      } else {
        const created = normalizeResponseData(await projectAPI.createProjectList(payload));
        if (pendingListAttachmentFiles.length) {
          const uploadedAttachments = await uploadProjectListAttachments(created.uuid);
          await projectAPI.updateProjectList(created.uuid, {
            ...payload,
            pdfAttachmentId: uploadedAttachments[0]?.uuid || null,
          });
        }
        message.success(pendingListAttachmentFiles.length ? "项目清单及附件已新增" : "项目清单已新增");
      }
      closeListModal();
      await fetchProjectLists();
    } catch (error) {
      if (!error?.errorFields) message.error(error?.message || "保存项目清单失败");
    }
  };

  const auditList = async (uuid) => {
    if (!canAudit) {
      message.error("没有审核项目清单的权限");
      return;
    }
    await projectAPI.auditProjectList(uuid);
    message.success("项目清单已审核");
    await fetchProjectLists();
  };

  const deleteList = async (uuid) => {
    if (!canManage) return message.error("没有维护项目清单的权限");
    await projectAPI.deleteProjectList(uuid);
    message.success("删除成功");
    await fetchProjectLists();
  };

  const openProjectDashboard = () => {
    if (!projectUuidFilter) return;
    navigate(`/project/dashboard?projectUuid=${projectUuidFilter}`);
  };

  const openProjectListDashboard = (record = null) => {
    const target = record || selectedList;
    if (!target?.uuid) {
      message.warning("请先选择一个项目清单");
      return;
    }
    const from = projectUuidFilter ? "lists-project" : "lists-all";
    navigate(`/project/list-dashboard?projectUuid=${target.projectId || projectUuidFilter}&listUuid=${target.uuid}&from=${from}`);
  };

  const openAggregateDashboard = (aggregateType) => {
    const targetProjectId = projectUuidFilter || selectedList?.projectId;
    if (!targetProjectId) {
      message.warning("请先进入某个项目，再查看最终清单");
      return;
    }
    const from = projectUuidFilter ? "lists-project" : "lists-all";
    navigate(`/project/list-dashboard?projectUuid=${targetProjectId}&aggregateType=${aggregateType}&from=${from}`);
  };

  const openItemModal = (record = null) => {
    if (!canManage) return message.error("没有维护项目清单明细的权限");
    if (!selectedListUuid) return message.warning("请先选择一个项目清单");
    setEditingItem(record);
    itemForm.resetFields();
    if (record) {
      const material = record.materialId ? productMap[record.materialId] : null;
      itemForm.setFieldsValue({
        ...record,
        categoryId: material?.categoryId ?? null,
        subcategoryId: material?.subcategoryId ?? null,
        selectedProductName: material?.productName ?? record.itemName ?? null,
        selectedProductModel: material?.productModel ?? record.model ?? null,
        selectedBandCode: material?.bandCode ?? material?.frequency ?? null,
        materialId: material?.uuid ?? record.materialId ?? null,
      });
    }
    setItemModalVisible(true);
  };

  const clearItemSelection = (names) => {
    const patch = {};
    names.forEach((name) => { patch[name] = undefined; });
    itemForm.setFieldsValue(patch);
  };

  const handleMaterialChange = (materialUuid) => {
    const material = productMap[materialUuid];
    if (!material) return;
    itemForm.setFieldsValue({
      materialId: material.uuid,
      materialCode: material.materialCode,
      itemName: material.productName,
      model: material.productModel,
      brand: brandMap[material.brandId] || material.brand || material.brandCode,
      unit: material.unit,
    });
  };

  const submitItem = async () => {
    if (!canManage) return message.error("没有维护项目清单明细的权限");
    try {
      const values = await itemForm.validateFields();
      const payload = {
        projectListId: selectedListUuid,
        materialId: values.materialId,
        quantity: Number(values.quantity),
        unitPrice: values.unitPrice == null ? null : Number(values.unitPrice),
        remark: values.remark || null,
      };
      if (editingItem && canMaintainEntryAudit) {
        payload.entryUser = values.entryUser ?? null;
        payload.auditorUser = values.auditorUser ?? null;
      }
      if (editingItem) {
        await projectAPI.updateProjectListItem(editingItem.uuid, payload);
        message.success("保存成功");
      } else {
        await projectAPI.createProjectListItem(payload);
        message.success("新增成功");
      }
      setItemModalVisible(false);
      await fetchItems(selectedListUuid);
    } catch (error) {
      if (!error?.errorFields) message.error(error?.message || "保存清单明细失败");
    }
  };

  const auditItem = async (uuid) => {
    if (!canAudit) {
      message.error("没有审核项目清单明细的权限");
      return;
    }
    await projectAPI.auditProjectListItem(uuid);
    message.success("项目清单明细已审核");
    await fetchItems(selectedListUuid);
  };

  const deleteItem = async (uuid) => {
    if (!canManage) return message.error("没有维护项目清单明细的权限");
    await projectAPI.deleteProjectListItem(uuid);
    await fetchItems(selectedListUuid);
    message.success("删除成功");
  };

  const downloadTemplate = async () => {
    try {
      await downloadApiFile(
        projectAPI.downloadProjectListItemsImportTemplate(),
        "project-list-items-import-template.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      );
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "下载模板失败"));
    }
  };

  const startImport = async () => {
    if (!pendingImportFile || !selectedListUuid) return;
    const formData = new FormData();
    formData.append("file", pendingImportFile);
    setImporting(true);
    try {
      await projectAPI.importProjectListItems(selectedListUuid, formData, replaceExistingOnImport, importMode);
      message.success("导入完成");
      setImportModalVisible(false);
      setPendingImportFile(null);
      if (importInputRef.current) importInputRef.current.value = "";
      await fetchItems(selectedListUuid);
    } catch (error) {
      message.error(error?.message || "导入失败");
    } finally {
      setImporting(false);
    }
  };

  const exportExcel = () => {
    if (!selectedList) return message.warning("请先选择一个项目清单");
    const rows = decoratedListItems.map((item) => [
      item.materialCode || "",
      item.itemName || "",
      item.model || "",
      item.brand || "",
      item.unit || "",
      Number(item.quantity || 0),
      Number(item.unitPrice || 0),
      calculateItemAmount(item),
      item.remark || "",
      item.entryUserName || "",
      item.auditorUserName || "",
    ]);
    const totalAmount = rows.reduce((sum, row) => sum + Number(row[7] || 0), 0);
    downloadExcel(
      `${selectedList.projectLabel || "项目"}-${selectedList.listName || selectedList.listTypeLabel || "清单"}.xls`,
      "项目清单明细",
      ["物料编码", "物料名称", "型号", "品牌", "单位", "数量", selectedUnitPriceLabel, "金额", "备注", "录入人", "审核人"],
      [...rows, ["", "", "", "", "合计", "", "", Number(totalAmount.toFixed(2)), "", "", ""]]
    );
  };

  const exportPdf = async () => {
    if (!selectedList) return message.warning("请先选择一个项目清单");
    const totalAmount = decoratedListItems.reduce((sum, item) => sum + calculateItemAmount(item), 0);
    const payload = {
      title: "项目清单明细",
      subtitle: `生成时间：${dayjs().format("YYYY-MM-DD HH:mm")}`,
      fileName: `${selectedList.projectLabel || "项目"}-${selectedList.listName || selectedList.listTypeLabel || "清单"}.pdf`,
      metadata: [
        { label: "项目", value: selectedList.projectLabel || "-" },
        { label: "清单名称", value: selectedList.listName || "-" },
        { label: "客户", value: selectedList.customerName || "-" },
        { label: "清单类型", value: selectedList.listTypeLabel || "-" },
        { label: "录入日期", value: selectedList.entryDate ? dayjs(selectedList.entryDate).format("YYYY-MM-DD") : "-" },
        { label: "录入人", value: selectedList.entryUserName || "-" },
        { label: "审核人", value: selectedList.auditorUserName || "-" },
      ],
      summaries: [{ label: "合计金额", value: formatAmount(totalAmount) }],
      columns: [
        { header: "物料编码", width: 12 },
        { header: "物料名称", width: 16 },
        { header: "型号", width: 13 },
        { header: "品牌", width: 9 },
        { header: "单位", align: "center", width: 6 },
        { header: "数量", align: "right", width: 7 },
        { header: selectedUnitPriceLabel, align: "right", width: 9 },
        { header: "金额", align: "right", width: 9 },
        { header: "备注", width: 12 },
        { header: "录入人", width: 8 },
        { header: "审核人", width: 8 },
      ],
      rows: decoratedListItems.map((item) => [
        item.materialCode || "",
        item.itemName || "",
        item.model || "",
        item.brand || "",
        item.unit || "",
        item.quantity || "",
        formatAmount(item.unitPrice),
        formatAmount(calculateItemAmount(item)),
        item.remark || "",
        item.entryUserName || "",
        item.auditorUserName || "",
      ]),
    };
    try {
      await downloadApiFile(exportAPI.downloadTablePdf(payload), payload.fileName, "application/pdf");
    } catch (error) {
      message.error(await resolveBlobErrorMessage(error, "导出 PDF 失败"));
    }
  };

  const listColumns = [
    {
      title: "操作",
      key: "action",
      width: 300,
      fixed: "left",
      render: (_, record) => (
        <Space wrap>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={(event) => {
              event.stopPropagation();
              openProjectListDashboard(record);
            }}
          >
            当前清单总览
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            disabled={!canManage}
            onClick={(event) => {
              event.stopPropagation();
              openListModal(record);
            }}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<CheckCircleOutlined />}
            disabled={!canAudit || Boolean(record.auditorUser)}
            onClick={(event) => {
              event.stopPropagation();
              auditList(record.uuid);
            }}
          >
            审核
          </Button>
          <Popconfirm title="确定删除这个项目清单吗？" onConfirm={() => deleteList(record.uuid)} okText="确定" cancelText="取消">
            <Button
              type="link"
              danger
              disabled={!canManage}
              icon={<DeleteOutlined />}
              onClick={(event) => event.stopPropagation()}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
    { title: "项目", dataIndex: "projectLabel", key: "projectLabel", width: 220 },
    { title: "清单名称", dataIndex: "listName", key: "listName", width: 180 },
    { title: "清单类型", dataIndex: "listTypeLabel", key: "listTypeLabel", width: 140, render: (value, record) => <Tag color={record.listType === "INITIAL_SALES" ? "blue" : record.listType === "PROCUREMENT" ? "green" : "orange"}>{value}</Tag> },
    { title: "客户", dataIndex: "customerName", key: "customerName", width: 220 },
    { title: "录入日期", dataIndex: "entryDate", key: "entryDate", width: 120, render: (value) => (value ? dayjs(value).format("YYYY-MM-DD") : "-") },
    { title: "录入人", dataIndex: "entryUserName", key: "entryUserName", width: 120, render: (value) => value || "-" },
    { title: "审核人", dataIndex: "auditorUserName", key: "auditorUserName", width: 120, render: (value) => value || "-" },
    {
      title: "PDF附件",
      dataIndex: "pdfAttachmentId",
      key: "pdfAttachmentId",
      width: 120,
      render: (value) => (value ? <Tag color="green">已上传</Tag> : <Tag>未上传</Tag>),
    },
  ];

  const itemColumns = [
    {
      title: "操作",
      key: "action",
      width: 260,
      fixed: "left",
      render: (_, record) => (
        <Space wrap>
          <Button type="link" icon={<EditOutlined />} disabled={!canManage} onClick={() => openItemModal(record)}>
            编辑
          </Button>
          <Button type="link" icon={<CheckCircleOutlined />} disabled={!canAudit || Boolean(record.auditorUser)} onClick={() => auditItem(record.uuid)}>
            审核
          </Button>
          <Popconfirm title="确定删除这条清单明细吗？" onConfirm={() => deleteItem(record.uuid)} okText="确定" cancelText="取消">
            <Button type="link" danger disabled={!canManage} icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
    { title: "物料编码", dataIndex: "materialCode", key: "materialCode", width: 140 },
    { title: "物料名称", dataIndex: "itemName", key: "itemName", width: 220 },
    { title: "型号", dataIndex: "model", key: "model", width: 180 },
    { title: "品牌", dataIndex: "brand", key: "brand", width: 120 },
    { title: "单位", dataIndex: "unit", key: "unit", width: 100 },
    { title: "数量", dataIndex: "quantity", key: "quantity", width: 120 },
    { title: selectedUnitPriceLabel, dataIndex: "unitPrice", key: "unitPrice", width: 120, render: formatAmount },
    { title: "金额", dataIndex: "totalAmount", key: "totalAmount", width: 120, render: formatAmount },
    { title: "备注", dataIndex: "remark", key: "remark", width: 220 },
    { title: "录入人", dataIndex: "entryUserName", key: "entryUserName", width: 120, render: (value) => value || "-" },
    { title: "审核人", dataIndex: "auditorUserName", key: "auditorUserName", width: 120, render: (value) => value || "-" },
  ];

  return (
    <Card title={projectUuidFilter ? "当前项目清单管理" : "项目清单管理"}>
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        {projectUuidFilter ? (
          <Card
            size="small"
            title="当前项目上下文"
            extra={
              <Space wrap>
                <Button icon={<ArrowLeftOutlined />} onClick={openProjectDashboard}>
                  返回项目总览
                </Button>
                <Button icon={<EyeOutlined />} disabled={!selectedListUuid} onClick={() => openProjectListDashboard(selectedList)}>
                  当前清单总览
                </Button>
                <Button icon={<EyeOutlined />} onClick={() => openAggregateDashboard("FINAL_ALL")}>
                  最终清单总览
                </Button>
              </Space>
            }
          >
            <Descriptions size="small" column={{ xs: 1, md: 2, xl: 4 }}>
              <Descriptions.Item label="项目名称">{currentProject?.projectName || "-"}</Descriptions.Item>
              <Descriptions.Item label="项目编号">{currentProject?.projectNumber || currentProject?.projectCode || "-"}</Descriptions.Item>
              <Descriptions.Item label="当前选中清单">{selectedList?.listName || "未选择"}</Descriptions.Item>
              <Descriptions.Item label="清单类型">{selectedList?.listTypeLabel || "-"}</Descriptions.Item>
            </Descriptions>
          </Card>
        ) : null}

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} xl={6}><Card><Statistic title="清单总数" value={summary.total} /></Card></Col>
          <Col xs={24} sm={12} xl={6}><Card><Statistic title="初始销售清单" value={summary.initial} /></Card></Col>
          <Col xs={24} sm={12} xl={6}><Card><Statistic title="采购清单" value={summary.procurement} /></Card></Col>
          <Col xs={24} sm={12} xl={6}><Card><Statistic title="销售变更清单" value={summary.change} /></Card></Col>
        </Row>

        <Card
          title={projectUuidFilter ? "当前项目清单" : "项目清单"}
          extra={
            <Space wrap>
              <input ref={importInputRef} type="file" accept=".xlsx,.xls" style={{ display: "none" }} onChange={(e) => { const file = e.target.files?.[0]; if (file) { setPendingImportFile(file); setImportModalVisible(true); } }} />
              <Button type="primary" icon={<PlusOutlined />} disabled={!canManage} onClick={() => openListModal()}>
                新增清单
              </Button>
              <Button icon={<EyeOutlined />} disabled={!projectUuidFilter && !selectedList?.projectId} onClick={() => openAggregateDashboard("FINAL_ALL")}>
                最终清单总览
              </Button>
              <Tooltip title={importDisabledReason}>
                <span>
                  <Button disabled={Boolean(importDisabledReason)} onClick={() => importInputRef.current?.click()}>
                    导入 Excel
                  </Button>
                </span>
              </Tooltip>
              <Button disabled={!canManage} onClick={downloadTemplate}>
                下载模板
              </Button>
              <Button icon={<DownloadOutlined />} disabled={!canExport || !selectedListUuid} onClick={exportExcel}>
                导出 Excel
              </Button>
              <Button icon={<FilePdfOutlined />} disabled={!canExport || !selectedListUuid} onClick={exportPdf}>
                导出 PDF
              </Button>
            </Space>
          }
        >
          <Table rowKey="uuid" loading={loading} dataSource={decoratedLists} columns={listColumns} pagination={{ pageSize: 8 }} scroll={{ x: "max-content" }} rowClassName={(record) => (record.uuid === selectedListUuid ? "ant-table-row-selected" : "")} onRow={(record) => ({ onClick: () => setSelectedListUuid(record.uuid), style: { cursor: "pointer" } })} />
        </Card>

        <Card
          title={selectedList ? `清单明细：${selectedList.listName || selectedList.projectLabel} / ${selectedList.listTypeLabel}` : "清单明细"}
          extra={
            <Space wrap>
              <Button icon={<EyeOutlined />} disabled={!selectedListUuid} onClick={() => openProjectListDashboard(selectedList)}>
                当前清单总览
              </Button>
              <Button icon={<LinkOutlined />} disabled={!selectedListUuid || !canAccessAttachments} onClick={() => navigate(`/attachment/center?businessType=project-lists&businessUuid=${selectedListUuid}`)}>
                清单附件
              </Button>
              <Statistic title="总数量" value={itemSummary.quantity} precision={4} />
              <Statistic title="总金额" value={itemSummary.amount} precision={2} />
              <Button type="primary" icon={<PlusOutlined />} disabled={!selectedListUuid || !canManage} onClick={() => openItemModal()}>
                新增明细
              </Button>
            </Space>
          }
        >
          {selectedListUuid ? (
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <Descriptions bordered size="small" column={{ xs: 1, md: 2, xl: 4 }}>
                <Descriptions.Item label="项目">{selectedList?.projectLabel || "-"}</Descriptions.Item>
                <Descriptions.Item label="清单名称">{selectedList?.listName || "-"}</Descriptions.Item>
                <Descriptions.Item label="清单类型">{selectedList?.listTypeLabel || "-"}</Descriptions.Item>
                <Descriptions.Item label="录入日期">{selectedList?.entryDate ? dayjs(selectedList.entryDate).format("YYYY-MM-DD") : "-"}</Descriptions.Item>
                <Descriptions.Item label="录入人">{selectedList?.entryUserName || "-"}</Descriptions.Item>
                <Descriptions.Item label="审核人">{selectedList?.auditorUserName || "-"}</Descriptions.Item>
                <Descriptions.Item label="客户">{selectedList?.customerName || "-"}</Descriptions.Item>
                <Descriptions.Item label="附件状态">{selectedListAttachmentStatus}</Descriptions.Item>
                <Descriptions.Item label="当前明细数">{decoratedListItems.length}</Descriptions.Item>
                <Descriptions.Item label="当前选择状态">已选中，可继续维护明细与附件</Descriptions.Item>
              </Descriptions>
              <Table rowKey="uuid" loading={itemsLoading} dataSource={decoratedListItems} columns={itemColumns} pagination={false} scroll={{ x: "max-content" }} />
            </Space>
          ) : (
            <Empty description={projectUuidFilter ? "当前项目下还没有选中清单，请先在上方列表选择一张清单。" : "请先选择一个项目清单，再查看对应明细。"} />
          )}
        </Card>

        <Modal title={editingList ? "编辑项目清单" : "新增项目清单"} open={listModalVisible} onOk={submitList} onCancel={closeListModal} okText="保存" cancelText="取消" destroyOnHidden>
          <Form form={listForm} layout="vertical">
            <Form.Item name="projectId" label="项目" rules={projectUuidFilter ? [] : [{ required: true, message: "请选择项目" }]}>
              <Select placeholder="请选择项目" showSearch optionFilterProp="children" disabled={Boolean(projectUuidFilter)}>{sortSelectItems(projects, ["projectNumber"]).map((item) => <Option key={item.uuid} value={item.uuid}>{item.projectName}</Option>)}</Select>
            </Form.Item>
            <Form.Item name="listName" label="清单名称" rules={[{ required: true, message: "请输入清单名称" }]}>
              <Input maxLength={128} placeholder="例如：一期采购清单" />
            </Form.Item>
            <Form.Item name="listType" label="清单类型" rules={[{ required: true, message: "请选择清单类型" }]}>
              <Select>{LIST_TYPE_OPTIONS.map((item) => <Option key={item.value} value={item.value}>{item.label}</Option>)}</Select>
            </Form.Item>
            <Form.Item name="entryDate" label="录入日期"><DatePicker style={{ width: "100%" }} /></Form.Item>
            {editingList && canMaintainEntryAudit ? (
              <>
                <Form.Item name="entryUser" label="录入人">
                  <Select placeholder="请选择录入人" allowClear showSearch optionFilterProp="children">
                    {userOptions}
                  </Select>
                </Form.Item>
                <Form.Item name="auditorUser" label="审核人">
                  <Select placeholder="请选择审核人" allowClear showSearch optionFilterProp="children">
                    {userOptions}
                  </Select>
                </Form.Item>
              </>
            ) : null}
            <Form.Item label="PDF附件">
              <BusinessAttachmentUpload
                title="PDF附件"
                businessType="project-lists"
                businessUuid={editingList?.uuid}
                pendingFiles={pendingListAttachmentFiles}
                onPendingFilesChange={setPendingListAttachmentFiles}
                inputRef={listAttachmentInputRef}
                canAccess={canAccessAttachments}
                canManage={canManageAttachments}
                chooseText={editingList ? "继续添加 PDF 附件" : "选择 PDF 附件"}
                helpText="保存项目清单时会自动上传并关联当前清单；第一份新上传文件会作为主 PDF 附件。"
                noManageText="当前账号没有附件上传权限，无法在这里上传 PDF 附件。"
                onOpenAttachments={editingList ? () => navigate(`/attachment/center?businessType=project-lists&businessUuid=${editingList.uuid}`) : undefined}
              />
            </Form.Item>
          </Form>
        </Modal>

        <Modal title={editingItem ? "编辑清单明细" : "新增清单明细"} open={itemModalVisible} onOk={submitItem} onCancel={() => { setItemModalVisible(false); setEditingItem(null); itemForm.resetFields(); }} okText="保存" cancelText="取消" width={860} destroyOnHidden>
          <Form form={itemForm} layout="vertical" preserve={false}>
            <Row gutter={16}>
              <Col span={8}><Form.Item name="categoryId" label="大类" rules={[{ required: true, message: "请选择大类" }]}><Select showSearch optionFilterProp="children" onChange={() => clearItemSelection(["subcategoryId", "selectedProductName", "selectedProductModel", "selectedBandCode", "materialId", "materialCode", "itemName", "model", "brand", "unit"])}>{sortSelectItems(activeCategories, ["sortOrder","code"]).map((item) => <Option key={item.id} value={item.id}>{item.name}</Option>)}</Select></Form.Item></Col>
              <Col span={8}><Form.Item name="subcategoryId" label="分项" rules={[{ required: true, message: "请选择分项" }]}><Select showSearch optionFilterProp="children" onChange={() => clearItemSelection(["selectedProductName", "selectedProductModel", "selectedBandCode", "materialId", "materialCode", "itemName", "model", "brand", "unit"])}>{sortSelectItems(filteredSubcategories, ["sortOrder","code"]).map((item) => <Option key={item.id} value={item.id}>{item.name}</Option>)}</Select></Form.Item></Col>
              <Col span={8}><Form.Item name="selectedProductName" label="名称" rules={[{ required: true, message: "请选择名称" }]}><Select showSearch optionFilterProp="children" onChange={() => clearItemSelection(["selectedProductModel", "selectedBandCode", "materialId", "materialCode", "itemName", "model", "brand", "unit"])}>{productNameOptions.map((name) => <Option key={name} value={name}>{name}</Option>)}</Select></Form.Item></Col>
            </Row>
            <Row gutter={16}>
              <Col span={8}><Form.Item name="selectedProductModel" label="型号" rules={[{ required: true, message: "请选择型号" }]}><Select showSearch optionFilterProp="children" onChange={() => clearItemSelection(["selectedBandCode", "materialId", "materialCode", "itemName", "model", "brand", "unit"])}>{sortSelectItems(productModelOptions).map((model) => <Option key={model || "_empty"} value={model}>{model || "未填写型号"}</Option>)}</Select></Form.Item></Col>
              <Col span={8}><Form.Item name="selectedBandCode" label="频段" rules={[{ required: true, message: "请选择频段" }]}><Select showSearch optionFilterProp="children" onChange={() => clearItemSelection(["materialId", "materialCode", "itemName", "model", "brand", "unit"])}>{sortSelectItems(bandOptions).map((band) => <Option key={band} value={band}>{band}</Option>)}</Select></Form.Item></Col>
              <Col span={8}><Form.Item name="materialId" label="物料" rules={[{ required: true, message: "请选择物料" }]}><Select showSearch optionFilterProp="children" onChange={handleMaterialChange}>{sortSelectItems(materialOptions, ["materialCode"]).map((item) => <Option key={item.uuid} value={item.uuid}>{item.materialCode} / {item.productModel || "未填写型号"}</Option>)}</Select></Form.Item></Col>
            </Row>
            <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="物料编码">{selectedMaterial?.materialCode || "-"}</Descriptions.Item>
                <Descriptions.Item label="品牌">{brandMap[selectedMaterial?.brandId] || selectedMaterial?.brand || selectedMaterial?.brandCode || "-"}</Descriptions.Item>
              <Descriptions.Item label="单位">{selectedMaterial?.unit || "-"}</Descriptions.Item>
              <Descriptions.Item label="规格">{selectedMaterial?.specification || "-"}</Descriptions.Item>
            </Descriptions>
            <Row gutter={16}>
              <Col span={8}><Form.Item name="quantity" label="数量" rules={[{ required: true, message: "请输入数量" }]}><InputNumber min={selectedList?.listType === "CHANGE" ? undefined : 0.0001} precision={4} style={{ width: "100%" }} placeholder={selectedList?.listType === "CHANGE" ? "销售变更清单可输入正数或负数" : "请输入数量"} /></Form.Item></Col>
              <Col span={8}><Form.Item name="unitPrice" label={selectedUnitPriceLabel}><InputNumber min={0} precision={2} style={{ width: "100%" }} /></Form.Item></Col>
              <Col span={8}><Form.Item name="remark" label="备注"><Input placeholder="请输入备注" /></Form.Item></Col>
            </Row>
            {editingItem && canMaintainEntryAudit ? (
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="entryUser" label="录入人">
                    <Select placeholder="请选择录入人" allowClear showSearch optionFilterProp="children">
                      {userOptions}
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="auditorUser" label="审核人">
                    <Select placeholder="请选择审核人" allowClear showSearch optionFilterProp="children">
                      {userOptions}
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
            ) : null}
            <Form.Item name="materialCode" hidden><Input /></Form.Item>
            <Form.Item name="itemName" hidden><Input /></Form.Item>
            <Form.Item name="model" hidden><Input /></Form.Item>
            <Form.Item name="brand" hidden><Input /></Form.Item>
            <Form.Item name="unit" hidden><Input /></Form.Item>
          </Form>
        </Modal>

        <Modal title="导入项目清单明细" open={importModalVisible} onOk={startImport} onCancel={() => { setImportModalVisible(false); setPendingImportFile(null); if (importInputRef.current) importInputRef.current.value = ""; }} okText="开始导入" cancelText="取消" confirmLoading={importing} destroyOnHidden>
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <div><strong>当前文件：</strong> {pendingImportFile?.name || "-"}</div>
            <div>支持导入字段：物料编码、物料名称、型号、品牌、单位、数量、销售价格/采购价格/单价、备注。物料编码或物料名称至少填写一项。</div>
            <Space><span>导入前覆盖现有明细</span><Switch checked={replaceExistingOnImport} onChange={setReplaceExistingOnImport} /></Space>
            <div><div style={{ marginBottom: 8 }}>导入模式</div><Select value={importMode} onChange={setImportMode} style={{ width: "100%" }}>{IMPORT_MODE_OPTIONS.map((item) => <Option key={item.value} value={item.value}>{item.label}</Option>)}</Select></div>
          </Space>
        </Modal>
      </Space>
    </Card>
  );
}
