import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import dayjs from "dayjs";
import {
  Button,
  Cascader,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Popover,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Timeline,
  message,
} from "antd";
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  LinkOutlined,
  PlusOutlined,
  ProfileOutlined,
  UnorderedListOutlined,
} from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getCurrentUser } from "../../api/auth";
import BusinessAttachmentUpload from "../../components/Common/BusinessAttachmentUpload";
import {
  attachmentAPI,
  contractAPI,
  customerAPI,
  financeAPI,
  permissionAPI,
  projectAPI,
  regionAPI,
} from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { uploadBusinessAttachments } from "../../utils/attachments";
import { buildRegionOptions, buildRegionPath, formatRegionLabel } from "../../utils/regions";

const { Option } = Select;

const LIST_TYPE_OPTIONS = [
  { value: "INITIAL_SALES", label: "初始销售清单" },
  { value: "PROCUREMENT", label: "采购清单" },
  { value: "CHANGE", label: "销售变更清单" },
];

const LEVEL_1_OPTIONS = [
  { value: "NON_PROJECT", label: "非项目" },
  { value: "PROJECT", label: "项目" },
];

const LEVEL_2_OPTIONS = [
  { value: "EQUIPMENT_PURCHASE", label: "设备采购" },
  { value: "AUXILIARY_MATERIAL_PURCHASE", label: "辅材采购" },
  { value: "CONSTRUCTION_FEE", label: "施工费" },
  { value: "SALES_EXPENSE", label: "销售费用" },
  { value: "MISCELLANEOUS", label: "杂项支出" },
  { value: "AMORTIZATION", label: "摊销" },
  { value: "PROJECT_RECEIPT", label: "项目回款" },
  { value: "WAREHOUSE_TRANSFER_IN", label: "仓库调入" },
  { value: "PROJECT_TRANSFER_OUT", label: "项目调出" },
  { value: "SALARY", label: "人员工资" },
  { value: "TRAVEL", label: "差旅" },
  { value: "ENTERTAINMENT", label: "招待" },
  { value: "CONFERENCE", label: "会议" },
  { value: "VEHICLE", label: "车辆" },
  { value: "OTHER", label: "其他" },
];

const DIRECTION_OPTIONS = [
  { value: "RECEIVE", label: "收款" },
  { value: "PAY", label: "付款" },
];

const INVOICE_STATUS_OPTIONS = [
  { value: "NOT_INVOICED", label: "未开票" },
  { value: "INVOICED", label: "已开票" },
];

const INVOICE_TYPE_OPTIONS = [
  { value: "VAT_ORDINARY", label: "增值税普通发票" },
  { value: "VAT_SPECIAL", label: "增值税专用发票" },
];

const FIXED_THIRTEEN_PERCENT_EXPENSE_SUBJECTS = ["SALARY", "TRAVEL"];

function normalizeResponseData(response) {
  return response?.data ?? response ?? [];
}

function formatDate(value, pattern = "YYYY-MM-DD") {
  return value ? dayjs(value).format(pattern) : "-";
}

function formatAmount(value) {
  if (value == null || value === "") {
    return "-";
  }
  return Number(value).toFixed(2);
}

function VoucherDirectionSync({ form }) {
  const direction = Form.useWatch("transactionDirection", form);

  useEffect(() => {
    if (direction === "RECEIVE") {
      form.setFields([
        { name: "actualExpenseAmount", value: null, errors: [] },
        { name: "actualIncomeAmount", errors: [] },
      ]);
    } else if (direction === "PAY") {
      form.setFields([
        { name: "actualIncomeAmount", value: null, errors: [] },
        { name: "actualExpenseAmount", errors: [] },
      ]);
    }
  }, [direction, form]);

  return null;
}

function InvoiceTypeField({ form }) {
  const invoiceStatus = Form.useWatch("invoiceStatus", form);

  useEffect(() => {
    if (invoiceStatus === "NOT_INVOICED") {
      form.setFieldValue("invoiceType", null);
    }
  }, [form, invoiceStatus]);

  return (
    <Form.Item
      name="invoiceType"
      label="发票分类"
      hidden={invoiceStatus !== "INVOICED"}
      preserve
      rules={invoiceStatus === "INVOICED" ? [{ required: true, message: "请选择发票分类" }] : []}
    >
      <Select placeholder="请选择发票分类">
        {INVOICE_TYPE_OPTIONS.map((item) => (
          <Option key={item.value} value={item.value}>
            {item.label}
          </Option>
        ))}
      </Select>
    </Form.Item>
  );
}

function DirectionalAmountField({ form, activeDirection, placeholder, ...inputProps }) {
  const direction = Form.useWatch("transactionDirection", form);
  const disabled = Boolean(direction) && direction !== activeDirection;

  return (
    <InputNumber
      {...inputProps}
      min={0}
      precision={2}
      style={{ width: "100%" }}
      disabled={disabled}
      placeholder={placeholder}
    />
  );
}

function normalizeDirectionalAmounts(values) {
  const direction = values.transactionDirection;
  const normalizedIncome =
    direction === "RECEIVE" && values.actualIncomeAmount !== undefined && values.actualIncomeAmount !== null
      ? Number(values.actualIncomeAmount)
      : null;
  const normalizedExpense =
    direction === "PAY" && values.actualExpenseAmount !== undefined && values.actualExpenseAmount !== null
      ? Number(values.actualExpenseAmount)
      : null;

  return {
    actualIncomeAmount: normalizedIncome,
    actualExpenseAmount: normalizedExpense,
  };
}

function validateDirectionalAmounts(values) {
  if (values.transactionDirection === "RECEIVE") {
    if (values.actualIncomeAmount === undefined || values.actualIncomeAmount === null || values.actualIncomeAmount === "") {
      throw new Error("收款方向必须填写实际收入金额");
    }
    if (Number(values.actualIncomeAmount) <= 0) {
      throw new Error("实际收入金额必须大于 0");
    }
  }

  if (values.transactionDirection === "PAY") {
    if (values.actualExpenseAmount === undefined || values.actualExpenseAmount === null || values.actualExpenseAmount === "") {
      throw new Error("付款方向必须填写实际支出金额");
    }
    if (Number(values.actualExpenseAmount) <= 0) {
      throw new Error("实际支出金额必须大于 0");
    }
  }
}

