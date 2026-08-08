import React, { useEffect, useMemo, useRef, useState } from "react";
import dayjs from "dayjs";
import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Tag,
  message,
} from "antd";
import {
  AccountBookOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  BankOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CreditCardOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  LinkOutlined,
  WalletOutlined,
} from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getCurrentUser } from "../../api/auth";
import BusinessAttachmentUpload from "../../components/Common/BusinessAttachmentUpload";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI, exportAPI, financeAPI, permissionAPI, projectAPI } from "../../api/modules";
import { hasAnyAuthority } from "../../utils/authorities";
import { canMaintainEntryAuditUsers, getUserLabel } from "../../utils/entryAudit";
import { uploadBusinessAttachments } from "../../utils/attachments";
import { downloadApiFile, downloadExcel, resolveBlobErrorMessage } from "../../utils/exporters";

const { Option } = Select;

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

function normalizeApiData(response) {
  return response?.data ?? response ?? [];
}

function normalizeApiEntity(response) {
  return response?.data ?? response ?? null;
}

function formatAmount(value) {
  if (value == null || value === "") {
    return "-";
  }
  return Number(value).toFixed(2);
}

function normalizeDateOnlyValue(value) {
  if (!value) {
    return null;
  }
  if (typeof value === "string") {
    return dayjs(value.slice(0, 10));
  }
  return dayjs(value);
}

