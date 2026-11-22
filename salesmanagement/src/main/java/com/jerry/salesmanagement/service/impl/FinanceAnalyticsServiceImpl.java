package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.FinanceVoucherMetrics;
import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.ContractTypeMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.FinanceSubjectMapper;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import com.jerry.salesmanagement.pojo.ContractType;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.FinanceSubject;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.pojo.dto.AgingBucketDto;
import com.jerry.salesmanagement.pojo.dto.CounterpartyAgingRowDto;
import com.jerry.salesmanagement.pojo.dto.FinanceAnalyticsOverviewDto;
import com.jerry.salesmanagement.pojo.dto.ProjectProfitRowDto;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.FinanceAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.jerry.salesmanagement.common.FinanceVoucherMetrics.defaultZero;
import static com.jerry.salesmanagement.common.FinanceVoucherMetrics.scale2;

@Service
public class FinanceAnalyticsServiceImpl implements FinanceAnalyticsService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private FinanceVoucherMapper financeVoucherMapper;

    @Autowired
    private FinanceSubjectMapper financeSubjectMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private StageMapper stageMapper;

    @Autowired
    private ProjectStatusHistoryMapper projectStatusHistoryMapper;

    @Autowired
    private ContractBasicInfoMapper contractBasicInfoMapper;

    @Autowired(required = false)
    private ContractTypeMapper contractTypeMapper;

    @Autowired
    private CurrentUserService currentUserService;

    private Clock clock = Clock.systemDefaultZone();

    @Override
    public FinanceAnalyticsOverviewDto getOverview() {
        boolean canViewContracts = currentUserService.hasAnyAuthority("contract.access", "contract.manage");

        List<FinanceVoucher> vouchers = financeVoucherMapper.selectAll();
        Map<Long, String> level1CodeById = new HashMap<>();
        if (financeSubjectMapper != null) {
            financeSubjectMapper.selectAll().stream()
                    .filter(s -> s != null && Objects.equals(s.getSubjectLevel(), 1))
                    .forEach(s -> level1CodeById.put(s.getId(), s.getSubjectCode()));
        }

        Map<String, Customer> customerByUuid = new HashMap<>();
        if (customerMapper != null) {
            customerMapper.selectAll().stream()
                    .filter(c -> c != null && StringUtils.hasText(c.getUuid()))
                    .forEach(c -> customerByUuid.put(c.getUuid(), c));
        }

        Map<Long, String> stageNameById = new HashMap<>();
        if (stageMapper != null) {
            stageMapper.selectAll().stream()
                    .filter(s -> s != null && s.getId() != null)
                    .forEach(s -> stageNameById.put(s.getId(), s.getDescription()));
        }

        Map<String, String> currentStageNameByProjectId = resolveCurrentStageNames(stageNameById);
        List<Project> projects = projectMapper.selectAll();
        Map<String, Project> projectByUuid = new HashMap<>();
        projects.stream()
                .filter(p -> p != null && StringUtils.hasText(p.getUuid()))
                .forEach(p -> projectByUuid.put(p.getUuid(), p));

        Map<Long, ContractType> contractTypeById = indexContractTypes();
        Map<String, BigDecimal> contractSumByProjectId = new HashMap<>();
        BigDecimal totalContractAmount = BigDecimal.ZERO;
        if (canViewContracts && contractBasicInfoMapper != null) {
            for (ContractBasicInfo contract : contractBasicInfoMapper.selectAll()) {
                if (contract == null || !isSalesContract(contract, contractTypeById)) {
                    continue;
                }
                BigDecimal amount = defaultZero(contract.getContractAmount());
                totalContractAmount = totalContractAmount.add(amount);
                if (StringUtils.hasText(contract.getProjectBasicInfoId())) {
                    contractSumByProjectId.merge(contract.getProjectBasicInfoId(), amount, BigDecimal::add);
                }
            }
        }

        Map<String, ProjectAccumulator> projectAccMap = new LinkedHashMap<>();
        for (Project project : projects) {
            if (project != null && StringUtils.hasText(project.getUuid())) {
                projectAccMap.put(project.getUuid(), new ProjectAccumulator(project));
            }
        }

        BigDecimal totalActualIncome = BigDecimal.ZERO;
        BigDecimal totalActualExpense = BigDecimal.ZERO;
        BigDecimal totalBookedIncome = BigDecimal.ZERO;
        BigDecimal totalBookedExpense = BigDecimal.ZERO;
        BigDecimal nonProjectIncome = BigDecimal.ZERO;
        BigDecimal nonProjectExpense = BigDecimal.ZERO;
        BigDecimal nonProjectBookedExpense = BigDecimal.ZERO;

        BigDecimal totalReceivable = BigDecimal.ZERO;
        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal totalPendingInvoice = BigDecimal.ZERO;
        BigDecimal totalMissingInvoice = BigDecimal.ZERO;

        AgingBucketDto receivableAging = new AgingBucketDto();
        AgingBucketDto payableAging = new AgingBucketDto();
        Map<String, AgingAccumulator> receivableByParty = new LinkedHashMap<>();
        Map<String, AgingAccumulator> payableByParty = new LinkedHashMap<>();

        LocalDate today = LocalDate.now(clock);

        for (FinanceVoucher voucher : vouchers) {
            if (voucher == null) {
                continue;
            }
            String level1Code = level1CodeById.get(voucher.getLevel1SubjectId());
            boolean isNonProject = "NON_PROJECT".equals(level1Code) || !StringUtils.hasText(voucher.getProjectId());

            String direction = voucher.getTransactionDirection();
            boolean completed = Boolean.TRUE.equals(voucher.getIsCompleted());
            BigDecimal income = completed && "RECEIVE".equals(direction) ? defaultZero(voucher.getActualIncomeAmount()) : BigDecimal.ZERO;
            BigDecimal expense = completed && "PAY".equals(direction) ? defaultZero(voucher.getActualExpenseAmount()) : BigDecimal.ZERO;
            BigDecimal bookedIncome = completed && "RECEIVE".equals(direction) ? defaultZero(voucher.getBookedAmount()) : BigDecimal.ZERO;
            BigDecimal bookedExpense = completed && "PAY".equals(direction) ? defaultZero(voucher.getBookedAmount()) : BigDecimal.ZERO;

            totalActualIncome = totalActualIncome.add(income);
            totalActualExpense = totalActualExpense.add(expense);
            totalBookedIncome = totalBookedIncome.add(bookedIncome);
            totalBookedExpense = totalBookedExpense.add(bookedExpense);

            if (isNonProject) {
                nonProjectIncome = nonProjectIncome.add(income);
                nonProjectExpense = nonProjectExpense.add(expense);
                nonProjectBookedExpense = nonProjectBookedExpense.add(bookedExpense);
            }

            FinanceVoucherMetrics.GapInfo gap = FinanceVoucherMetrics.resolveGap(voucher);
            switch (gap.type()) {
                case RECEIVABLE -> totalReceivable = totalReceivable.add(gap.amount());
                case PAYABLE -> totalPayable = totalPayable.add(gap.amount());
                case PENDING_INVOICE -> totalPendingInvoice = totalPendingInvoice.add(gap.amount());
                case MISSING_INVOICE -> totalMissingInvoice = totalMissingInvoice.add(gap.amount());
                default -> {
                }
            }

            if (!isNonProject && projectByUuid.containsKey(voucher.getProjectId())) {
                ProjectAccumulator acc = projectAccMap.get(voucher.getProjectId());
                acc.voucherCount++;
                if (!completed) {
                    acc.uncompletedVoucherCount++;
                }
                acc.actualIncome = acc.actualIncome.add(income);
                acc.actualExpense = acc.actualExpense.add(expense);
                acc.bookedIncome = acc.bookedIncome.add(bookedIncome);
                acc.bookedExpense = acc.bookedExpense.add(bookedExpense);
                switch (gap.type()) {
                    case RECEIVABLE -> acc.receivable = acc.receivable.add(gap.amount());
                    case PAYABLE -> acc.payable = acc.payable.add(gap.amount());
                    case PENDING_INVOICE -> acc.pendingInvoice = acc.pendingInvoice.add(gap.amount());
                    case MISSING_INVOICE -> acc.missingInvoice = acc.missingInvoice.add(gap.amount());
                    default -> {
                    }
                }
            }

            if (gap.type() == FinanceVoucherMetrics.GapType.RECEIVABLE || gap.type() == FinanceVoucherMetrics.GapType.PAYABLE) {
                long agingDays = resolveAgingDays(voucher.getOccurredOn(), today);
                String customerId = StringUtils.hasText(voucher.getCounterpartyCustomerId()) ? voucher.getCounterpartyCustomerId().trim() : "";
                String customerName = resolveCurrentCounterpartyName(voucher, customerByUuid);
                String partyKey = StringUtils.hasText(customerId) ? "ID::" + customerId : "NAME::" + customerName;

                if (gap.type() == FinanceVoucherMetrics.GapType.RECEIVABLE) {
                    receivableAging.add(agingDays, gap.amount());
                    receivableByParty
                            .computeIfAbsent(partyKey, k -> new AgingAccumulator(customerId, customerName, "RECEIVE"))
                            .add(agingDays, gap.amount(), voucher.getOccurredOn());
                } else {
                    payableAging.add(agingDays, gap.amount());
                    payableByParty
                            .computeIfAbsent(partyKey, k -> new AgingAccumulator(customerId, customerName, "PAY"))
                            .add(agingDays, gap.amount(), voucher.getOccurredOn());
                }
            }
        }

        List<ProjectProfitRowDto> profitRows = new ArrayList<>();
        for (ProjectAccumulator acc : projectAccMap.values()) {
            Project project = acc.project;
            Customer customer = customerByUuid.get(project.getCustomerId());
            ProjectProfitRowDto row = new ProjectProfitRowDto();
            row.setProjectId(project.getUuid());
            row.setProjectNumber(project.getProjectNumber());
            row.setProjectName(project.getProjectName());
            row.setCustomerId(project.getCustomerId());
            row.setCustomerName(customer != null ? customer.getCustomerName() : null);
            row.setCurrentStageName(currentStageNameByProjectId.getOrDefault(project.getUuid(), "未开始"));

            if (canViewContracts) {
                row.setContractAmount(scale2(contractSumByProjectId.get(project.getUuid())));
            }
            BigDecimal actualIncome = scale2(acc.actualIncome);
            BigDecimal actualExpense = scale2(acc.actualExpense);
            BigDecimal bookedIncome = scale2(acc.bookedIncome);
            BigDecimal bookedExpense = scale2(acc.bookedExpense);
            BigDecimal grossProfit = scale2(actualIncome.subtract(actualExpense));

            row.setActualIncomeAmount(actualIncome);
            row.setActualExpenseAmount(actualExpense);
            row.setBookedIncomeAmount(bookedIncome);
            row.setBookedExpenseAmount(bookedExpense);
            row.setGrossProfit(grossProfit);
            row.setBookedNetProfit(scale2(bookedIncome.subtract(bookedExpense)));
            if (actualIncome.compareTo(BigDecimal.ZERO) > 0) {
                row.setGrossMarginRate(
                        grossProfit.multiply(HUNDRED).divide(actualIncome, 2, RoundingMode.HALF_UP)
                );
            }
            if (row.getContractAmount() != null && row.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
                row.setCollectionRate(
                        actualIncome.multiply(HUNDRED).divide(row.getContractAmount(), 2, RoundingMode.HALF_UP)
                );
            }

            row.setReceivableAmount(scale2(acc.receivable));
            row.setPayableAmount(scale2(acc.payable));
            row.setPendingInvoiceAmount(scale2(acc.pendingInvoice));
            row.setMissingInvoiceAmount(scale2(acc.missingInvoice));
            row.setVoucherCount(acc.voucherCount);
            row.setUncompletedVoucherCount(acc.uncompletedVoucherCount);
            profitRows.add(row);
        }

        profitRows.sort(Comparator
                .comparing((ProjectProfitRowDto r) -> r.getActualIncomeAmount() == null ? BigDecimal.ZERO : r.getActualIncomeAmount())
                .reversed()
                .thenComparing(r -> r.getProjectNumber() == null ? "" : r.getProjectNumber()));

        FinanceAnalyticsOverviewDto overview = new FinanceAnalyticsOverviewDto();
        overview.setCanViewContracts(canViewContracts);
        if (canViewContracts) {
            overview.setTotalContractAmount(scale2(totalContractAmount));
        }
        BigDecimal scaledIncome = scale2(totalActualIncome);
        BigDecimal scaledExpense = scale2(totalActualExpense);
        BigDecimal totalGrossProfit = scale2(scaledIncome.subtract(scaledExpense));

        overview.setTotalActualIncome(scaledIncome);
        overview.setTotalActualExpense(scaledExpense);
        overview.setTotalBookedIncome(scale2(totalBookedIncome));
        overview.setTotalBookedExpense(scale2(totalBookedExpense));
        overview.setTotalGrossProfit(totalGrossProfit);
        if (scaledIncome.compareTo(BigDecimal.ZERO) > 0) {
            overview.setOverallGrossMarginRate(
                    totalGrossProfit.multiply(HUNDRED).divide(scaledIncome, 2, RoundingMode.HALF_UP)
            );
        }
        overview.setNonProjectIncome(scale2(nonProjectIncome));
        overview.setNonProjectExpense(scale2(nonProjectExpense));
        overview.setNonProjectBookedExpense(scale2(nonProjectBookedExpense));
        overview.setTotalReceivable(scale2(totalReceivable));
        overview.setTotalPayable(scale2(totalPayable));
        overview.setTotalPendingInvoice(scale2(totalPendingInvoice));
        overview.setTotalMissingInvoice(scale2(totalMissingInvoice));
        overview.setReceivableAging(receivableAging);
        overview.setPayableAging(payableAging);
        overview.setProjectProfits(profitRows);
        overview.setReceivableByCounterparty(toSortedCounterpartyRows(receivableByParty));
        overview.setPayableByCounterparty(toSortedCounterpartyRows(payableByParty));
        return overview;
    }

    private List<CounterpartyAgingRowDto> toSortedCounterpartyRows(Map<String, AgingAccumulator> source) {
        return source.values().stream()
                .map(AgingAccumulator::toDto)
                .sorted(Comparator.comparing(CounterpartyAgingRowDto::getTotal).reversed())
                .toList();
    }

    private Map<String, String> resolveCurrentStageNames(Map<Long, String> stageNameById) {
        Map<String, String> stageByProjectId = new HashMap<>();
        if (projectStatusHistoryMapper == null) {
            return stageByProjectId;
        }
        Map<String, List<ProjectStatusHistory>> grouped = new HashMap<>();
        for (ProjectStatusHistory history : projectStatusHistoryMapper.selectAll()) {
            if (history != null && StringUtils.hasText(history.getProjectId())) {
                grouped.computeIfAbsent(history.getProjectId(), k -> new ArrayList<>()).add(history);
            }
        }
        Comparator<ProjectStatusHistory> chronological = Comparator
                .comparing(ProjectStatusHistory::getCreatedAt, Comparator.nullsFirst(Date::compareTo))
                .thenComparing(h -> h.getId() == null ? Long.MAX_VALUE : h.getId());
        grouped.forEach((projectId, histories) -> {
            histories.sort(chronological);
            ProjectStatusHistory latest = histories.get(histories.size() - 1);
            String stageName = stageNameById.get(latest.getStageId());
            if (!StringUtils.hasText(stageName)) {
                stageName = latest.getStageName();
            }
            if (StringUtils.hasText(stageName)) {
                stageByProjectId.put(projectId, stageName);
            }
        });
        return stageByProjectId;
    }

    private Map<Long, ContractType> indexContractTypes() {
        Map<Long, ContractType> map = new HashMap<>();
        if (contractTypeMapper == null) {
            return map;
        }
        for (ContractType type : contractTypeMapper.selectAll()) {
            if (type != null && type.getId() != null) {
                map.put(type.getId(), type);
            }
        }
        return map;
    }

    private boolean isSalesContract(ContractBasicInfo contract, Map<Long, ContractType> contractTypeById) {
        if (contractTypeById == null || contractTypeById.isEmpty() || contract.getContractTypeId() == null) {
            return true;
        }
        ContractType type = contractTypeById.get(contract.getContractTypeId());
        if (type == null) {
            return true;
        }
        String code = type.getTypeCode() == null ? "" : type.getTypeCode().trim().toUpperCase();
        if ("XS".equals(code) || "XL".equals(code)) {
            return true;
        }
        if ("C".equals(code) || "LW".equals(code) || "CL".equals(code)) {
            return false;
        }
        String name = type.getTypeName() == null ? "" : type.getTypeName();
        return name.contains("销售") && !name.contains("采购") && !name.contains("劳务");
    }

    private String resolveCurrentCounterpartyName(FinanceVoucher voucher, Map<String, Customer> customerByUuid) {
        Customer customer = StringUtils.hasText(voucher.getCounterpartyCustomerId())
                ? customerByUuid.get(voucher.getCounterpartyCustomerId().trim())
                : null;
        if (customer != null && StringUtils.hasText(customer.getCustomerName())) {
            return customer.getCustomerName().trim();
        }
        if (StringUtils.hasText(voucher.getCounterpartyNameSnapshot())) {
            return voucher.getCounterpartyNameSnapshot().trim();
        }
        return "未指定往来方";
    }

    private long resolveAgingDays(LocalDate occurredOn, LocalDate today) {
        if (occurredOn == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.DAYS.between(occurredOn, today));
    }

    private static class ProjectAccumulator {
        private final Project project;
        private BigDecimal actualIncome = BigDecimal.ZERO;
        private BigDecimal actualExpense = BigDecimal.ZERO;
        private BigDecimal bookedIncome = BigDecimal.ZERO;
        private BigDecimal bookedExpense = BigDecimal.ZERO;
        private BigDecimal receivable = BigDecimal.ZERO;
        private BigDecimal payable = BigDecimal.ZERO;
        private BigDecimal pendingInvoice = BigDecimal.ZERO;
        private BigDecimal missingInvoice = BigDecimal.ZERO;
        private int voucherCount;
        private int uncompletedVoucherCount;

        private ProjectAccumulator(Project project) {
            this.project = project;
        }
    }

    private static class AgingAccumulator {
        private final String customerId;
        private final String customerName;
        private final String direction;
        private final AgingBucketDto buckets = new AgingBucketDto();
        private int voucherCount;
        private LocalDate oldestOccurredOn;
        private long maxAgingDays;

        private AgingAccumulator(String customerId, String customerName, String direction) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.direction = direction;
        }

        private void add(long agingDays, BigDecimal amount, LocalDate occurredOn) {
            buckets.add(agingDays, amount);
            voucherCount++;
            if (agingDays > maxAgingDays) {
                maxAgingDays = agingDays;
            }
            if (occurredOn != null && (oldestOccurredOn == null || occurredOn.isBefore(oldestOccurredOn))) {
                oldestOccurredOn = occurredOn;
            }
        }

        private CounterpartyAgingRowDto toDto() {
            CounterpartyAgingRowDto dto = new CounterpartyAgingRowDto();
            dto.setCustomerId(customerId);
            dto.setCustomerName(customerName);
            dto.setDirection(direction);
            dto.setBucket0To30(buckets.getBucket0To30());
            dto.setBucket31To60(buckets.getBucket31To60());
            dto.setBucket61To90(buckets.getBucket61To90());
            dto.setBucketOver90(buckets.getBucketOver90());
            dto.setTotal(buckets.getTotal());
            dto.setVoucherCount(voucherCount);
            dto.setOldestOccurredOn(oldestOccurredOn != null ? oldestOccurredOn.toString() : null);
            dto.setMaxAgingDays(maxAgingDays);
            return dto;
        }
    }
}