function calculateBookedAmountPreview(values) {
  const taxRate = values.taxRate === undefined || values.taxRate === null || values.taxRate === ""
    ? null
    : Number(values.taxRate);

  if (values.transactionDirection === "RECEIVE") {
    const income = Number(values.actualIncomeAmount || 0);
    if (!(income > 0) || taxRate == null) {
      return null;
    }
    const denominator = taxRate === 0
      ? 1 - (values.invoiceStatus === "INVOICED" ? 0.115 : 0.18)
      : 1 - (0.13 - taxRate);
    if (!Number.isFinite(denominator) || denominator === 0) {
      return null;
    }
    return Number((income / denominator).toFixed(2));
  }

  if (values.transactionDirection === "PAY") {
    const expense = Number(values.actualExpenseAmount || 0);
    if (!(expense > 0) || taxRate == null) {
      return null;
    }

    let denominator;
    if (FIXED_THIRTEEN_PERCENT_EXPENSE_SUBJECTS.includes(values.level2Subject)) {
      denominator = 1 - 0.13;
    } else if (taxRate === 0) {
      denominator = 1 - (values.invoiceStatus === "INVOICED" ? 0.115 : 0.18);
    } else {
      denominator = 1 - (0.13 - taxRate);
    }
    if (!Number.isFinite(denominator) || denominator === 0) {
      return null;
    }

    let bookedAmount = expense / denominator;
    if (values.level2Subject === "PROJECT_TRANSFER_OUT") {
      bookedAmount = -bookedAmount;
    }
    return Number(bookedAmount.toFixed(2));
  }

  return null;
}

function calculateReceivablePayableInfo(values) {
  if (!["RECEIVE", "PAY"].includes(values.transactionDirection)) {
    return { type: null, amount: null };
  }

  const actualAmount =
    values.transactionDirection === "RECEIVE"
      ? Number(values.actualIncomeAmount || 0)
      : values.transactionDirection === "PAY"
        ? Number(values.actualExpenseAmount || 0)
        : 0;

  if (!values.isCompleted) {
    if (!(actualAmount > 0)) {
      return { type: null, amount: null };
    }
    return {
      type: values.transactionDirection === "RECEIVE" ? "应收" : "应付",
      amount: Number(actualAmount.toFixed(2)),
    };
  }

  const invoiceAmount =
    values.invoiceAmount === undefined || values.invoiceAmount === null || values.invoiceAmount === ""
      ? null
      : Number(values.invoiceAmount);

  if (invoiceAmount == null) {
    return { type: null, amount: null };
  }

  if (values.transactionDirection === "RECEIVE") {
    if (actualAmount < invoiceAmount) {
      return {
        type: "应收",
        amount: Number((invoiceAmount - actualAmount).toFixed(2)),
      };
    }
    if (actualAmount > invoiceAmount) {
      return {
        type: "待开票",
        amount: Number((actualAmount - invoiceAmount).toFixed(2)),
      };
    }
  }

  if (values.transactionDirection === "PAY") {
    if (actualAmount < invoiceAmount) {
      return {
        type: "应付",
        amount: Number((invoiceAmount - actualAmount).toFixed(2)),
      };
    }
    if (actualAmount > invoiceAmount) {
      return {
        type: "缺票",
        amount: Number((actualAmount - invoiceAmount).toFixed(2)),
      };
    }
  }

  return { type: null, amount: null };
}

function BookedAmountPreview({ form }) {
  const direction = Form.useWatch("transactionDirection", form);
  const income = Form.useWatch("actualIncomeAmount", form);
  const expense = Form.useWatch("actualExpenseAmount", form);
  const taxRate = Form.useWatch("taxRate", form);
  const level2Subject = Form.useWatch("level2Subject", form);
  const invoiceStatus = Form.useWatch("invoiceStatus", form);
  const storedBookedAmount = Form.useWatch("bookedAmount", form);

  const preview = useMemo(
    () =>
      calculateBookedAmountPreview({
        transactionDirection: direction,
        actualIncomeAmount: income,
        actualExpenseAmount: expense,
        taxRate,
        level2Subject,
        invoiceStatus,
      }),
    [direction, expense, income, invoiceStatus, level2Subject, taxRate]
  );

  const displayValue =
    preview != null
      ? preview.toFixed(2)
      : storedBookedAmount != null && storedBookedAmount !== ""
        ? Number(storedBookedAmount).toFixed(2)
        : "";

  return (
    <Input
      value={displayValue}
      placeholder="填写金额后自动计算"
      disabled
    />
  );
}

function ReceivablePayableAmountPreview({ form }) {
  const direction = Form.useWatch("transactionDirection", form);
  const income = Form.useWatch("actualIncomeAmount", form);
  const expense = Form.useWatch("actualExpenseAmount", form);
  const invoiceAmount = Form.useWatch("invoiceAmount", form);
  const isCompleted = Form.useWatch("isCompleted", form);

  const info = useMemo(
    () =>
      calculateReceivablePayableInfo({
        transactionDirection: direction,
        actualIncomeAmount: income,
        actualExpenseAmount: expense,
        invoiceAmount,
        isCompleted,
      }),
    [direction, expense, income, invoiceAmount, isCompleted]
  );

  const hasGap = info.amount != null && info.amount !== 0;

  return (
    <div>
      <Input value={info.amount != null ? info.amount.toFixed(2) : ""} placeholder="填写实际金额和开票金额后自动计算" disabled />
      {hasGap ? (
        <div style={{ marginTop: 6, color: "#d4380d", fontSize: 12 }}>
          已识别为{info.type}，金额 {info.amount.toFixed(2)}。
        </div>
      ) : null}
    </div>
  );
}

function ReceivablePayableTypePreview({ form }) {
  const direction = Form.useWatch("transactionDirection", form);
  const income = Form.useWatch("actualIncomeAmount", form);
  const expense = Form.useWatch("actualExpenseAmount", form);
  const invoiceAmount = Form.useWatch("invoiceAmount", form);
  const isCompleted = Form.useWatch("isCompleted", form);

  const info = useMemo(
    () =>
      calculateReceivablePayableInfo({
        transactionDirection: direction,
        actualIncomeAmount: income,
        actualExpenseAmount: expense,
        invoiceAmount,
        isCompleted,
      }),
    [direction, expense, income, invoiceAmount, isCompleted]
  );

  return (
    <Input
      value={info.type || ""}
      placeholder="填写实际金额和开票金额后自动判断"
      disabled
    />
  );
}

function VoucherAmountFields({ form, incomeLabel, expenseLabel }) {
  return (
    <Row gutter={16}>
      <Col span={12}>
        <Form.Item name="actualIncomeAmount" label={incomeLabel}>
          <DirectionalAmountField form={form} activeDirection="RECEIVE" placeholder="选择“收款”后填写" />
        </Form.Item>
      </Col>
      <Col span={12}>
        <Form.Item name="actualExpenseAmount" label={expenseLabel}>
          <DirectionalAmountField form={form} activeDirection="PAY" placeholder="选择“付款”后填写" />
        </Form.Item>
      </Col>
    </Row>
  );
}