function dateSortValue(value) {
  if (!value) {
    return Number.POSITIVE_INFINITY;
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.valueOf() : Number.POSITIVE_INFINITY;
}

function sortVouchersAscending(rows = []) {
  return [...rows].sort((left, right) => {
    const dateDiff = dateSortValue(left.occurredOn) - dateSortValue(right.occurredOn);
    if (dateDiff !== 0) {
      return dateDiff;
    }

    const voucherNoDiff = String(left.voucherNo || "").localeCompare(
      String(right.voucherNo || ""),
      "zh-CN",
      { numeric: true }
    );
    if (voucherNoDiff !== 0) {
      return voucherNoDiff;
    }

    return Number(left.id || 0) - Number(right.id || 0);
  });
}

function isDateInInclusiveRange(value, range) {
  if (!Array.isArray(range) || range.length !== 2 || !range[0] || !range[1]) {
    return true;
  }
  if (!value) {
    return false;
  }
  const date = dayjs(value);
  return date.isValid() && !date.isBefore(range[0], "day") && !date.isAfter(range[1], "day");
}

function buildVoucherSearchText(voucher) {
  const values = [
    voucher.voucherNo,
    voucher.occurredOn ? dayjs(voucher.occurredOn).format("YYYY-MM-DD") : "",
    voucher.projectLabel,
    voucher.level1Label,
    voucher.level2Label,
    voucher.summary,
    voucher.directionLabel,
    voucher.taxRateLabel,
    voucher.counterpartyNameSnapshot,
    voucher.invoiceStatusLabel,
    voucher.invoiceTypeLabel,
    voucher.invoiceNo,
    voucher.invoiceAmount != null ? formatAmount(voucher.invoiceAmount) : "",
    voucher.receivablePayableType,
    voucher.receivablePayableAmount != null ? formatAmount(voucher.receivablePayableAmount) : "",
    voucher.actualIncomeAmount != null ? formatAmount(voucher.actualIncomeAmount) : "",
    voucher.actualExpenseAmount != null ? formatAmount(voucher.actualExpenseAmount) : "",
    voucher.bookedAmount != null ? formatAmount(voucher.bookedAmount) : "",
    voucher.isCompleted ? "是 已完成 完成" : "否 未完成 未完成",
    voucher.entryUserName,
    voucher.auditorUserName,
    voucher.remark,
  ];

  return values
    .filter((value) => value != null && value !== "")
    .join(" ")
    .toLowerCase();
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
    if (invoiceStatus !== "INVOICED") {
      form.setFieldValue("invoiceType", null);
    }
  }, [form, invoiceStatus]);

  if (invoiceStatus !== "INVOICED") {
    return null;
  }

  return (
    <Form.Item
      name="invoiceType"
      label="发票分类"
      preserve={false}
      rules={[{ required: true, message: "请选择发票分类" }]}
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
      throw new Error("收款方向必须填写实际收入");
    }
    if (Number(values.actualIncomeAmount) < 0) {
      throw new Error("实际收入不能为负数");
    }
  }

  if (values.transactionDirection === "PAY") {
    if (values.actualExpenseAmount === undefined || values.actualExpenseAmount === null || values.actualExpenseAmount === "") {
      throw new Error("付款方向必须填写实际支出");
    }
    if (Number(values.actualExpenseAmount) < 0) {
      throw new Error("实际支出不能为负数");
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

function calculatePayableAmount(voucher) {
  const info = calculateReceivablePayableInfo(voucher);
  return info.type === "应付" ? Number(info.amount || 0) : 0;
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
    <>
      <Form.Item name="actualIncomeAmount" label={incomeLabel}>
        <DirectionalAmountField form={form} activeDirection="RECEIVE" placeholder="选择“收款”后填写" />
      </Form.Item>
      <Form.Item name="actualExpenseAmount" label={expenseLabel}>
        <DirectionalAmountField form={form} activeDirection="PAY" placeholder="选择“付款”后填写" />
      </Form.Item>
    </>
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

export default function FinanceVouchers() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const projectIdFilter = searchParams.get("projectId") || searchParams.get("projectUuid") || "";
  const [vouchers, setVouchers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [taxRates, setTaxRates] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [pendingVoucherAttachmentFiles, setPendingVoucherAttachmentFiles] = useState([]);
  const [filteredVisibleVouchers, setFilteredVisibleVouchers] = useState([]);
  const voucherAttachmentInputRef = useRef(null);
  const canAccessAttachments = hasAnyAuthority(currentUser, ["attachment.access"]);
  const canManageAttachments = hasAnyAuthority(currentUser, ["attachment.manage"]);
  const canAuditVouchers = hasAnyAuthority(currentUser, ["finance.voucher.audit", "finance.manage"]);
  const canMaintainEntryAudit = canMaintainEntryAuditUsers(currentUser);

  const projectMap = useMemo(
    () => Object.fromEntries(projects.map((item) => [item.uuid, formatProjectOptionLabel(item)])),
    [projects]
  );

  const customerNameToUuidMap = useMemo(
    () =>
      Object.fromEntries(
        customers
          .filter((item) => item.customerName && item.uuid)
          .map((item) => [item.customerName, item.uuid])
      ),
    [customers]
  );

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

  const activeTaxRates = useMemo(
    () => taxRates.filter((item) => item.isActive !== false),
    [taxRates]
  );

  const taxRateOptions = useMemo(
    () =>
      activeTaxRates.map((item) => ({
        value: String(item.rate),
        label: item.label || `${Number(item.rate || 0) * 100}%`,
      })),
    [activeTaxRates]
  );

  const taxRateLabelMap = useMemo(
    () =>
      Object.fromEntries(
        taxRates.map((item) => [
          Number(item.rate).toFixed(2),
          item.label || `${Number(item.rate || 0) * 100}%`,
        ])
      ),
    [taxRates]
  );

  const decoratedVouchers = useMemo(
    () =>
      vouchers.map((item) => {
        const projectLabel = item.projectId ? projectMap[item.projectId] || item.projectId : "-";
        const level1Label =
          LEVEL_1_OPTIONS.find((option) => option.value === item.level1Subject)?.label || item.level1Subject;
        const level2Label =
          LEVEL_2_OPTIONS.find((option) => option.value === item.level2Subject)?.label || item.level2Subject;
        const directionLabel =
          DIRECTION_OPTIONS.find((option) => option.value === item.transactionDirection)?.label ||
          item.transactionDirection;
        const invoiceStatusLabel =
          INVOICE_STATUS_OPTIONS.find((option) => option.value === item.invoiceStatus)?.label || item.invoiceStatus;
        const invoiceTypeLabel =
          INVOICE_TYPE_OPTIONS.find((option) => option.value === item.invoiceType)?.label || item.invoiceType;
        const taxRateLabel = taxRateLabelMap[Number(item.taxRate || 0).toFixed(2)] || item.taxRate;
        const receivablePayableInfo = calculateReceivablePayableInfo(item);
        const occurredOnLabel = item.occurredOn ? dayjs(item.occurredOn).format("YYYY-MM-DD") : "-";
        const invoiceAmountLabel = item.invoiceAmount == null ? "-" : formatAmount(item.invoiceAmount);
        const receivablePayableAmountLabel =
          receivablePayableInfo.amount == null ? "-" : formatAmount(receivablePayableInfo.amount);
        const actualIncomeAmountLabel = item.actualIncomeAmount == null ? "-" : formatAmount(item.actualIncomeAmount);
        const actualExpenseAmountLabel = item.actualExpenseAmount == null ? "-" : formatAmount(item.actualExpenseAmount);
        const bookedAmountLabel = item.bookedAmount == null ? "-" : formatAmount(item.bookedAmount);
        const isCompletedLabel = item.isCompleted ? "已完成" : "未完成";
        const entryUserName = item.entryUserName || userMap[item.entryUser] || "";
        const auditorUserName = item.auditorUserName || userMap[item.auditorUser] || "";

        const decoratedItem = {
          ...item,
          projectLabel,
          level1Label,
          level2Label,
          directionLabel,
          invoiceStatusLabel,
          invoiceTypeLabel,
          taxRateLabel,
          receivablePayableType: receivablePayableInfo.type,
          receivablePayableAmount: receivablePayableInfo.amount,
          occurredOnLabel,
          invoiceAmountLabel,
          receivablePayableAmountLabel,
          actualIncomeAmountLabel,
          actualExpenseAmountLabel,
          bookedAmountLabel,
          isCompletedLabel,
          entryUserName,
          auditorUserName,
        };

        return {
          ...decoratedItem,
          searchText: buildVoucherSearchText(decoratedItem),
        };
      }),
    [projectMap, taxRateLabelMap, userMap, vouchers]
  );

  const visibleVouchers = useMemo(
    () =>
      projectIdFilter
        ? decoratedVouchers.filter((item) => item.projectId === projectIdFilter)
        : decoratedVouchers,
    [decoratedVouchers, projectIdFilter]
  );

  useEffect(() => {
    setFilteredVisibleVouchers(visibleVouchers);
  }, [visibleVouchers]);

  const summary = useMemo(
    () =>
      filteredVisibleVouchers.reduce(
        (accumulator, item) => {
          const bookedAmount = Number(item.bookedAmount || 0);
          accumulator.income += Number(item.actualIncomeAmount || 0);
          accumulator.expense += Number(item.actualExpenseAmount || 0);
          if (item.transactionDirection === "RECEIVE") {
            accumulator.incomeBooked += bookedAmount;
          }
          if (item.transactionDirection === "PAY") {
            accumulator.expenseBooked += Math.abs(bookedAmount);
          }
          accumulator.pending += item.isCompleted ? 0 : 1;
          const excludesNotInvoicedCount =
            item.transactionDirection === "PAY"
            && FIXED_THIRTEEN_PERCENT_EXPENSE_SUBJECTS.includes(item.level2Subject);
          accumulator.notInvoiced += item.invoiceStatus === "NOT_INVOICED" && !excludesNotInvoicedCount ? 1 : 0;
          accumulator.payable += item.receivablePayableType === "应付" ? 1 : 0;
          accumulator.payableAmount += calculatePayableAmount(item);
          accumulator.missingInvoice += item.receivablePayableType === "缺票" ? 1 : 0;
          accumulator.missingInvoiceAmount += item.receivablePayableType === "缺票"
            ? Number(item.receivablePayableAmount || 0)
            : 0;
          accumulator.receivable += item.receivablePayableType === "应收" ? 1 : 0;
          accumulator.receivableAmount += item.receivablePayableType === "应收"
            ? Number(item.receivablePayableAmount || 0)
            : 0;
          accumulator.pendingInvoice += item.receivablePayableType === "待开票" ? 1 : 0;
          accumulator.pendingInvoiceAmount += item.receivablePayableType === "待开票"
            ? Number(item.receivablePayableAmount || 0)
            : 0;
          return accumulator;
        },
        {
          income: 0,
          expense: 0,
          incomeBooked: 0,
          expenseBooked: 0,
          pending: 0,
          notInvoiced: 0,
          payable: 0,
          payableAmount: 0,
          missingInvoice: 0,
          missingInvoiceAmount: 0,
          receivable: 0,
          receivableAmount: 0,
          pendingInvoice: 0,
          pendingInvoiceAmount: 0,
        }
      ),
    [filteredVisibleVouchers]
  );

  const balanceTotal = useMemo(
    () => Number((summary.incomeBooked - summary.expenseBooked).toFixed(2)),
    [summary.expenseBooked, summary.incomeBooked]
  );

  const statCards = useMemo(
    () => [
      { key: "income", title: "收入合计", value: summary.income, precision: 2, icon: <ArrowDownOutlined />, accent: "#1677ff", bg: "#e6f4ff" },
      { key: "expense", title: "支出合计", value: summary.expense, precision: 2, icon: <ArrowUpOutlined />, accent: "#d46b08", bg: "#fff7e6" },
      { key: "booked", title: "支出记账合计", value: summary.expenseBooked, precision: 2, icon: <AccountBookOutlined />, accent: "#722ed1", bg: "#f9f0ff" },
      { key: "balance", title: "结余合计", value: balanceTotal, precision: 2, icon: <WalletOutlined />, accent: "#08979c", bg: "#e6fffb" },
      { key: "pending", title: "未完成数量", value: summary.pending, icon: <ClockCircleOutlined />, accent: "#595959", bg: "#f5f5f5" },
      { key: "notInvoiced", title: "未开票数量", value: summary.notInvoiced, icon: <FileTextOutlined />, accent: "#1d39c4", bg: "#f0f5ff" },
      { key: "payableCount", title: "应付数量", value: summary.payable, icon: <CreditCardOutlined />, accent: "#a8071a", bg: "#fff1f0" },
      { key: "payable", title: "应付金额", value: summary.payableAmount, precision: 2, icon: <CreditCardOutlined />, accent: "#cf1322", bg: "#fff1f0" },
      { key: "missingInvoice", title: "缺票数量", value: summary.missingInvoice, icon: <ExclamationCircleOutlined />, accent: "#fa8c16", bg: "#fff7e6" },
      { key: "missingInvoiceAmount", title: "缺票金额", value: summary.missingInvoiceAmount, precision: 2, icon: <ExclamationCircleOutlined />, accent: "#d46b08", bg: "#fff7e6" },
      { key: "receivable", title: "应收数量", value: summary.receivable, icon: <BankOutlined />, accent: "#389e0d", bg: "#f6ffed" },
      { key: "receivableAmount", title: "应收金额", value: summary.receivableAmount, precision: 2, icon: <BankOutlined />, accent: "#237804", bg: "#f6ffed" },
      { key: "pendingInvoice", title: "待开票数量", value: summary.pendingInvoice, icon: <FileTextOutlined />, accent: "#531dab", bg: "#f9f0ff" },
      { key: "pendingInvoiceAmount", title: "待开票金额", value: summary.pendingInvoiceAmount, precision: 2, icon: <FileTextOutlined />, accent: "#391085", bg: "#f9f0ff" },
    ],
    [balanceTotal, summary]
  );

  const fetchVouchers = async () => {
    setLoading(true);
    try {
      const response = await financeAPI.getFinanceVouchers();
      setVouchers(normalizeApiData(response));
    } finally {
      setLoading(false);
    }
  };

  const fetchProjects = async () => {
    const response = await projectAPI.getProjectOptions();
    setProjects(normalizeApiData(response));
  };

  const fetchCustomers = async () => {
    const response = await customerAPI.getCustomerOptions();
    setCustomers(normalizeApiData(response));
  };

  const fetchTaxRates = async () => {
    const response = await financeAPI.getTaxRateDicts();
    setTaxRates(normalizeApiData(response));
  };

  const fetchUsers = async () => {
    const response = await permissionAPI.getUserOptions().catch(() => []);
    setUsers(Array.isArray(response) ? response : normalizeApiData(response));
  };

  useEffect(() => {
    fetchVouchers();
    fetchProjects();
    fetchCustomers();
    fetchTaxRates();
    fetchUsers();
  }, []);

  useEffect(() => {
    async function fetchCurrent() {
      try {
        setCurrentUser(await getCurrentUser());
      } catch (error) {
        setCurrentUser(null);
      }
    }

    fetchCurrent();
  }, []);

  const resetPendingVoucherAttachment = () => {
    setPendingVoucherAttachmentFiles([]);
    if (voucherAttachmentInputRef.current) {
      voucherAttachmentInputRef.current.value = "";
    }
  };

  const handleCreate = async (values) => {
    const createdResponse = await financeAPI.createFinanceVoucher(values);
    const createdVoucher = normalizeApiEntity(createdResponse);
    if (pendingVoucherAttachmentFiles.length && createdVoucher?.uuid) {
      await uploadBusinessAttachments("finance-vouchers", createdVoucher.uuid, pendingVoucherAttachmentFiles);
    }
    await fetchVouchers();
    return createdVoucher;
  };

  const handleUpdate = async (uuid, values) => {
    const updatedResponse = await financeAPI.updateFinanceVoucher(uuid, values);
    if (pendingVoucherAttachmentFiles.length) {
      await uploadBusinessAttachments("finance-vouchers", uuid, pendingVoucherAttachmentFiles);
    }
    await fetchVouchers();
    return normalizeApiEntity(updatedResponse);
  };

  const handleDelete = async (uuid) => {
    await financeAPI.deleteFinanceVoucher(uuid);
    await fetchVouchers();
  };

  const handleAudit = async (uuid) => {
    if (!canAuditVouchers) {
      message.error("没有审核财务凭证的权限");
      return;
    }
    await financeAPI.auditFinanceVoucher(uuid);
    message.success("财务凭证已审核");
    await fetchVouchers();
  };

  const openAttachments = (uuid) => {
    navigate(`/attachment/center?businessType=finance-vouchers&businessUuid=${uuid}`);
  };

  const handleExportExcel = (rows = decoratedVouchers) => {
    const exportRows = sortVouchersAscending(rows);
    downloadExcel(
      `finance-vouchers-${new Date().toISOString().slice(0, 10)}.xls`,
      "财务凭证",
      [
        "凭证号",
        "发生日期",
        "项目",
        "一级科目",
        "二级科目",
        "摘要",
        "方向",
        "税率",
        "对方单位",
        "开票状态",
        "发票分类",
        "发票号码",
        "发票金额",
        "差额类型",
        "金额",
        "实际收入",
        "实际支出",
        "记账金额",
        "是否完成",
        "录入人",
        "审核人",
        "备注",
      ],
      exportRows.map((item) => [
        item.voucherNo || "",
        item.occurredOn ? dayjs(item.occurredOn).format("YYYY-MM-DD") : "",
        item.projectLabel || "",
        item.level1Label || "",
        item.level2Label || "",
        item.summary || "",
        item.directionLabel || "",
        item.taxRateLabel || "",
        item.counterpartyNameSnapshot || "",
        item.invoiceStatusLabel || "",
        item.invoiceTypeLabel || "",
        item.invoiceNo || "",
        item.invoiceAmount ?? "",
        item.receivablePayableType || "",
        item.receivablePayableAmount ?? "",
        item.actualIncomeAmount ?? "",
        item.actualExpenseAmount ?? "",
        item.bookedAmount ?? "",
        item.isCompleted ? "是" : "否",
        item.entryUserName || "",
        item.auditorUserName || "",
        item.remark || "",
      ])
    );
  };

  const handleExportPdf = async (rows = decoratedVouchers) => {
    const exportRows = sortVouchersAscending(rows);
    if (!exportRows.length) {
      return;
    }

    const incomeTotal = exportRows.reduce((sum, item) => sum + Number(item.actualIncomeAmount || 0), 0);
    const expenseTotal = exportRows.reduce((sum, item) => sum + Number(item.actualExpenseAmount || 0), 0);
    const incomeBookedTotal = exportRows.reduce(
      (sum, item) => sum + (item.transactionDirection === "RECEIVE" ? Number(item.bookedAmount || 0) : 0),
      0
    );
    const expenseBookedTotal = exportRows.reduce(
      (sum, item) => sum + (item.transactionDirection === "PAY" ? Math.abs(Number(item.bookedAmount || 0)) : 0),
      0
    );
    const payableAmountTotal = exportRows.reduce((sum, item) => sum + calculatePayableAmount(item), 0);
    const missingInvoiceAmountTotal = exportRows.reduce(
      (sum, item) => sum + (item.receivablePayableType === "缺票" ? Number(item.receivablePayableAmount || 0) : 0),
      0
    );
    const payableCount = exportRows.reduce((sum, item) => sum + (item.receivablePayableType === "应付" ? 1 : 0), 0);
    const receivableAmountTotal = exportRows.reduce(
      (sum, item) => sum + (item.receivablePayableType === "应收" ? Number(item.receivablePayableAmount || 0) : 0),
      0
    );
    const pendingInvoiceAmountTotal = exportRows.reduce(
      (sum, item) => sum + (item.receivablePayableType === "待开票" ? Number(item.receivablePayableAmount || 0) : 0),
      0
    );
    const balanceTotal = incomeBookedTotal - expenseBookedTotal;
    const payload = {
      title: "财务凭证台账",
      subtitle: `生成时间：${dayjs().format("YYYY-MM-DD HH:mm")}`,
      fileName: `finance-vouchers-${new Date().toISOString().slice(0, 10)}.pdf`,
      summaries: [
        { label: "记录数", value: String(exportRows.length) },
        { label: "收入合计", value: incomeTotal.toFixed(2) },
        { label: "支出合计", value: expenseTotal.toFixed(2) },
        { label: "支出记账合计", value: expenseBookedTotal.toFixed(2) },
        { label: "收入记账合计", value: incomeBookedTotal.toFixed(2) },
        { label: "结余合计", value: balanceTotal.toFixed(2) },
        { label: "应付数量", value: String(payableCount) },
        { label: "应付金额", value: payableAmountTotal.toFixed(2) },
        { label: "缺票金额", value: missingInvoiceAmountTotal.toFixed(2) },
        { label: "应收金额", value: receivableAmountTotal.toFixed(2) },
        { label: "待开票金额", value: pendingInvoiceAmountTotal.toFixed(2) },
      ],
      columns: [
        { header: "序号", align: "center", width: 5 },
        { header: "凭证号", width: 9 },
        { header: "发生日期", align: "center", width: 8 },
        { header: "项目", width: 13 },
        { header: "一级科目", width: 7 },
        { header: "二级科目", width: 8 },
        { header: "摘要", width: 12 },
        { header: "方向", align: "center", width: 5 },
        { header: "税率", align: "center", width: 5 },
        { header: "对方单位", width: 10 },
        { header: "开票状态", align: "center", width: 6 },
        { header: "发票分类", align: "center", width: 9 },
        { header: "发票号码", width: 8 },
        { header: "发票金额", align: "right", width: 7 },
        { header: "差额类型", align: "center", width: 6 },
        { header: "金额", align: "right", width: 7 },
        { header: "收入", align: "right", width: 7 },
        { header: "支出", align: "right", width: 7 },
        { header: "记账金额", align: "right", width: 7 },
        { header: "完成状态", align: "center", width: 6 },
        { header: "录入人", align: "center", width: 6 },
        { header: "审核人", align: "center", width: 6 },
      ],
      rows: exportRows.map((item, index) => [
        index + 1,
        item.voucherNo || "",
        item.occurredOn ? dayjs(item.occurredOn).format("YYYY-MM-DD") : "",
        item.projectLabel || "",
        item.level1Label || "",
        item.level2Label || "",
        item.summary || "",
        item.directionLabel || "",
        item.taxRateLabel || "",
        item.counterpartyNameSnapshot || "",
        item.invoiceStatusLabel || "",
        item.invoiceTypeLabel || "",
        item.invoiceNo || "",
        item.invoiceAmount ?? "",
        item.receivablePayableType || "",
        item.receivablePayableAmount ?? "",
        item.actualIncomeAmount ?? "",
        item.actualExpenseAmount ?? "",
        item.bookedAmount ?? "",
        item.isCompleted ? "是" : "否",
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

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Row gutter={[16, 16]}>
        {statCards.map((item) => (
          <Col xs={24} sm={12} xl={6} key={item.key}>
            <Card
              styles={{
                body: {
                  borderTop: `3px solid ${item.accent}`,
                  borderRadius: 12,
                },
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <div
                  style={{
                    width: 44,
                    height: 44,
                    borderRadius: 12,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: item.accent,
                    background: item.bg,
                    fontSize: 20,
                    flexShrink: 0,
                  }}
                >
                  {item.icon}
                </div>
                <Statistic title={item.title} value={item.value} precision={item.precision} />
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <CRUDTable
        title="财务凭证"
        columns={[
          {
            title: "序号",
            key: "serialNo",
            width: 80,
            render: (_, __, index) => index + 1,
            sorter: (left, right) => (left.id || 0) - (right.id || 0),
          },
          { title: "凭证号", dataIndex: "voucherNo", key: "voucherNo", width: 140 },
          {
            title: "发生日期",
            dataIndex: "occurredOn",
            key: "occurredOn",
            width: 120,
            render: (value) => (value ? dayjs(value).format("YYYY-MM-DD") : "-"),
          },
          { title: "项目", dataIndex: "projectLabel", key: "projectLabel", width: 220 },
          { title: "一级科目", dataIndex: "level1Label", key: "level1Label", width: 120 },
          { title: "二级科目", dataIndex: "level2Label", key: "level2Label", width: 180 },
          { title: "摘要", dataIndex: "summary", key: "summary", width: 220 },
          { title: "方向", dataIndex: "directionLabel", key: "directionLabel", width: 100 },
          { title: "税率", dataIndex: "taxRateLabel", key: "taxRateLabel", width: 100 },
          { title: "对方单位", dataIndex: "counterpartyNameSnapshot", key: "counterpartyNameSnapshot", width: 180 },
          { title: "开票状态", dataIndex: "invoiceStatusLabel", key: "invoiceStatusLabel", width: 120 },
          { title: "发票分类", dataIndex: "invoiceTypeLabel", key: "invoiceTypeLabel", width: 160, render: (value) => value || "-" },
          { title: "发票号码", dataIndex: "invoiceNo", key: "invoiceNo", width: 160 },
          {
            title: "发票金额",
            dataIndex: "invoiceAmount",
            key: "invoiceAmount",
            width: 120,
            render: formatAmount,
          },
          {
            title: "实际收入",
            dataIndex: "actualIncomeAmount",
            key: "actualIncomeAmount",
            width: 120,
            onCell: (record) => (!record.isCompleted ? { style: { background: "#fff1f0" } } : {}),
            render: formatAmount,
          },
          {
            title: "实际支出",
            dataIndex: "actualExpenseAmount",
            key: "actualExpenseAmount",
            width: 120,
            onCell: (record) => (!record.isCompleted ? { style: { background: "#fff1f0" } } : {}),
            render: formatAmount,
          },
          {
            title: "记账金额",
            dataIndex: "bookedAmount",
            key: "bookedAmount",
            width: 120,
            render: (value) => {
              const color = Number(value || 0) < 0 ? "error" : "processing";
              return <Tag color={color}>{formatAmount(value)}</Tag>;
            },
          },
          {
            title: "差额类型",
            dataIndex: "receivablePayableType",
            key: "receivablePayableType",
            width: 120,
            render: (value) => (value ? <Tag color="geekblue">{value}</Tag> : "-"),
          },
          {
            title: "金额",
            dataIndex: "receivablePayableAmount",
            key: "receivablePayableAmount",
            width: 160,
            render: (value) =>
              value == null ? "-" : <Tag color={Number(value) === 0 ? "success" : "warning"}>{formatAmount(value)}</Tag>,
          },
          {
            title: "完成状态",
            dataIndex: "isCompleted",
            key: "isCompleted",
            width: 100,
            render: (value) => (value ? <Tag color="success">是</Tag> : <Tag color="warning">否</Tag>),
          },
          { title: "录入人", dataIndex: "entryUserName", key: "entryUserName", width: 120, render: (value) => value || "-" },
          { title: "审核人", dataIndex: "auditorUserName", key: "auditorUserName", width: 120, render: (value) => value || "-" },
          { title: "备注", dataIndex: "remark", key: "remark", width: 200 },
        ]}
        dataSource={visibleVouchers}
        onFilteredDataChange={setFilteredVisibleVouchers}
        loading={loading}
        onCreate={handleCreate}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
        onModalCancel={resetPendingVoucherAttachment}
        onModalSuccess={resetPendingVoucherAttachment}
        rowActions={({ record }) => [
          canAccessAttachments ? (
            <Button key="attachments" type="link" icon={<LinkOutlined />} onClick={() => openAttachments(record.uuid)} size="small">
              查看附件
            </Button>
          ) : null,
          <Button
            key="audit"
            type="link"
            icon={<CheckCircleOutlined />}
            onClick={() => handleAudit(record.uuid)}
            disabled={!canAuditVouchers || Boolean(record.auditorUser)}
            size="small"
          >
            审核
          </Button>,
        ]}
        rowKey="uuid"
        createAuthorities={["finance.voucher.entry", "finance.manage"]}
        updateAuthorities={["finance.voucher.entry", "finance.manage"]}
        deleteAuthorities={["finance.voucher.entry", "finance.manage"]}
        extraActions={({ filteredData, canExport }) => (
          <>
            <Button
              icon={<DownloadOutlined />}
              onClick={() => handleExportExcel(filteredData)}
              disabled={!canExport || !filteredData.length}
            >
              导出 Excel
            </Button>
            <Button
              icon={<FilePdfOutlined />}
              onClick={() => handleExportPdf(filteredData)}
              disabled={!canExport || !filteredData.length}
            >
              导出 PDF
            </Button>
          </>
        )}
        searchFields={[
          { name: "searchText", label: "关键字", placeholder: "搜索所有列" },
          { name: "voucherNo", label: "凭证号" },
          {
            name: "occurredOnRange",
            label: "发生日期",
            component: (
              <DatePicker.RangePicker
                allowClear
                placeholder={["开始日期", "结束日期"]}
                style={{ width: 240 }}
              />
            ),
            filter: (item, value) => isDateInInclusiveRange(item.occurredOn, value),
          },
          {
            name: "projectLabel",
            label: "项目",
            component: (
              <Select placeholder="筛选项目" allowClear showSearch optionFilterProp="children">
                {projects.map((item) => (
                  <Option key={item.uuid} value={formatProjectOptionLabel(item)}>
                    {formatProjectOptionLabel(item)}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "level1Label",
            label: "一级科目",
            component: (
              <Select placeholder="筛选一级科目" allowClear showSearch optionFilterProp="children">
                {LEVEL_1_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.label}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "level2Label",
            label: "二级科目",
            component: (
              <Select placeholder="筛选二级科目" allowClear showSearch optionFilterProp="children">
                {LEVEL_2_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.label}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          { name: "summary", label: "摘要" },
          {
            name: "directionLabel",
            label: "方向",
            component: (
              <Select placeholder="筛选方向" allowClear showSearch optionFilterProp="children">
                {DIRECTION_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.label}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "taxRateLabel",
            label: "税率",
            component: (
              <Select placeholder="筛选税率" allowClear showSearch optionFilterProp="children">
                {activeTaxRates.map((item) => {
                  const label = item.label || `${Number(item.rate || 0) * 100}%`;
                  return (
                    <Option key={item.id || item.rate} value={label}>
                      {label}
                    </Option>
                  );
                })}
              </Select>
            ),
          },
          {
            name: "counterpartyNameSnapshot",
            label: "对方单位",
            component: (
              <Select placeholder="筛选对方单位" allowClear showSearch optionFilterProp="children">
                {customers.map((item) => (
                  <Option key={item.uuid} value={item.customerName}>
                    {item.customerName}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "invoiceStatusLabel",
            label: "开票状态",
            component: (
              <Select placeholder="筛选开票状态" allowClear showSearch optionFilterProp="children">
                {INVOICE_STATUS_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.label}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "invoiceTypeLabel",
            label: "发票分类",
            component: (
              <Select placeholder="筛选发票分类" allowClear showSearch optionFilterProp="children">
                {INVOICE_TYPE_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.label}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          { name: "invoiceNo", label: "发票号码" },
          { name: "invoiceAmountLabel", label: "发票金额" },
          {
            name: "receivablePayableType",
            label: "差额类型",
            component: (
              <Select placeholder="筛选差额类型" allowClear showSearch optionFilterProp="children">
                {["应收", "应付", "待开票", "缺票"].map((item) => (
                  <Option key={item} value={item}>
                    {item}
                  </Option>
                ))}
              </Select>
            ),
          },
          { name: "receivablePayableAmountLabel", label: "差额金额" },
          { name: "actualIncomeAmountLabel", label: "实际收入" },
          { name: "actualExpenseAmountLabel", label: "实际支出" },
          { name: "bookedAmountLabel", label: "记账金额" },
          { name: "entryUserName", label: "录入人" },
          { name: "auditorUserName", label: "审核人" },
          {
            name: "isCompletedLabel",
            label: "完成状态",
            component: (
              <Select placeholder="筛选完成状态" allowClear showSearch optionFilterProp="children">
                {["已完成", "未完成"].map((item) => (
                  <Option key={item} value={item}>
                    {item}
                  </Option>
                ))}
              </Select>
            ),
          },
          { name: "remark", label: "备注" },
        ]}
        detailFields={[
          { key: "voucherNo", label: "凭证号" },
          { key: "projectLabel", label: "项目" },
          { key: "occurredOn", label: "发生日期" },
          { key: "level1Label", label: "一级科目" },
          { key: "level2Label", label: "二级科目" },
          { key: "summary", label: "摘要" },
          { key: "directionLabel", label: "方向" },
          { key: "taxRateLabel", label: "税率" },
          { key: "counterpartyNameSnapshot", label: "对方单位" },
          { key: "invoiceStatusLabel", label: "开票状态" },
          { key: "invoiceTypeLabel", label: "发票分类" },
          { key: "invoiceNo", label: "发票号码" },
          { key: "invoiceAmount", label: "发票金额" },
          { key: "receivablePayableType", label: "差额类型" },
          { key: "receivablePayableAmount", label: "金额" },
          { key: "actualIncomeAmount", label: "实际收入" },
          { key: "actualExpenseAmount", label: "实际支出" },
          { key: "bookedAmount", label: "记账金额" },
          { key: "isCompleted", label: "完成状态" },
          { key: "entryUserName", label: "录入人" },
          { key: "auditorUserName", label: "审核人" },
          { key: "remark", label: "备注" },
        ]}
        mapRecordToFormValues={(record) => ({
          ...record,
          counterpartyCustomerId:
            record.counterpartyCustomerId ||
            customerNameToUuidMap[record.counterpartyNameSnapshot] ||
            undefined,
          occurredOn: normalizeDateOnlyValue(record.occurredOn),
          entryUser: record.entryUser || undefined,
          auditorUser: record.auditorUser || undefined,
        })}
        transformValues={(values) => ({
          ...(() => {
            validateDirectionalAmounts(values);
            return {};
          })(),
          ...values,
          projectId: values.projectId || projectIdFilter || null,
          counterpartyNameSnapshot: undefined,
          invoiceType: values.invoiceStatus === "INVOICED" ? values.invoiceType : null,
          invoiceAmount:
            values.invoiceAmount === undefined || values.invoiceAmount === null || values.invoiceAmount === ""
              ? null
              : Number(values.invoiceAmount),
          occurredOn: values.occurredOn ? values.occurredOn.format("YYYY-MM-DD") : null,
          taxRate:
            values.taxRate === undefined || values.taxRate === null || values.taxRate === ""
              ? null
              : Number(values.taxRate),
          entryUser: values.entryUser ?? null,
          auditorUser: values.auditorUser ?? null,
          ...normalizeDirectionalAmounts(values),
        })}
        formFields={[
          {
            name: "voucherNo",
            label: "凭证号",
            component: <Input placeholder="保存后自动生成" disabled />,
          },
          {
            name: "occurredOn",
            label: "发生日期",
            rules: [{ required: true, message: "请选择发生日期" }],
            component: ({ editingRecord }) => (
              <DatePicker
                style={{ width: "100%" }}
                disabled={Boolean(editingRecord)}
                placeholder={editingRecord ? "发生日期不可修改" : "请选择发生日期"}
              />
            ),
          },
          {
            name: "projectId",
            label: "项目",
            getDefaultValue: () => projectIdFilter || undefined,
            rules: [{ required: true, message: "请选择项目" }],
            component: (
              <Select
                placeholder="请选择项目"
                showSearch
                optionFilterProp="children"
                disabled={Boolean(projectIdFilter)}
              >
                {projects.map((item) => (
                  <Option key={item.uuid} value={item.uuid}>
                    {formatProjectOptionLabel(item)}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "level1Subject",
            label: "一级科目",
            rules: [{ required: true, message: "请选择一级科目" }],
            component: (
              <Select placeholder="请选择一级科目">
                {LEVEL_1_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.value}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "level2Subject",
            label: "二级科目",
            rules: [{ required: true, message: "请选择二级科目" }],
            component: (
              <Select placeholder="请选择二级科目" showSearch optionFilterProp="children">
                {LEVEL_2_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.value}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "summary",
            label: "摘要",
            rules: [{ required: true, message: "请输入摘要" }],
            component: <Input placeholder="请输入摘要" />,
          },
          {
            name: "transactionDirection",
            label: "方向",
            rules: [{ required: true, message: "请选择方向" }],
            component: (
              <Select placeholder="请选择方向">
                {DIRECTION_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.value}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            key: "voucher-direction-sync",
            renderOnly: true,
            render: ({ form }) => <VoucherDirectionSync form={form} />,
          },
          {
            key: "voucher-amount-fields",
            renderOnly: true,
            render: ({ form }) => <VoucherAmountFields form={form} incomeLabel="实际收入" expenseLabel="实际支出" />,
          },
          {
            key: "voucher-booked-amount-preview",
            renderOnly: true,
            render: ({ form }) => (
              <Form.Item label="记账金额（自动计算）">
                <BookedAmountPreview form={form} />
              </Form.Item>
            ),
          },
          {
            name: "taxRate",
            label: "税率",
            rules: [{ required: true, message: "请选择税率" }],
            component: (
              <Select placeholder="请选择税率">
                {taxRateOptions.map((item) => (
                  <Option key={item.value} value={item.value}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "counterpartyCustomerId",
            label: "对方单位",
            rules: [{ required: true, message: "请选择对方单位" }],
            component: (
              <Select placeholder="请选择对方单位" showSearch optionFilterProp="children">
                {customers.map((item) => (
                  <Option key={item.uuid} value={item.uuid}>
                    {item.customerName}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            name: "invoiceStatus",
            label: "开票状态",
            getDefaultValue: () => "NOT_INVOICED",
            rules: [{ required: true, message: "请选择开票状态" }],
            component: (
              <Select placeholder="请选择开票状态">
                {INVOICE_STATUS_OPTIONS.map((item) => (
                  <Option key={item.value} value={item.value}>
                    {item.label}
                  </Option>
                ))}
              </Select>
            ),
          },
          {
            key: "voucher-invoice-type",
            renderOnly: true,
            render: ({ form }) => <InvoiceTypeField form={form} />,
          },
          {
            name: "invoiceNo",
            label: "发票号码",
            component: <Input placeholder="请输入发票号码" maxLength={128} />,
          },
          {
            name: "invoiceAmount",
            label: "发票金额",
            component: <InputNumber min={0} precision={2} style={{ width: "100%" }} placeholder="请输入发票金额" />,
          },
          {
            key: "voucher-receivable-payable-preview",
            renderOnly: true,
            render: ({ form }) => (
              <Space direction="vertical" size={0} style={{ width: "100%" }}>
                <Form.Item label="差额类型（自动判断)" style={{ marginBottom: 16 }}>
                  <ReceivablePayableTypePreview form={form} />
                </Form.Item>
                <Form.Item label="金额（自动计算）">
                  <ReceivablePayableAmountPreview form={form} />
                </Form.Item>
              </Space>
            ),
          },
          {
            name: "isCompleted",
            label: "完成状态",
            rules: [{ required: true, message: "请选择完成状态" }],
            valuePropName: "checked",
            component: <Switch checkedChildren="是" unCheckedChildren="否" />,
          },
          {
            name: "entryUser",
            label: "录入人",
            visible: ({ editingRecord }) => Boolean(editingRecord) && canMaintainEntryAudit,
            component: (
              <Select placeholder="请选择录入人" allowClear showSearch optionFilterProp="children">
                {userOptions}
              </Select>
            ),
          },
          {
            name: "auditorUser",
            label: "审核人",
            visible: ({ editingRecord }) => Boolean(editingRecord) && canMaintainEntryAudit,
            component: (
              <Select placeholder="请选择审核人" allowClear showSearch optionFilterProp="children">
                {userOptions}
              </Select>
            ),
          },
          { name: "remark", label: "备注", component: <Input.TextArea rows={3} placeholder="请输入备注" /> },
          {
            key: "voucher-attachment-upload",
            renderOnly: true,
            render: ({ editingRecord }) => (
              <BusinessAttachmentUpload
                title="票据附件"
                businessType="finance-vouchers"
                businessUuid={editingRecord?.uuid}
                pendingFiles={pendingVoucherAttachmentFiles}
                onPendingFilesChange={setPendingVoucherAttachmentFiles}
                inputRef={voucherAttachmentInputRef}
                canAccess={canAccessAttachments}
                canManage={canManageAttachments}
                chooseText={editingRecord ? "继续添加票据附件" : "选择票据附件"}
                helpText="保存财务凭证时会自动上传并关联当前票据附件，可一次选择多个文件。"
                noManageText="当前账号没有附件上传权限，无法在这里上传票据附件。"
                onOpenAttachments={editingRecord ? () => openAttachments(editingRecord.uuid) : undefined}
              />
            ),
          },
        ]}
      />
    </Space>
  );
}
