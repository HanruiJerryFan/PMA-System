package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.TaxRateDictMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.FinanceSubject;
import com.jerry.salesmanagement.pojo.TaxRateDict;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.EntryAuditService;
import com.jerry.salesmanagement.service.FinanceVoucherService;
import com.jerry.salesmanagement.service.FinanceSubjectService;
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

    private static final Set<String> TRANSACTION_DIRECTIONS = Set.of("RECEIVE", "PAY");
    private static final Set<String> INVOICE_STATUSES = Set.of("NOT_INVOICED", "INVOICED");
    private static final Set<String> INVOICE_TYPES = Set.of("VAT_ORDINARY", "VAT_SPECIAL");
    private static final BigDecimal THIRTEEN_PERCENT = new BigDecimal("0.13");
    private static final BigDecimal EIGHTEEN_PERCENT = new BigDecimal("0.18");
    private static final BigDecimal ELEVEN_POINT_FIVE_PERCENT = new BigDecimal("0.115");
    private static final Set<String> FIXED_THIRTEEN_PERCENT_EXPENSE_SUBJECTS = Set.of("SALARY", "TRAVEL");
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

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private EntryAuditService entryAuditService;

    @Autowired
    private FinanceSubjectService financeSubjectService;

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
        Long currentUserId = currentUserService.requireCurrentUserId();
        entryAuditService.applyCreate(voucher);
        voucher.setCreatedBy(currentUserId);
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
        entryAuditService.applyUpdate(voucher, existing);
        voucher.setUpdatedBy(currentUserService.requireCurrentUserId());
        validateVoucher(voucher);
        voucher.setBookedAmount(calculateBookedAmount(voucher));
        mapper.updateByUuid(voucher);
        customerService.touchActivity(voucher.getCounterpartyCustomerId());
        return mapper.selectByUuid(voucher.getUuid());
    }

    @Override
    @Transactional
    public FinanceVoucher audit(String uuid) {
        FinanceVoucher existing = mapper.selectByUuid(uuid);
        if (existing == null) {
            throw new IllegalArgumentException("Finance voucher does not exist");
        }
        entryAuditService.applyAudit(existing);
        mapper.updateAuditorByUuid(uuid, existing.getAuditorUser(), currentUserService.requireCurrentUserId());
        return mapper.selectByUuid(uuid);
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
        FinanceSubject level1 = financeSubjectService.validateSubject(1, voucher.getLevel1SubjectId());
        voucher.setLevel1SubjectName(level1.getSubjectName());
        if (!StringUtils.hasText(voucher.getProjectId())) {
            throw new IllegalArgumentException("Project is required");
        }
        if (projectMapper.selectByUuid(voucher.getProjectId()) == null) {
            throw new IllegalArgumentException("Project does not exist");
        }
        FinanceSubject level2 = financeSubjectService.validateSubject(2, voucher.getLevel2SubjectId());
        voucher.setLevel2SubjectName(level2.getSubjectName());
        voucher.setLevel2SubjectCode(level2.getSubjectCode());
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
        if ("INVOICED".equals(voucher.getInvoiceStatus())) {
            if (!StringUtils.hasText(voucher.getInvoiceType()) || !INVOICE_TYPES.contains(voucher.getInvoiceType())) {
                throw new IllegalArgumentException("Invoice type is required for invoiced vouchers");
            }
        } else {
            voucher.setInvoiceType(null);
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

        BigDecimal denominator = calculateBookkeepingDenominator(voucher);
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

        BigDecimal denominator = calculateBookkeepingDenominator(voucher);
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Invalid tax rate denominator");
        }

        BigDecimal bookedAmount = baseAmount.divide(denominator, 2, RoundingMode.HALF_UP);
        if ("PROJECT_TRANSFER_OUT".equals(voucher.getLevel2SubjectCode())) {
            bookedAmount = bookedAmount.negate();
        }
        return bookedAmount;
    }

    private BigDecimal calculateBookkeepingDenominator(FinanceVoucher voucher) {
        if ("PAY".equals(voucher.getTransactionDirection())
                && FIXED_THIRTEEN_PERCENT_EXPENSE_SUBJECTS.contains(voucher.getLevel2SubjectCode())) {
            return BigDecimal.ONE.subtract(THIRTEEN_PERCENT);
        }

        BigDecimal selectedTaxRate = voucher.getTaxRate().setScale(2, RoundingMode.HALF_UP);
        if (selectedTaxRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal deductionRate = "INVOICED".equals(voucher.getInvoiceStatus())
                    ? ELEVEN_POINT_FIVE_PERCENT
                    : EIGHTEEN_PERCENT;
            return BigDecimal.ONE.subtract(deductionRate);
        }

        BigDecimal rateGap = THIRTEEN_PERCENT.subtract(selectedTaxRate);
        return BigDecimal.ONE.subtract(rateGap);
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