function formatProjectOptionLabel(project) {
  if (!project) {
    return "";
  }
  if (project.projectNumber && project.projectName) {
    return `${project.projectNumber} - ${project.projectName}`;
  }
  return project.projectName || project.projectNumber || project.uuid || "";
}

function getListTypeLabel(value) {
  return LIST_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || "-";
}

export default function ProjectDashboard() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const projectUuid = searchParams.get("projectUuid") || searchParams.get("projectId") || "";

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [projects, setProjects] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [projectTypes, setProjectTypes] = useState([]);
  const [users, setUsers] = useState([]);
  const [regions, setRegions] = useState([]);
  const [stages, setStages] = useState([]);
  const [statusHistory, setStatusHistory] = useState([]);
  const [projectLists, setProjectLists] = useState([]);
  const [contracts, setContracts] = useState([]);
  const [contractTypes, setContractTypes] = useState([]);
  const [financeVouchers, setFinanceVouchers] = useState([]);
  const [taxRates, setTaxRates] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const [editVisible, setEditVisible] = useState(false);
  const [listVisible, setListVisible] = useState(false);
  const [pendingListAttachmentFiles, setPendingListAttachmentFiles] = useState([]);
  const [contractVisible, setContractVisible] = useState(false);
  const [voucherVisible, setVoucherVisible] = useState(false);
  const [advanceVisible, setAdvanceVisible] = useState(false);

  const [editForm] = Form.useForm();
  const [listForm] = Form.useForm();
  const [contractForm] = Form.useForm();
  const [voucherForm] = Form.useForm();
  const [advanceForm] = Form.useForm();
  const milestoneContainerRef = useRef(null);
  const listAttachmentInputRef = useRef(null);

  const canManageProject = hasAnyAuthority(currentUser, ["project.manage"]);
  const canEnterProjectList = hasAnyAuthority(currentUser, ["project.list.entry", "project.manage"]);
  const canAccessContract = hasAnyAuthority(currentUser, ["contract.access"]);
  const canManageContract = hasAnyAuthority(currentUser, ["contract.manage"]);
  const canAccessFinance = hasAnyAuthority(currentUser, [
    "finance.access",
    "finance.manage",
    "finance.voucher.entry",
    "finance.voucher.audit",
  ]);
  const canManageFinance = hasAnyAuthority(currentUser, ["finance.voucher.entry", "finance.manage"]);
  const canAccessAttachment = hasAnyAuthority(currentUser, ["attachment.access"]);
  const canManageAttachment = hasAnyAuthority(currentUser, ["attachment.manage"]);

  const customerMap = useMemo(() => Object.fromEntries(customers.map((item) => [item.uuid, item.customerName])), [customers]);
  const userMap = useMemo(() => Object.fromEntries(users.map((item) => [item.id, item.realName || item.username || `用户${item.id}`])), [users]);
  const projectTypeMap = useMemo(() => Object.fromEntries(projectTypes.map((item) => [item.id, `${item.code} ${item.name}`])), [projectTypes]);
  const stageMap = useMemo(() => Object.fromEntries(stages.map((item) => [item.id, item])), [stages]);
  const regionMap = useMemo(() => Object.fromEntries(regions.map((item) => [item.id, formatRegionLabel(item)])), [regions]);
  const regionById = useMemo(() => Object.fromEntries(regions.map((item) => [item.id, item])), [regions]);
  const regionByAreaCode = useMemo(() => Object.fromEntries(regions.map((item) => [item.areaCode, item])), [regions]);
  const regionOptions = useMemo(() => buildRegionOptions(regions), [regions]);
  const contractTypeMap = useMemo(() => Object.fromEntries(contractTypes.map((item) => [item.id, item.typeName || item.name || item.code])), [contractTypes]);
  const projectMap = useMemo(() => Object.fromEntries(projects.map((item) => [item.uuid, formatProjectOptionLabel(item)])), [projects]);

  const stageList = useMemo(() => [...stages].sort((a, b) => {
    const sortDiff = Number(a.sortOrder || 0) - Number(b.sortOrder || 0);
    if (sortDiff !== 0) {
      return sortDiff;
    }
    return String(a.stageCode || "").localeCompare(String(b.stageCode || ""), "zh-CN", { numeric: true });
  }), [stages]);

  const project = useMemo(() => projects.find((item) => item.uuid === projectUuid) || null, [projects, projectUuid]);
  const canDeleteProject = canManageProject && project?.canDelete !== false;
  const historyList = useMemo(() => [...statusHistory].filter((item) => item.projectId === projectUuid).sort((a, b) => dayjs(a.createdAt).valueOf() - dayjs(b.createdAt).valueOf()), [projectUuid, statusHistory]);
  const currentStageId = useMemo(() => (historyList.length ? historyList[historyList.length - 1].stageId : null), [historyList]);
  const currentStageIndex = useMemo(() => stageList.findIndex((item) => item.id === currentStageId), [currentStageId, stageList]);
  const nextStage = useMemo(() => (currentStageIndex < 0 ? stageList[0] || null : stageList[currentStageIndex + 1] || null), [currentStageIndex, stageList]);
  const availableAdvanceStages = useMemo(
    () => stageList.filter((item, index) => index > currentStageIndex),
    [currentStageIndex, stageList]
  );
  const milestoneItems = useMemo(
    () =>
      stageList.map((item, index) => ({
        ...item,
        status:
          index < currentStageIndex ? "done" : index === currentStageIndex ? "current" : "upcoming",
      })),
    [currentStageIndex, stageList]
  );


  const visibleLists = useMemo(
    () =>
      projectLists
        .filter((item) => item.projectId === projectUuid)
        .map((item) => ({
          ...item,
          entryUserName: item.entryUserName || userMap[item.entryUser] || "",
          auditorUserName: item.auditorUserName || userMap[item.auditorUser] || "",
        })),
    [projectLists, projectUuid, userMap]
  );
  const visibleContracts = useMemo(() => contracts.filter((item) => item.projectBasicInfoId === projectUuid), [contracts, projectUuid]);
  const visibleVouchers = useMemo(
    () =>
      financeVouchers
        .filter((item) => item.projectId === projectUuid)
        .map((item) => ({
          ...item,
          invoiceTypeLabel:
            INVOICE_TYPE_OPTIONS.find((option) => option.value === item.invoiceType)?.label || item.invoiceType,
          receivablePayableType: calculateReceivablePayableInfo(item).type,
          receivablePayableAmount: calculateReceivablePayableInfo(item).amount,
        })),
    [financeVouchers, projectUuid]
  );
  const visibleAttachments = useMemo(() => attachments.filter((item) => item.businessUuid === projectUuid), [attachments, projectUuid]);
  const dashboardStats = useMemo(
    () => [
      {
        key: "lists",
        title: "清单",
        value: visibleLists.length,
        icon: <UnorderedListOutlined />,
        route: `/project/lists?projectUuid=${projectUuid}`,
      },
      {
        key: "contracts",
        title: "合同",
        value: visibleContracts.length,
        icon: <FileTextOutlined />,
        route: `/contract/info?projectUuid=${projectUuid}`,
      },
      {
        key: "vouchers",
        title: "财务凭证",
        value: visibleVouchers.length,
        icon: <ProfileOutlined />,
        route: `/finance/vouchers?projectUuid=${projectUuid}`,
      },
      {
        key: "attachments",
        title: "附件",
        value: visibleAttachments.length,
        icon: <LinkOutlined />,
        route: `/attachment/center?businessType=projects&businessUuid=${projectUuid}`,
      },
    ],
    [projectUuid, visibleAttachments.length, visibleContracts.length, visibleLists.length, visibleVouchers.length]
  );

  const loadAll = useCallback(async () => {
    if (!projectUuid) {
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const user = await getCurrentUser().catch(() => null);
      const allowContract = hasAnyAuthority(user, ["contract.access"]);
      const allowFinance = hasAnyAuthority(user, [
        "finance.access",
        "finance.manage",
        "finance.voucher.entry",
        "finance.voucher.audit",
      ]);
      const allowAttachment = hasAnyAuthority(user, ["attachment.access"]);

      const [
        projectResponse,
        customerResponse,
        projectTypeResponse,
        userOptions,
        regionResponse,
        stageResponse,
        historyResponse,
        listResponse,
        contractResponse,
        contractTypeResponse,
        voucherResponse,
        taxRateResponse,
        attachmentResponse,
      ] = await Promise.all([
        projectAPI.getProjects(),
        customerAPI.getCustomerOptions(),
        projectAPI.getProjectTypes(),
        permissionAPI.getUserOptions().catch(() => []),
        regionAPI.getRegionOptions(),
        projectAPI.getStages(),
        projectAPI.getProjectStatusHistory(projectUuid),
        projectAPI.getProjectListsByProject(projectUuid),
        allowContract ? contractAPI.getContracts() : Promise.resolve([]),
        allowContract ? contractAPI.getContractTypes() : Promise.resolve([]),
        allowFinance ? financeAPI.getFinanceVouchers() : Promise.resolve([]),
        allowFinance ? financeAPI.getTaxRateDicts() : Promise.resolve([]),
        allowAttachment ? attachmentAPI.getAttachmentsByBusiness("projects", projectUuid) : Promise.resolve([]),
      ]);

      setCurrentUser(user);
      setProjects(normalizeResponseData(projectResponse));
      setCustomers(normalizeResponseData(customerResponse));
      setProjectTypes(normalizeResponseData(projectTypeResponse));
      setUsers(Array.isArray(userOptions) ? userOptions : normalizeResponseData(userOptions));
      setRegions(normalizeResponseData(regionResponse));
      setStages(normalizeResponseData(stageResponse));
      setStatusHistory(normalizeResponseData(historyResponse));
      setProjectLists(normalizeResponseData(listResponse));
      setContracts(normalizeResponseData(contractResponse));
      setContractTypes(normalizeResponseData(contractTypeResponse));
      setFinanceVouchers(normalizeResponseData(voucherResponse));
      setTaxRates(normalizeResponseData(taxRateResponse));
      setAttachments(normalizeResponseData(attachmentResponse));
    } finally {
      setLoading(false);
    }
  }, [projectUuid]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  useEffect(() => {
    if (!project) {
      return;
    }
    editForm.setFieldsValue({
      projectNumber: project.projectNumber,
      projectName: project.projectName,
      customerId: project.customerId,
      managerId: project.managerId,
      participant1UserId: project.participant1UserId,
      participant2UserId: project.participant2UserId,
      participant3UserId: project.participant3UserId,
      projectTypeId: project.projectTypeId,
      regionId: project.regionId ? buildRegionPath(project.regionId, regionById, regionByAreaCode) : [],
      planStartTime: project.planStartTime ? dayjs(project.planStartTime) : null,
      planEndTime: project.planEndTime ? dayjs(project.planEndTime) : null,
      warrantyUntil: project.warrantyUntil ? dayjs(project.warrantyUntil) : null,
      salesContractAttachmentUuid: project.salesContractAttachmentUuid,
    });
  }, [editForm, project, regionByAreaCode, regionById]);

  useEffect(() => {
    if (!advanceVisible) {
      return;
    }
    advanceForm.setFieldsValue({
      stageId: nextStage?.id ?? undefined,
      createdBy: currentUser?.id ?? undefined,
    });
  }, [advanceForm, advanceVisible, currentUser, nextStage]);

  const handleDeleteProject = async () => {
    if (!project) {
      return;
    }
    await projectAPI.deleteProject(project.uuid);
    message.success("项目已删除");
    navigate("/project/info");
  };

  const handleSaveProject = async () => {
    const values = await editForm.validateFields();
    setSubmitting(true);
    try {
      await projectAPI.updateProject(project.uuid, {
        ...values,
        regionId: Array.isArray(values.regionId) ? values.regionId[values.regionId.length - 1] || null : values.regionId,
        planStartTime: values.planStartTime ? values.planStartTime.format("YYYY-MM-DD") : null,
        planEndTime: values.planEndTime ? values.planEndTime.format("YYYY-MM-DD") : null,
        warrantyUntil: values.warrantyUntil ? values.warrantyUntil.format("YYYY-MM-DD") : null,
      });
      message.success("项目已更新");
      setEditVisible(false);
      await loadAll();
    } finally {
      setSubmitting(false);
    }
  };

  const handleAdvanceStage = async () => {
    if (!project) {
      return;
    }
    const values = await advanceForm.validateFields();
    const nextIndex = stageList.findIndex((item) => item.id === values.stageId);
    if (nextIndex <= currentStageIndex) {
      message.error("阶段不能回退或停留在当前阶段");
      return;
    }
    setSubmitting(true);
    try {
      await projectAPI.createProjectStatusHistory({
        projectId: project.uuid,
        stageId: values.stageId,
        createdBy: values.createdBy || null,
      });
      message.success(`项目已推进到“${stageMap[values.stageId]?.description || "新阶段"}”`);
      setAdvanceVisible(false);
      advanceForm.resetFields();
      await loadAll();
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateList = async () => {
    const values = await listForm.validateFields();
    setSubmitting(true);
    try {
      const listPayload = {
        projectId: project.uuid,
        listName: values.listName,
        listType: values.listType,
        entryDate: values.entryDate ? values.entryDate.format("YYYY-MM-DD") : null,
      };
      const created = normalizeResponseData(await projectAPI.createProjectList(listPayload));
      if (pendingListAttachmentFiles.length) {
        const uploadedAttachments = await uploadBusinessAttachments("project-lists", created.uuid, pendingListAttachmentFiles);
        await projectAPI.updateProjectList(created.uuid, {
          ...listPayload,
          pdfAttachmentId: uploadedAttachments[0]?.uuid || null,
        });
      }
      message.success("项目清单已创建");
      setListVisible(false);
      listForm.resetFields();
      setPendingListAttachmentFiles([]);
      if (listAttachmentInputRef.current) {
        listAttachmentInputRef.current.value = "";
      }
      await loadAll();
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateContract = async () => {
    const values = await contractForm.validateFields();
    setSubmitting(true);
    try {
      await contractAPI.createContract({
        projectBasicInfoId: project.uuid,
        clientId: project.customerId,
        contractTypeId: values.contractTypeId,
        contractNumber: values.contractNumber,
        subItemNo: values.subItemNo,
        subItemContent: values.subItemContent,
        signDate: values.signDate ? values.signDate.format("YYYY-MM-DD") : null,
        contractAmount: values.contractAmount,
      });
      message.success("合同已创建");
      setContractVisible(false);
      contractForm.resetFields();
      await loadAll();
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateVoucher = async () => {
    const values = await voucherForm.validateFields();
    validateDirectionalAmounts(values);
    const normalizedAmounts = normalizeDirectionalAmounts(values);
    setSubmitting(true);
    try {
        await financeAPI.createFinanceVoucher({
          occurredOn: values.occurredOn ? values.occurredOn.format("YYYY-MM-DD") : null,
          projectId: project.uuid,
        level1Subject: values.level1Subject,
        level2Subject: values.level2Subject,
        summary: values.summary,
          transactionDirection: values.transactionDirection,
          taxRate: values.taxRate,
          counterpartyCustomerId: values.counterpartyCustomerId,
          invoiceStatus: values.invoiceStatus,
          invoiceType: values.invoiceStatus === "INVOICED" ? values.invoiceType : null,
          invoiceNo: values.invoiceNo || null,
          invoiceAmount:
            values.invoiceAmount === undefined || values.invoiceAmount === null || values.invoiceAmount === ""
              ? null
              : Number(values.invoiceAmount),
          actualIncomeAmount: normalizedAmounts.actualIncomeAmount,
          actualExpenseAmount: normalizedAmounts.actualExpenseAmount,
          isCompleted: values.isCompleted ?? false,
        remark: values.remark,
      });
      message.success("财务凭证已创建");
      setVoucherVisible(false);
      voucherForm.resetFields();
      await loadAll();
    } finally {
      setSubmitting(false);
    }
  };

  const openProjectListDashboard = (record) => {
    if (!record?.uuid) {
      return;
    }
    navigate(`/project/list-dashboard?projectUuid=${project.uuid}&listUuid=${record.uuid}&from=dashboard`);
  };

  const openAggregateProjectListDashboard = (aggregateType) => {
    if (!project?.uuid) {
      return;
    }
    navigate(`/project/list-dashboard?projectUuid=${project.uuid}&aggregateType=${aggregateType}&from=dashboard`);
  };

  const listColumns = [
    { title: "清单名称", dataIndex: "listName", key: "listName", width: 160 },
    { title: "清单类型", dataIndex: "listType", key: "listType", width: 120, render: getListTypeLabel },
    { title: "录入日期", dataIndex: "entryDate", key: "entryDate", width: 120, render: (value) => formatDate(value) },
    { title: "录入人", dataIndex: "entryUserName", key: "entryUserName", width: 120, render: (value) => value || "-" },
    { title: "审核人", dataIndex: "auditorUserName", key: "auditorUserName", width: 120, render: (value) => value || "-" },
    { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 160, render: (value) => formatDate(value, "YYYY-MM-DD HH:mm") },
  ];

  const contractColumns = [
    { title: "合同编号", dataIndex: "contractNumber", key: "contractNumber" },
    { title: "合同类型", dataIndex: "contractTypeId", key: "contractTypeId", render: (value) => contractTypeMap[value] || value || "-" },
    { title: "签约日期", dataIndex: "signDate", key: "signDate", render: (value) => formatDate(value) },
    { title: "合同金额", dataIndex: "contractAmount", key: "contractAmount", render: (value) => formatAmount(value) },
  ];

    const voucherColumns = [
      { title: "凭证号", dataIndex: "voucherNo", key: "voucherNo" },
      { title: "发生日期", dataIndex: "occurredOn", key: "occurredOn", render: (value) => formatDate(value) },
      { title: "项目", dataIndex: "projectId", key: "projectId", render: (value) => projectMap[value] || "-" },
      { title: "二级科目", dataIndex: "level2Subject", key: "level2Subject", render: (value) => LEVEL_2_OPTIONS.find((item) => item.value === value)?.label || value || "-" },
      { title: "开票状态", dataIndex: "invoiceStatus", key: "invoiceStatus", render: (value) => INVOICE_STATUS_OPTIONS.find((item) => item.value === value)?.label || value || "-" },
      { title: "发票分类", dataIndex: "invoiceTypeLabel", key: "invoiceTypeLabel", render: (value) => value || "-" },
      { title: "记账金额", dataIndex: "bookedAmount", key: "bookedAmount", render: (value) => formatAmount(value) },
      { title: "差额类型", dataIndex: "receivablePayableType", key: "receivablePayableType", render: (value) => value || "-" },
      { title: "金额", dataIndex: "receivablePayableAmount", key: "receivablePayableAmount", render: (value) => formatAmount(value) },
    ];

  const attachmentColumns = [
    { title: "文件名", dataIndex: "originalFileName", key: "originalFileName" },
    { title: "扩展名", dataIndex: "fileExt", key: "fileExt", render: (value) => value || "-" },
    { title: "上传时间", dataIndex: "uploadedAt", key: "uploadedAt", render: (value) => formatDate(value, "YYYY-MM-DD HH:mm") },
  ];

  if (loading) {
    return (
      <div style={{ padding: 40, textAlign: "center" }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!projectUuid || !project) {
    return (
      <Card>
        <Empty description="未找到对应项目，请先从项目信息进入。">
          <Button type="primary" icon={<ArrowLeftOutlined />} onClick={() => navigate("/project/info")}>
            返回项目信息
          </Button>
        </Empty>
      </Card>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <style>{`
        .milestone-scroll-shell {
          position: relative;
        }
        .milestone-scroll-shell::before,
        .milestone-scroll-shell::after {
          content: "";
          position: absolute;
          top: 0;
          bottom: 10px;
          width: 24px;
          pointer-events: none;
          z-index: 1;
        }
        .milestone-scroll-shell::before {
          left: 0;
          background: linear-gradient(90deg, rgba(255,255,255,0.96), rgba(255,255,255,0));
        }
        .milestone-scroll-shell::after {
          right: 0;
          background: linear-gradient(270deg, rgba(255,255,255,0.96), rgba(255,255,255,0));
        }
        .milestone-scroll {
          overflow-x: auto;
          overflow-y: hidden;
          padding: 2px 4px 8px;
          margin: 0 -4px;
          scroll-behavior: smooth;
          -webkit-overflow-scrolling: touch;
          scrollbar-width: thin;
          scrollbar-color: rgba(15, 23, 42, 0.18) transparent;
        }
        .milestone-scroll::-webkit-scrollbar {
          height: 6px;
        }
        .milestone-scroll::-webkit-scrollbar-track {
          background: transparent;
        }
        .milestone-scroll::-webkit-scrollbar-thumb {
          background: rgba(148, 163, 184, 0.16);
          border-radius: 999px;
        }
        .milestone-scroll:hover::-webkit-scrollbar-thumb {
          background: rgba(100, 116, 139, 0.34);
        }
       `}</style>
      <Card>
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/project/info")}>
            返回项目信息
          </Button>
          <div>
            <div style={{ fontSize: 24, fontWeight: 700 }}>{project.projectName || "未命名项目"}</div>
            <div style={{ color: "#666", marginTop: 4 }}>
              项目编号：{project.projectNumber || "-"} · 客户：{customerMap[project.customerId] || "-"}
            </div>
          </div>
          <Row gutter={[16, 16]}>
            {dashboardStats.map((item) => (
              <Col xs={24} sm={12} xl={6} key={item.key}>
                <Card
                  hoverable
                  onClick={() => navigate(item.route)}
                  size="small"
                  styles={{
                    body: {
                      borderTop: `3px solid ${item.accent}`,
                      borderRadius: 12,
                    },
                  }}
                >
                  <Space align="start" style={{ width: "100%", justifyContent: "space-between" }}>
                    <div>
                      <div style={{ color: "#666", fontSize: 13, marginBottom: 8 }}>{item.title}</div>
                      <div style={{ fontSize: 30, fontWeight: 700, color: item.accent }}>{item.value}</div>
                    </div>
                    <div style={{ fontSize: 24, color: item.accent }}>{item.icon}</div>
                  </Space>
                </Card>
              </Col>
            ))}
          </Row>
        </Space>
      </Card>

      <Card
        title="项目记录"
        extra={
          canManageProject && availableAdvanceStages.length ? (
            <Popover
              trigger="click"
              open={advanceVisible}
              onOpenChange={setAdvanceVisible}
              placement="bottomRight"
              content={
                <Form form={advanceForm} layout="vertical" style={{ width: 280 }}>
                  <Form.Item name="stageId" label="目标阶段" rules={[{ required: true, message: "请选择目标阶段" }]}>
                    <Select placeholder="请选择阶段">
                      {availableAdvanceStages.map((item) => (
                        <Option key={item.id} value={item.id}>
                          {item.stageCode} {item.description}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                  <Form.Item name="createdBy" label="分配用户" rules={[{ required: true, message: "请选择用户" }]}>
                    <Select showSearch optionFilterProp="children" placeholder="请选择用户">
                      {users.map((item) => (
                        <Option key={item.id} value={item.id}>
                          {item.realName || item.username}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                  <Space style={{ width: "100%", justifyContent: "flex-end" }}>
                    <Button onClick={() => setAdvanceVisible(false)}>取消</Button>
                    <Button type="primary" loading={submitting} onClick={handleAdvanceStage}>
                      确认推进
                    </Button>
                  </Space>
                </Form>
              }
            >
              <Button type="primary">推进阶段</Button>
            </Popover>
          ) : null
        }
      >
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Descriptions size="small" column={2}>
            <Descriptions.Item label="当前阶段">{stageMap[currentStageId]?.description || "未开始"}</Descriptions.Item>
            <Descriptions.Item label="下一阶段">{nextStage?.description || "已完成"}</Descriptions.Item>
          </Descriptions>
          <div ref={milestoneContainerRef} className="milestone-scroll-shell">
            <div className="milestone-scroll">
              <div
                style={{
                  position: "relative",
                  paddingTop: 10,
                  paddingBottom: 4,
                  paddingLeft: 70,
                  paddingRight: 70,
                  minWidth: Math.max(milestoneItems.length * 140, 480),
                }}
              >
                 <div
                   style={{
                     position: "relative",
                     display: "grid",
                     gridTemplateColumns: `repeat(${Math.max(milestoneItems.length, 1)}, minmax(140px, 1fr))`,
                     gap: 0,
                     alignItems: "start",
                   }}
                 >
                   {milestoneItems.map((item, index) => {
                     const dotColor =
                       item.status === "done" ? "#1677ff" : item.status === "current" ? "#52c41a" : "#cfcfcf";
                     const textColor = item.status === "upcoming" ? "#8c8c8c" : "#262626";
                     const leftConnectorColor = index <= currentStageIndex ? "#1677ff" : "#e8e8e8";
                     const rightConnectorColor = index < currentStageIndex ? "#1677ff" : "#e8e8e8";
                     return (
                       <div key={item.id} style={{ minWidth: 0, textAlign: "center" }}>
                         <div
                           style={{
                             height: 18,
                             display: "flex",
                             alignItems: "center",
                             justifyContent: "center",
                             position: "relative",
                           }}
                         >
                           {index > 0 ? (
                             <span
                               style={{
                                 position: "absolute",
                                 left: 0,
                               right: "50%",
                               top: "50%",
                               height: 3,
                               transform: "translateY(-50%)",
                               background: leftConnectorColor,
                               borderRadius: 999,
                             }}
                           />
                           ) : null}
                           {index < milestoneItems.length - 1 ? (
                             <span
                               style={{
                                 position: "absolute",
                                 left: "50%",
                                right: 0,
                                top: "50%",
                                height: 3,
                                transform: "translateY(-50%)",
                                background: rightConnectorColor,
                                borderRadius: 999,
                              }}
                            />
                           ) : null}
                           <span
                             style={{
                               width: 12,
                               height: 12,
                               borderRadius: "50%",
                               background: dotColor,
                               border: "2px solid #ffffff",
                               position: "relative",
                               zIndex: 1,
                               boxShadow:
                                 item.status === "current"
                                   ? "0 0 0 4px rgba(82,196,26,0.16)"
                                   : "0 0 0 2px rgba(22,119,255,0.08)",
                             }}
                           />
                         </div>
                         <div style={{ marginTop: 10, fontSize: 12, fontWeight: 600, color: textColor }}>
                           {item.stageCode}
                         </div>
                         <div style={{ marginTop: 4, fontSize: 12, color: textColor, wordBreak: "break-word" }}>
                           {item.description}
                         </div>
                       </div>
                     );
                   })}
                 </div>
               </div>
             </div>
           </div>
          <Timeline
            items={
              historyList.length
                ? historyList
                    .slice()
                    .reverse()
                    .map((item) => ({
                      color: item.stageId === currentStageId ? "blue" : "gray",
                      children: (
                        <div>
                          <div style={{ fontWeight: 600 }}>{stageMap[item.stageId]?.description || item.stageName || "未知阶段"}</div>
                          <div style={{ color: "#666", marginTop: 4 }}>{formatDate(item.createdAt, "YYYY-MM-DD HH:mm")} · {userMap[item.createdBy] || `用户${item.createdBy}`}</div>
                        </div>
                      ),
                    }))
                : [{ color: "gray", children: "暂无项目记录" }]
            }
          />
        </Space>
      </Card>

      <Card
        title="项目概况"
        extra={
          canManageProject ? (
            <Space>
              <Button icon={<EditOutlined />} onClick={() => setEditVisible(true)}>
                编辑项目
              </Button>
              {canDeleteProject ? (
                <Popconfirm title="确认删除当前项目？" okText="删除" cancelText="取消" onConfirm={handleDeleteProject}>
                  <Button danger icon={<DeleteOutlined />}>
                    删除项目
                  </Button>
                </Popconfirm>
              ) : (
                <Popover
                  content={`当前项目已立项，达到删除锁定阈值，不允许删除。`}
                >
                  <span>
                    <Button danger icon={<DeleteOutlined />} disabled>
                      删除项目
                    </Button>
                  </span>
                </Popover>
              )}
            </Space>
          ) : null
        }
      >
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="项目编号">{project.projectNumber || "-"}</Descriptions.Item>
          <Descriptions.Item label="客户">{customerMap[project.customerId] || "-"}</Descriptions.Item>
          <Descriptions.Item label="负责人">{userMap[project.managerId] || "-"}</Descriptions.Item>
          <Descriptions.Item label="地区">{regionMap[project.regionId] || "-"}</Descriptions.Item>
          <Descriptions.Item label="项目类型">{projectTypeMap[project.projectTypeId] || "-"}</Descriptions.Item>
          <Descriptions.Item label="质保截止">{formatDate(project.warrantyUntil)}</Descriptions.Item>
          <Descriptions.Item label="计划开始">{formatDate(project.planStartTime)}</Descriptions.Item>
          <Descriptions.Item label="计划结束">{formatDate(project.planEndTime)}</Descriptions.Item>
          <Descriptions.Item label="参与人" span={2}>
            {[project.participant1UserId, project.participant2UserId, project.participant3UserId].filter(Boolean).map((value) => userMap[value] || `用户${value}`).join(" / ") || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="删除状态" span={2}>
            {project.canDelete === false
              ? `已锁定`
              : "可删除"}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card
            title="项目清单"
            extra={
              <Space>
                {canEnterProjectList ? (
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => {
                      listForm.setFieldsValue({
                        listType: "INITIAL_SALES",
                      });
                      setListVisible(true);
                    }}
                  >
                    快速新增
                  </Button>
                ) : null}
                <Button icon={<EyeOutlined />} onClick={() => openAggregateProjectListDashboard("FINAL_ALL")}>
                  最终清单总览
                </Button>
                <Button icon={<UnorderedListOutlined />} onClick={() => navigate(`/project/lists?projectUuid=${project.uuid}`)}>
                  查看当前项目全部清单
                </Button>
              </Space>
            }
          >
            <Table
              rowKey="uuid"
              size="small"
              pagination={false}
              columns={listColumns}
              dataSource={visibleLists.slice(0, 5)}
              locale={{ emptyText: "暂无项目清单" }}
              scroll={{ x: "max-content" }}
              onRow={(record) => ({
                onClick: () => openProjectListDashboard(record),
                style: { cursor: "pointer" },
              })}
            />
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card
            title="合同"
            extra={
              canAccessContract ? (
                <Space>
                  {canManageContract ? (
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => setContractVisible(true)}>
                      快速新增
                    </Button>
                  ) : null}
                  <Button icon={<FileTextOutlined />} onClick={() => navigate(`/contract/info?projectUuid=${project.uuid}`)}>
                    查看全部
                  </Button>
                </Space>
              ) : null
            }
          >
            {canAccessContract ? <Table rowKey="uuid" size="small" pagination={false} columns={contractColumns} dataSource={visibleContracts.slice(0, 5)} locale={{ emptyText: "暂无合同记录" }} /> : <Empty description="无合同查看权限" />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card
            title="财务凭证"
            extra={
              canAccessFinance ? (
                <Space>
                  {canManageFinance ? (
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => setVoucherVisible(true)}>
                      快速新增
                    </Button>
                  ) : null}
                  <Button icon={<ProfileOutlined />} onClick={() => navigate(`/finance/vouchers?projectUuid=${project.uuid}`)}>
                    查看全部
                  </Button>
                </Space>
              ) : null
            }
          >
            {canAccessFinance ? <Table rowKey="uuid" size="small" pagination={false} columns={voucherColumns} dataSource={visibleVouchers.slice(0, 5)} locale={{ emptyText: "暂无财务凭证" }} /> : <Empty description="无财务查看权限" />}
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card
            title="附件"
            extra={
              canAccessAttachment ? (
                <Button icon={<LinkOutlined />} onClick={() => navigate(`/attachment/center?businessType=projects&businessUuid=${project.uuid}`)}>
                  查看全部
                </Button>
              ) : null
            }
          >
            {canAccessAttachment ? <Table rowKey="uuid" size="small" pagination={false} columns={attachmentColumns} dataSource={visibleAttachments.slice(0, 5)} locale={{ emptyText: "暂无附件" }} /> : <Empty description="无附件查看权限" />}
          </Card>
        </Col>
      </Row>

      <Modal title="编辑项目" open={editVisible} onCancel={() => setEditVisible(false)} onOk={handleSaveProject} confirmLoading={submitting} destroyOnHidden width={860}>
        <Form form={editForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="projectNumber" label="项目编号" rules={[{ required: true, message: "请输入项目编号" }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="projectName" label="项目名称" rules={[{ required: true, message: "请输入项目名称" }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="customerId" label="客户" rules={[{ required: true, message: "请选择客户" }]}>
                <Select showSearch optionFilterProp="children">
                  {customers.map((item) => (
                    <Option key={item.uuid} value={item.uuid}>
                      {item.customerName}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="managerId" label="负责人" rules={[{ required: true, message: "请选择负责人" }]}>
                <Select showSearch optionFilterProp="children">
                  {users.map((item) => (
                    <Option key={item.id} value={item.id}>
                      {item.realName || item.username}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="participant1UserId" label="参与人 1">
                <Select allowClear showSearch optionFilterProp="children">
                  {users.map((item) => (
                    <Option key={item.id} value={item.id}>
                      {item.realName || item.username}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="participant2UserId" label="参与人 2">
                <Select allowClear showSearch optionFilterProp="children">
                  {users.map((item) => (
                    <Option key={item.id} value={item.id}>
                      {item.realName || item.username}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="participant3UserId" label="参与人 3">
                <Select allowClear showSearch optionFilterProp="children">
                  {users.map((item) => (
                    <Option key={item.id} value={item.id}>
                      {item.realName || item.username}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="projectTypeId" label="项目类型" rules={[{ required: true, message: "请选择项目类型" }]}>
                <Select>
                  {projectTypes.map((item) => (
                    <Option key={item.id} value={item.id}>
                      {item.code} {item.name}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="regionId" label="地区" rules={[{ required: true, message: "请选择地区" }]}>
                <Cascader options={regionOptions} placeholder="请选择省 / 市 / 区" changeOnSelect />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="planStartTime" label="计划开始">
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="planEndTime" label="计划结束">
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="warrantyUntil" label="质保截止">
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="快速新增项目清单"
        open={listVisible}
        onCancel={() => {
          setListVisible(false);
          listForm.resetFields();
          setPendingListAttachmentFiles([]);
          if (listAttachmentInputRef.current) {
            listAttachmentInputRef.current.value = "";
          }
        }}
        onOk={handleCreateList}
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Form form={listForm} layout="vertical" initialValues={{ listType: "INITIAL_SALES" }}>
          <Form.Item name="listName" label="清单名称" rules={[{ required: true, message: "请输入清单名称" }]}>
            <Input maxLength={128} placeholder="例如：一期采购清单" />
          </Form.Item>
          <Form.Item name="listType" label="清单类型" rules={[{ required: true, message: "请选择清单类型" }]}>
            <Select>
              {LIST_TYPE_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="entryDate" label="录入日期">
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item label="PDF附件">
            <BusinessAttachmentUpload
              title="PDF附件"
              businessType="project-lists"
              pendingFiles={pendingListAttachmentFiles}
              onPendingFilesChange={setPendingListAttachmentFiles}
              inputRef={listAttachmentInputRef}
              canAccess={canAccessAttachment}
              canManage={canManageAttachment}
              chooseText="选择 PDF 附件"
              helpText="保存项目清单时会自动上传并关联当前清单，可一次选择多个文件。"
              noManageText="当前账号没有附件上传权限，无法在这里上传 PDF 附件。"
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="快速新增合同" open={contractVisible} onCancel={() => setContractVisible(false)} onOk={handleCreateContract} confirmLoading={submitting} destroyOnHidden>
        <Form form={contractForm} layout="vertical">
          <Form.Item name="contractNumber" label="合同编号" rules={[{ required: true, message: "请输入合同编号" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="contractTypeId" label="合同类型" rules={[{ required: true, message: "请选择合同类型" }]}>
            <Select>
              {contractTypes.map((item) => (
                <Option key={item.id} value={item.id}>
                  {item.typeName || item.name || item.code}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="signDate" label="签约日期">
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="contractAmount" label="合同金额" rules={[{ required: true, message: "请输入合同金额" }]}>
            <InputNumber min={0} precision={2} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="subItemNo" label="分项编号">
            <Input />
          </Form.Item>
          <Form.Item name="subItemContent" label="分项内容">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="快速新增财务凭证" open={voucherVisible} onCancel={() => setVoucherVisible(false)} onOk={handleCreateVoucher} confirmLoading={submitting} destroyOnHidden>
        <Form form={voucherForm} layout="vertical" initialValues={{ level1Subject: "PROJECT", transactionDirection: "PAY", taxRate: 0.13, invoiceStatus: "NOT_INVOICED" }}>
          <VoucherDirectionSync form={voucherForm} />
          <Form.Item name="occurredOn" label="发生日期" rules={[{ required: true, message: "请选择发生日期" }]}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="level1Subject" label="一级科目" rules={[{ required: true, message: "请选择一级科目" }]}>
            <Select>
              {LEVEL_1_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="level2Subject" label="二级科目" rules={[{ required: true, message: "请选择二级科目" }]}>
            <Select>
              {LEVEL_2_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="summary" label="摘要" rules={[{ required: true, message: "请输入摘要" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="transactionDirection" label="方向" rules={[{ required: true, message: "请选择方向" }]}>
            <Select>
              {DIRECTION_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="taxRate" label="税率" rules={[{ required: true, message: "请选择税率" }]}>
            <Select>
              {taxRates.map((item) => (
                <Option key={item.id} value={item.rate}>
                  {item.label || `${Number(item.rate || 0) * 100}%`}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="counterpartyCustomerId" label="对方单位" rules={[{ required: true, message: "请选择对方单位" }]}>
            <Select showSearch optionFilterProp="children">
              {customers.map((item) => (
                <Option key={item.uuid} value={item.uuid}>
                  {item.customerName}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="invoiceStatus" label="开票状态" rules={[{ required: true, message: "请选择开票状态" }]}>
            <Select>
              {INVOICE_STATUS_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <InvoiceTypeField form={voucherForm} />
          <Form.Item name="invoiceNo" label="发票号码">
            <Input />
          </Form.Item>
          <Form.Item name="invoiceAmount" label="发票金额">
            <InputNumber min={0} precision={2} style={{ width: "100%" }} />
          </Form.Item>
          <VoucherAmountFields form={voucherForm} incomeLabel="实际收入金额" expenseLabel="实际支出金额" />
          <Form.Item label="差额类型（自动判断)">
            <ReceivablePayableTypePreview form={voucherForm} />
          </Form.Item>
          <Form.Item label="金额（自动计算）">
            <ReceivablePayableAmountPreview form={voucherForm} />
          </Form.Item>
          <Form.Item label="记账金额（自动计算）">
            <BookedAmountPreview form={voucherForm} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
