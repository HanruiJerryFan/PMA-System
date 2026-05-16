package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.TaxRateDictMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.TaxRateDict;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.FinanceVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FinanceVoucherServiceImpl implements FinanceVoucherService {

    private static final Set<String> LEVEL_1_SUBJECTS = Set.of("NON_PROJECT", "PROJECT");
    private static final Set<String> TRANSACTION_DIRECTIONS = Set.of("RECEIVE", "PAY");
    private static final Set<String> INVOICE_STATUSES = Set.of("NOT_INVOICED", "INVOICED");
    private static final Set<String> LEVEL_2_SUBJECTS = Set.of(
            "EQUIPMENT_PURCHASE",
            "AUXILIARY_MATERIAL_PURCHASE",
            "CONSTRUCTION_FEE",
            "SALES_EXPENSE",
            "MISCELLANEOUS",
            "AMORTIZATION",
            "PROJECT_RECEIPT",
            "WAREHOUSE_TRANSFER_IN",
            "PROJECT_TRANSFER_OUT",
            "SALARY",
            "TRAVEL",
            "ENTERTAINMENT",
            "CONFERENCE",
            "VEHICLE",
            "OTHER"
    );
    private static final BigDecimal THIRTEEN_PERCENT = new BigDecimal("0.13");
    private static final BigDecimal ZERO_TAX_EFFECTIVE_RATE = new BigDecimal("-0.05");
    private static final String SALARY_SUBJECT = "SALARY";
    private static final Pattern VOUCHER_NO_PATTERN = Pattern.compile("^\\d{10}$");
    private static final DateTimeFormatter VOUCHER_DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter VOUCHER_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    private FinanceVoucherMapper mapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaxRateDictMapper taxRateDictMapper;

    @Autowired
    private CodeSequenceService codeSequenceService;

    @Autowired
    private CustomerService customerService;

    @Override
    public FinanceVoucher getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<FinanceVoucher> getAll() {
        return mapper.selectAll();
    }

    @Override
    @Transactional
    public FinanceVoucher create(FinanceVoucher voucher) {
        if (!StringUtils.hasText(voucher.getUuid())) {
            voucher.setUuid(UUID.randomUUID().toString());
        }
        if (!StringUtils.hasText(voucher.getVoucherNo())) {
            voucher.setVoucherNo(generateVoucherNo(voucher.getOccurredOn()));
        }
        validateVoucher(voucher);
        voucher.setBookedAmount(calculateBookedAmount(voucher));
        mapper.insert(voucher);
        customerService.touchActivity(voucher.getCounterpartyCustomerId());
        return voucher;
    }

    @Override
    @Transactional
    public FinanceVoucher update(FinanceVoucher voucher) {
        FinanceVoucher existing = mapper.selectByUuid(voucher.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Finance voucher does not exist");
        }
        if (voucher.getOccurredOn() == null) {
            voucher.setOccurredOn(existing.getOccurredOn());
        } else if (!voucher.getOccurredOn().equals(existing.getOccurredOn())) {
            throw new IllegalArgumentException("Occurred date cannot be changed");
        }
        if (!StringUtils.hasText(voucher.getVoucherNo())) {
            voucher.setVoucherNo(existing.getVoucherNo());
        }
        validateVoucher(voucher);
        voucher.setBookedAmount(calculateBookedAmount(voucher));
        mapper.updateByUuid(voucher);
        customerService.touchActivity(voucher.getCounterpartyCustomerId());
        return voucher;
    }

    @Override
    public void delete(String uuid) {
        mapper.deleteByUuid(uuid);
    }

    private void validateVoucher(FinanceVoucher voucher) {
        if (voucher.getOccurredOn() == null) {
            throw new IllegalArgumentException("Occurred date is required");
        }
        if (!StringUtils.hasText(voucher.getVoucherNo()) || !VOUCHER_NO_PATTERN.matcher(voucher.getVoucherNo()).matches()) {
            throw new IllegalArgumentException("Voucher number must be 10 digits");
        }
        FinanceVoucher existingByVoucherNo = mapper.selectByVoucherNo(voucher.getVoucherNo().trim());
        if (existingByVoucherNo != null && !sameVoucher(existingByVoucherNo, voucher)) {
            throw new IllegalArgumentException("Voucher number already exists");
        }
        if (!StringUtils.hasText(voucher.getLevel1Subject()) || !LEVEL_1_SUBJECTS.contains(voucher.getLevel1Subject())) {
            throw new IllegalArgumentException("Level 1 subject is invalid");
        }
        if (!StringUtils.hasText(voucher.getProjectId())) {
            throw new IllegalArgumentException("Project is required");
        }
        if (projectMapper.selectByUuid(voucher.getProjectId()) == null) {
            throw new IllegalArgumentException("Project does not exist");
        }
        if (!StringUtils.hasText(voucher.getLevel2Subject()) || !LEVEL_2_SUBJECTS.contains(voucher.getLevel2Subject())) {
            throw new IllegalArgumentException("Level 2 subject is invalid");
        }
        if (!StringUtils.hasText(voucher.getSummary())) {
            throw new IllegalArgumentException("Summary is required");
        }
        if (!StringUtils.hasText(voucher.getTransactionDirection())
                || !TRANSACTION_DIRECTIONS.contains(voucher.getTransactionDirection())) {
            throw new IllegalArgumentException("Transaction direction is invalid");
        }
        if (voucher.getTaxRate() == null) {
            throw new IllegalArgumentException("Tax rate is invalid");
        }
        BigDecimal normalizedTaxRate = voucher.getTaxRate().setScale(2, RoundingMode.HALF_UP);
        TaxRateDict taxRateDict = taxRateDictMapper.selectByRate(normalizedTaxRate);
        if (taxRateDict == null || !Boolean.TRUE.equals(taxRateDict.getIsActive())) {
            throw new IllegalArgumentException("Tax rate is invalid");
        }
        voucher.setTaxRate(normalizedTaxRate);

        if (!StringUtils.hasText(voucher.getCounterpartyCustomerId())) {
            throw new IllegalArgumentException("Counterparty customer is required");
        }
        Customer counterpartyCustomer = customerMapper.selectByUuid(voucher.getCounterpartyCustomerId().trim());
        if (counterpartyCustomer == null) {
            throw new IllegalArgumentException("Counterparty customer does not exist");
        }
        voucher.setCounterpartyCustomerId(counterpartyCustomer.getUuid());
        voucher.setCounterpartyNameSnapshot(counterpartyCustomer.getCustomerName());
        if (!StringUtils.hasText(voucher.getInvoiceStatus())) {
            voucher.setInvoiceStatus("NOT_INVOICED");
        }
        if (!INVOICE_STATUSES.contains(voucher.getInvoiceStatus())) {
            throw new IllegalArgumentException("Invoice status is invalid");
        }
        if (StringUtils.hasText(voucher.getInvoiceNo())) {
            voucher.setInvoiceNo(voucher.getInvoiceNo().trim());
        } else {
            voucher.setInvoiceNo(null);
        }
        if (voucher.getInvoiceAmount() != null && voucher.getInvoiceAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invoice amount cannot be negative");
        }
        if (voucher.getIsCompleted() == null) {
            throw new IllegalArgumentException("Completion status is required");
        }

        BigDecimal income = defaultZero(voucher.getActualIncomeAmount());
        BigDecimal expense = defaultZero(voucher.getActualExpenseAmount());
        if (voucher.getActualIncomeAmount() != null && voucher.getActualIncomeAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Actual income amount cannot be negative");
        }
        if (voucher.getActualExpenseAmount() != null && voucher.getActualExpenseAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Actual expense amount cannot be negative");
        }
        if (income.compareTo(BigDecimal.ZERO) > 0 && expense.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Income and expense cannot both be greater than 0");
        }
        if ("RECEIVE".equals(voucher.getTransactionDirection()) && voucher.getActualIncomeAmount() == null) {
            throw new IllegalArgumentException("Actual income amount is required for receive vouchers");
        }
        if ("PAY".equals(voucher.getTransactionDirection()) && voucher.getActualExpenseAmount() == null) {
            throw new IllegalArgumentException("Actual expense amount is required for pay vouchers");
        }
        if ("RECEIVE".equals(voucher.getTransactionDirection()) && expense.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Receive vouchers cannot set a positive expense amount");
        }
        if ("PAY".equals(voucher.getTransactionDirection()) && income.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Pay vouchers cannot set a positive income amount");
        }
    }

    private BigDecimal calculateBookedAmount(FinanceVoucher voucher) {
        if ("PAY".equals(voucher.getTransactionDirection())) {
            return calculatePayBookedAmount(voucher);
        }
        return calculateReceiveBookedAmount(voucher);
    }

    private BigDecimal calculateReceiveBookedAmount(FinanceVoucher voucher) {
        BigDecimal baseAmount = defaultZero(voucher.getActualIncomeAmount());
        if (baseAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal normalizedTaxRate = voucher.getTaxRate().setScale(2, RoundingMode.HALF_UP);
        BigDecimal rateGap = THIRTEEN_PERCENT.subtract(normalizedTaxRate);
        BigDecimal denominator = BigDecimal.ONE.subtract(rateGap);
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Invalid tax rate denominator");
        }

        return baseAmount.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePayBookedAmount(FinanceVoucher voucher) {
        BigDecimal baseAmount = defaultZero(voucher.getActualExpenseAmount());
        if (baseAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal denominator;
        if (SALARY_SUBJECT.equals(voucher.getLevel2Subject())) {
            denominator = BigDecimal.ONE.subtract(THIRTEEN_PERCENT);
        } else {
            BigDecimal selectedTaxRate = voucher.getTaxRate().setScale(2, RoundingMode.HALF_UP);
            BigDecimal effectiveTaxRate =
                    selectedTaxRate.compareTo(BigDecimal.ZERO) == 0 ? ZERO_TAX_EFFECTIVE_RATE : selectedTaxRate;
            BigDecimal rateGap = THIRTEEN_PERCENT.subtract(effectiveTaxRate);
            denominator = BigDecimal.ONE.subtract(rateGap);
        }

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Invalid tax rate denominator");
        }

        BigDecimal bookedAmount = baseAmount.divide(denominator, 2, RoundingMode.HALF_UP);
        if ("PROJECT_TRANSFER_OUT".equals(voucher.getLevel2Subject())) {
            bookedAmount = bookedAmount.negate();
        }
        return bookedAmount;
    }

    private String generateVoucherNo(LocalDate occurredOn) {
        if (occurredOn == null) {
            throw new IllegalArgumentException("Occurred date is required");
        }
        String prefix = occurredOn.format(VOUCHER_DAY_FORMATTER);
        String monthPrefix = occurredOn.format(VOUCHER_MONTH_FORMATTER);
        Integer currentMaxSequence = mapper.selectMaxVoucherSequenceByMonthPrefix(monthPrefix);
        int nextSequence = codeSequenceService.nextValue(
                "finance_voucher:" + monthPrefix,
                currentMaxSequence == null ? 0 : currentMaxSequence,
                99,
                "Finance voucher monthly sequence for " + monthPrefix
        );
        return prefix + String.format("%02d", nextSequence);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean sameVoucher(FinanceVoucher existing, FinanceVoucher incoming) {
        return existing != null
                && incoming != null
                && StringUtils.hasText(existing.getUuid())
                && existing.getUuid().equals(incoming.getUuid());
    }
}
