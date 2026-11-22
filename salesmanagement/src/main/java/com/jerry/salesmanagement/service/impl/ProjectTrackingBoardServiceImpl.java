package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.FinanceVoucherMetrics;
import com.jerry.salesmanagement.common.WarehouseDocumentRules;
import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.ContractTypeMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.FinanceSubjectMapper;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.ProjectTypeMapper;
import com.jerry.salesmanagement.mapper.RegionMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.mapper.SysUserMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import com.jerry.salesmanagement.pojo.ContractType;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.FinanceSubject;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import com.jerry.salesmanagement.pojo.ProjectType;
import com.jerry.salesmanagement.pojo.Region;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingBoardDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingCardDto;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.ProjectTrackingBoardService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.jerry.salesmanagement.common.FinanceVoucherMetrics.defaultZero;
import static com.jerry.salesmanagement.common.FinanceVoucherMetrics.scale2;

@Service
public class ProjectTrackingBoardServiceImpl implements ProjectTrackingBoardService {

    private static final String STALLED_DAYS_CONFIG_KEY = "project.tracking.stalled_days";
    private static final int DEFAULT_STALLED_DAYS = 30;
    private static final long WARRANTY_EXPIRING_DAYS = 30L;
    private static final String DELETE_LOCK_STAGE_KEY = "project.delete.lock_stage_sort_order";
    private static final int DEFAULT_DELETE_LOCK_STAGE = 5;
    private static final String LOW_COLLECTION_STAGE_KEY = "project.tracking.low_collection_stage_sort_order";
    private static final int DEFAULT_LOW_COLLECTION_STAGE = 9;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal LOW_COLLECTION_THRESHOLD = new BigDecimal("30");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private StageMapper stageMapper;

    @Autowired
    private ProjectStatusHistoryMapper projectStatusHistoryMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private ProjectTypeMapper projectTypeMapper;

    @Autowired
    private ProjectListMapper projectListMapper;

    @Autowired
    private ContractBasicInfoMapper contractBasicInfoMapper;

    @Autowired(required = false)
    private ContractTypeMapper contractTypeMapper;

    @Autowired
    private FinanceVoucherMapper financeVoucherMapper;

    @Autowired(required = false)
    private FinanceSubjectMapper financeSubjectMapper;

    @Autowired
    private WarehouseDocumentMapper warehouseDocumentMapper;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private SystemConfigService systemConfigService;

    private Clock clock = Clock.systemDefaultZone();

    @Override
    public ProjectTrackingBoardDto getTrackingBoard(
            String keyword,
            Long managerId,
            Long projectTypeId,
            String riskTag
    ) {
        ProjectTrackingBoardDto.Permissions permissions = resolvePermissions();
        int stalledDaysThreshold = systemConfigService != null
                ? Math.max(1, systemConfigService.getInt(STALLED_DAYS_CONFIG_KEY, DEFAULT_STALLED_DAYS))
                : DEFAULT_STALLED_DAYS;
        int deleteLockSortOrder = systemConfigService != null
                ? systemConfigService.getInt(DELETE_LOCK_STAGE_KEY, DEFAULT_DELETE_LOCK_STAGE)
                : DEFAULT_DELETE_LOCK_STAGE;
        int lowCollectionStageSortOrder = systemConfigService != null
                ? systemConfigService.getInt(LOW_COLLECTION_STAGE_KEY, DEFAULT_LOW_COLLECTION_STAGE)
                : DEFAULT_LOW_COLLECTION_STAGE;

        List<Stage> activeStages = stageMapper.selectActive().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((Stage s) -> s.getSortOrder() == null ? Integer.MAX_VALUE : s.getSortOrder())
                        .thenComparing(s -> s.getStageCode() == null ? "" : s.getStageCode())
                        .thenComparing(s -> s.getId() == null ? Long.MAX_VALUE : s.getId()))
                .toList();

        Map<Long, Stage> stageById = new HashMap<>();
        stageMapper.selectAll().stream()
                .filter(Objects::nonNull)
                .forEach(s -> stageById.put(s.getId(), s));

        Map<Long, Integer> activeStageIndexById = new HashMap<>();
        int maxActiveStageSortOrder = 0;
        for (int i = 0; i < activeStages.size(); i++) {
            Stage stage = activeStages.get(i);
            activeStageIndexById.put(stage.getId(), i);
            if (stage.getSortOrder() != null && stage.getSortOrder() > maxActiveStageSortOrder) {
                maxActiveStageSortOrder = stage.getSortOrder();
            }
        }

        List<Project> projects = projectMapper.selectAll();
        Map<String, Customer> customerByUuid = indexCustomers();
        Map<Long, SysUser> userById = indexUsers();
        Map<Integer, Region> regionById = indexRegions();
        Map<Long, ProjectType> projectTypeById = indexProjectTypes();

        Map<String, List<ProjectStatusHistory>> historyByProjectId = groupHistoryByProject(
                projectStatusHistoryMapper.selectAll()
        );
        Map<String, List<ProjectList>> listsByProjectId = groupListsByProject(
                projectListMapper.selectAll()
        );
        Map<Long, ContractType> contractTypeById = indexContractTypes();
        Map<String, List<ContractBasicInfo>> contractsByProjectId = permissions.isCanViewContracts()
                ? groupContractsByProject(contractBasicInfoMapper.selectAll(), contractTypeById)
                : Map.of();
        Map<Long, String> level1SubjectCodeById = indexLevel1SubjectCodes();
        Map<String, List<FinanceVoucher>> vouchersByProjectId = permissions.isCanViewFinance()
                ? groupVouchersByProject(financeVoucherMapper.selectAll(), level1SubjectCodeById)
                : Map.of();
        Map<String, List<WarehouseDocument>> warehouseDocsByProjectId = permissions.isCanViewInventory()
                ? groupWarehouseDocsByProject(warehouseDocumentMapper.selectAll())
                : Map.of();

        LocalDate today = LocalDate.now(clock);
        ZoneId zoneId = clock.getZone();
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        String normalizedRiskTag = StringUtils.hasText(riskTag) ? riskTag.trim().toUpperCase(Locale.ROOT) : "";

        List<ProjectTrackingCardDto> cards = new ArrayList<>();
        for (Project project : projects) {
            if (project == null || !StringUtils.hasText(project.getUuid())) {
                continue;
            }
            ProjectTrackingCardDto card = buildCard(
                    project,
                    customerByUuid.get(project.getCustomerId()),
                    userById.get(project.getManagerId()),
                    regionById.get(project.getRegionId()),
                    projectTypeById.get(project.getProjectTypeId()),
                    historyByProjectId.getOrDefault(project.getUuid(), List.of()),
                    activeStages,
                    stageById,
                    activeStageIndexById,
                    maxActiveStageSortOrder,
                    deleteLockSortOrder,
                    lowCollectionStageSortOrder,
                    listsByProjectId.getOrDefault(project.getUuid(), List.of()),
                    contractsByProjectId.getOrDefault(project.getUuid(), List.of()),
                    vouchersByProjectId.getOrDefault(project.getUuid(), List.of()),
                    warehouseDocsByProjectId.getOrDefault(project.getUuid(), List.of()),
                    permissions,
                    stalledDaysThreshold,
                    today,
                    zoneId
            );

            if (matchesFilter(card, normalizedKeyword, managerId, projectTypeId, normalizedRiskTag)) {
                cards.add(card);
            }
        }

        cards.sort(Comparator
                .comparing((ProjectTrackingCardDto c) -> c.getCurrentStageSortOrder() == null ? -1 : c.getCurrentStageSortOrder())
                .thenComparing(c -> c.getProjectNumber() == null ? "" : c.getProjectNumber()));

        ProjectTrackingBoardDto board = new ProjectTrackingBoardDto();
        board.setStages(activeStages);
        board.setStalledDaysThreshold(stalledDaysThreshold);
        board.setPermissions(permissions);
        board.setItems(cards);
        board.setSummary(buildSummary(cards, permissions));
        return board;
    }

    private ProjectTrackingCardDto buildCard(
            Project project,
            Customer customer,
            SysUser manager,
            Region region,
            ProjectType projectType,
            List<ProjectStatusHistory> histories,
            List<Stage> activeStages,
            Map<Long, Stage> stageById,
            Map<Long, Integer> activeStageIndexById,
            int maxActiveStageSortOrder,
            int deleteLockSortOrder,
            int lowCollectionStageSortOrder,
            List<ProjectList> projectLists,
            List<ContractBasicInfo> contracts,
            List<FinanceVoucher> vouchers,
            List<WarehouseDocument> warehouseDocs,
            ProjectTrackingBoardDto.Permissions permissions,
            int stalledDaysThreshold,
            LocalDate today,
            ZoneId zoneId
    ) {
        ProjectTrackingCardDto card = new ProjectTrackingCardDto();
        card.setUuid(project.getUuid());
        card.setProjectNumber(project.getProjectNumber());
        card.setProjectName(project.getProjectName());
        card.setCustomerId(project.getCustomerId());
        card.setCustomerName(customer != null ? customer.getCustomerName() : null);
        card.setManagerId(project.getManagerId());
        card.setManagerName(resolveUserDisplayName(manager));
        card.setRegionId(project.getRegionId());
        card.setRegionName(region != null ? (StringUtils.hasText(region.getMergerName()) ? region.getMergerName() : region.getName()) : null);
        card.setProjectTypeId(project.getProjectTypeId());
        card.setProjectTypeName(projectType != null ? projectType.getName() : null);
        card.setPlanStartTime(project.getPlanStartTime());
        card.setPlanEndTime(project.getPlanEndTime());
        card.setWarrantyUntil(project.getWarrantyUntil());

        ProjectStatusHistory latestHistory = histories.isEmpty() ? null : histories.get(histories.size() - 1);
        Stage currentStage = latestHistory != null ? stageById.get(latestHistory.getStageId()) : null;
        int maxRecordedSortOrder = histories.stream()
                .map(h -> stageById.get(h.getStageId()))
                .filter(Objects::nonNull)
                .map(Stage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1);
        card.setCanDelete(maxRecordedSortOrder < 0 || maxRecordedSortOrder < deleteLockSortOrder);

        if (currentStage != null) {
            card.setCurrentStageId(currentStage.getId());
            card.setCurrentStageCode(currentStage.getStageCode());
            card.setCurrentStageName(currentStage.getDescription());
            card.setCurrentStageSortOrder(currentStage.getSortOrder());
        } else {
            card.setCurrentStageName("未开始");
        }

        if (latestHistory != null && latestHistory.getCreatedAt() != null) {
            card.setStageEnteredAt(latestHistory.getCreatedAt());
            LocalDate enteredDate = toLocalDate(latestHistory.getCreatedAt(), zoneId);
            if (enteredDate != null) {
                card.setStageStayDays(Math.max(0L, ChronoUnit.DAYS.between(enteredDate, today)));
            }
        } else if (project.getCreateTime() != null) {
            LocalDate createdDate = toLocalDate(project.getCreateTime(), zoneId);
            if (createdDate != null) {
                card.setStageStayDays(Math.max(0L, ChronoUnit.DAYS.between(createdDate, today)));
            }
        }

        Integer currentActiveIdx = currentStage != null ? activeStageIndexById.get(currentStage.getId()) : null;
        Stage nextStage = null;
        if (currentStage == null) {
            nextStage = activeStages.isEmpty() ? null : activeStages.get(0);
        } else if (currentActiveIdx != null && currentActiveIdx + 1 < activeStages.size()) {
            nextStage = activeStages.get(currentActiveIdx + 1);
        } else {
            int curSort = currentStage.getSortOrder() == null ? 0 : currentStage.getSortOrder();
            nextStage = activeStages.stream()
                    .filter(s -> s.getSortOrder() != null && s.getSortOrder() > curSort)
                    .findFirst()
                    .orElse(null);
        }
        if (nextStage != null) {
            card.setNextStageId(nextStage.getId());
            card.setNextStageCode(nextStage.getStageCode());
            card.setNextStageName(nextStage.getDescription());
        }

        boolean completed = currentStage != null
                && nextStage == null
                && maxActiveStageSortOrder > 0
                && currentStage.getSortOrder() != null
                && currentStage.getSortOrder() >= maxActiveStageSortOrder;
        card.setCompleted(completed);

        LocalDate planEndDate = toLocalDate(project.getPlanEndTime(), zoneId);
        if (planEndDate != null) {
            long daysToPlanEnd = ChronoUnit.DAYS.between(today, planEndDate);
            card.setDaysToPlanEnd(daysToPlanEnd);
            card.setOverdue(!completed && daysToPlanEnd < 0);
        } else {
            card.setOverdue(false);
        }

        LocalDate warrantyEndDate = toLocalDate(project.getWarrantyUntil(), zoneId);
        if (warrantyEndDate != null) {
            long daysToWarranty = ChronoUnit.DAYS.between(today, warrantyEndDate);
            card.setDaysToWarrantyEnd(daysToWarranty);
            if (daysToWarranty < 0) {
                card.setWarrantyStatus("EXPIRED");
            } else if (daysToWarranty <= WARRANTY_EXPIRING_DAYS) {
                card.setWarrantyStatus("EXPIRING_SOON");
            } else {
                card.setWarrantyStatus("ACTIVE");
            }
        } else {
            card.setWarrantyStatus("NONE");
        }

        int pendingLists = (int) projectLists.stream().filter(item -> item.getAuditorUser() == null).count();
        card.setListCount(projectLists.size());
        card.setPendingListCount(pendingLists);

        if (permissions.isCanViewContracts()) {
            card.setContractCount(contracts.size());
            BigDecimal contractTotal = contracts.stream()
                    .map(ContractBasicInfo::getContractAmount)
                    .map(FinanceVoucherMetrics::defaultZero)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            card.setContractAmount(scale2(contractTotal));
        }

        if (permissions.isCanViewFinance()) {
            BigDecimal received = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            BigDecimal bookedExpense = BigDecimal.ZERO;
            BigDecimal receivable = BigDecimal.ZERO;
            BigDecimal payable = BigDecimal.ZERO;
            int pendingVouchers = 0;

            for (FinanceVoucher voucher : vouchers) {
                if (voucher.getAuditorUser() == null) {
                    pendingVouchers++;
                }
                String dir = voucher.getTransactionDirection();
                if (Boolean.TRUE.equals(voucher.getIsCompleted())) {
                    if ("RECEIVE".equals(dir)) {
                        received = received.add(defaultZero(voucher.getActualIncomeAmount()));
                    } else if ("PAY".equals(dir)) {
                        expense = expense.add(defaultZero(voucher.getActualExpenseAmount()));
                        bookedExpense = bookedExpense.add(defaultZero(voucher.getBookedAmount()));
                    }
                }
                FinanceVoucherMetrics.GapInfo gap = FinanceVoucherMetrics.resolveGap(voucher);
                if (gap.type() == FinanceVoucherMetrics.GapType.RECEIVABLE) {
                    receivable = receivable.add(gap.amount());
                } else if (gap.type() == FinanceVoucherMetrics.GapType.PAYABLE) {
                    payable = payable.add(gap.amount());
                }
            }

            BigDecimal scaledReceived = scale2(received);
            BigDecimal scaledExpense = scale2(expense);
            BigDecimal grossProfit = scale2(scaledReceived.subtract(scaledExpense));
            card.setReceivedAmount(scaledReceived);
            card.setExpenseAmount(scaledExpense);
            card.setBookedExpenseAmount(scale2(bookedExpense));
            card.setReceivableAmount(scale2(receivable));
            card.setPayableAmount(scale2(payable));
            card.setPendingFinanceVoucherCount(pendingVouchers);
            card.setEstimatedGrossProfit(grossProfit);
            if (scaledReceived.compareTo(BigDecimal.ZERO) > 0) {
                card.setEstimatedGrossMargin(
                        grossProfit.multiply(HUNDRED).divide(scaledReceived, 2, RoundingMode.HALF_UP)
                );
            }
            if (card.getContractAmount() != null && card.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
                card.setCollectionRate(
                        scaledReceived.multiply(HUNDRED).divide(card.getContractAmount(), 2, RoundingMode.HALF_UP)
                );
            }
        }

        if (permissions.isCanViewInventory()) {
            BigDecimal outboundTotal = BigDecimal.ZERO;
            int pendingDocs = 0;
            for (WarehouseDocument doc : warehouseDocs) {
                if (WarehouseDocumentRules.TRANSFER_SOURCE.equals(doc.getSourceRefType())) {
                    continue;
                }
                if (doc.getAuditorUser() == null) {
                    pendingDocs++;
                }
                if ("OUTBOUND_NOTE".equals(doc.getDocType())) {
                    outboundTotal = outboundTotal.add(defaultZero(doc.getTotalAmount()));
                }
            }
            card.setOutboundAmount(scale2(outboundTotal));
            card.setPendingWarehouseDocCount(pendingDocs);
        }

        int totalPending = pendingLists
                + (card.getPendingWarehouseDocCount() != null ? card.getPendingWarehouseDocCount() : 0)
                + (card.getPendingFinanceVoucherCount() != null ? card.getPendingFinanceVoucherCount() : 0);
        card.setTotalPendingAuditCount(totalPending);

        List<String> riskTags = new ArrayList<>();
        if (card.isOverdue()) {
            riskTags.add("OVERDUE");
        }
        if (!completed && card.getStageStayDays() != null && card.getStageStayDays() >= stalledDaysThreshold) {
            riskTags.add("STALLED");
        }
        if ("EXPIRING_SOON".equals(card.getWarrantyStatus())) {
            riskTags.add("WARRANTY_EXPIRING");
        } else if ("EXPIRED".equals(card.getWarrantyStatus())) {
            riskTags.add("WARRANTY_EXPIRED");
        }
        if (permissions.isCanViewContracts()
                && permissions.isCanViewFinance()
                && card.getCollectionRate() != null
                && card.getCurrentStageSortOrder() != null
                && card.getCurrentStageSortOrder() >= lowCollectionStageSortOrder
                && card.getCollectionRate().compareTo(LOW_COLLECTION_THRESHOLD) < 0) {
            riskTags.add("LOW_COLLECTION");
        }
        if (permissions.isCanViewFinance()
                && card.getReceivedAmount() != null
                && card.getReceivedAmount().compareTo(BigDecimal.ZERO) > 0
                && card.getEstimatedGrossProfit() != null
                && card.getEstimatedGrossProfit().compareTo(BigDecimal.ZERO) < 0) {
            riskTags.add("NEGATIVE_MARGIN");
        }
        if (totalPending > 0) {
            riskTags.add("PENDING_AUDIT");
        }
        card.setRiskTags(riskTags);
        return card;
    }

    private ProjectTrackingBoardDto.Summary buildSummary(
            List<ProjectTrackingCardDto> cards,
            ProjectTrackingBoardDto.Permissions permissions
    ) {
        ProjectTrackingBoardDto.Summary summary = new ProjectTrackingBoardDto.Summary();
        summary.setTotalProjects(cards.size());

        BigDecimal totalContract = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalReceivable = BigDecimal.ZERO;

        for (ProjectTrackingCardDto card : cards) {
            if (card.isCompleted()) {
                summary.setCompletedProjects(summary.getCompletedProjects() + 1);
            } else {
                summary.setActiveProjects(summary.getActiveProjects() + 1);
            }
            if (card.isOverdue()) {
                summary.setOverdueCount(summary.getOverdueCount() + 1);
            }
            if (card.getRiskTags().contains("STALLED")) {
                summary.setStalledCount(summary.getStalledCount() + 1);
            }
            if ("EXPIRING_SOON".equals(card.getWarrantyStatus())) {
                summary.setWarrantyExpiringCount(summary.getWarrantyExpiringCount() + 1);
            } else if ("EXPIRED".equals(card.getWarrantyStatus())) {
                summary.setWarrantyExpiredCount(summary.getWarrantyExpiredCount() + 1);
            }
            if (card.getTotalPendingAuditCount() != null && card.getTotalPendingAuditCount() > 0) {
                summary.setPendingAuditProjectCount(summary.getPendingAuditProjectCount() + 1);
            }
            if (permissions.isCanViewContracts()) {
                totalContract = totalContract.add(defaultZero(card.getContractAmount()));
            }
            if (permissions.isCanViewFinance()) {
                totalReceived = totalReceived.add(defaultZero(card.getReceivedAmount()));
                totalReceivable = totalReceivable.add(defaultZero(card.getReceivableAmount()));
            }
        }

        if (permissions.isCanViewContracts()) {
            summary.setTotalContractAmount(scale2(totalContract));
        }
        if (permissions.isCanViewFinance()) {
            summary.setTotalReceivedAmount(scale2(totalReceived));
            summary.setTotalReceivableAmount(scale2(totalReceivable));
        }
        return summary;
    }

    private ProjectTrackingBoardDto.Permissions resolvePermissions() {
        ProjectTrackingBoardDto.Permissions permissions = new ProjectTrackingBoardDto.Permissions();
        permissions.setCanViewContracts(currentUserService.hasAnyAuthority("contract.access", "contract.manage"));
        permissions.setCanViewFinance(currentUserService.hasAnyAuthority("finance.access", "finance.manage", "finance.voucher.audit"));
        permissions.setCanViewInventory(currentUserService.hasAnyAuthority("inventory.access", "inventory.manage", "inventory.warehouse-doc.audit"));
        permissions.setCanManageProject(currentUserService.hasAnyAuthority("project.manage"));
        permissions.setCanAuditProjectLists(currentUserService.hasAnyAuthority("project.list.audit", "project.manage"));
        permissions.setCanAuditWarehouseDocs(currentUserService.hasAnyAuthority("inventory.warehouse-doc.audit", "inventory.manage"));
        permissions.setCanAuditFinanceVouchers(currentUserService.hasAnyAuthority("finance.voucher.audit", "finance.manage"));
        return permissions;
    }

    private boolean matchesFilter(
            ProjectTrackingCardDto card,
            String keyword,
            Long managerId,
            Long projectTypeId,
            String riskTag
    ) {
        if (managerId != null && !Objects.equals(card.getManagerId(), managerId)) {
            return false;
        }
        if (projectTypeId != null && !Objects.equals(card.getProjectTypeId(), projectTypeId)) {
            return false;
        }
        if (StringUtils.hasText(riskTag) && !card.getRiskTags().contains(riskTag)) {
            return false;
        }
        if (StringUtils.hasText(keyword)) {
            boolean matchesKeyword = containsIgnoreCase(card.getProjectNumber(), keyword)
                    || containsIgnoreCase(card.getProjectName(), keyword)
                    || containsIgnoreCase(card.getCustomerName(), keyword)
                    || containsIgnoreCase(card.getManagerName(), keyword)
                    || containsIgnoreCase(card.getCurrentStageName(), keyword);
            if (!matchesKeyword) {
                return false;
            }
        }
        return true;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Map<String, Customer> indexCustomers() {
        Map<String, Customer> map = new HashMap<>();
        if (customerMapper == null) {
            return map;
        }
        customerMapper.selectAll().stream()
                .filter(c -> c != null && StringUtils.hasText(c.getUuid()))
                .forEach(c -> map.put(c.getUuid(), c));
        return map;
    }

    private Map<Long, SysUser> indexUsers() {
        Map<Long, SysUser> map = new HashMap<>();
        if (sysUserMapper == null) {
            return map;
        }
        sysUserMapper.selectAll().stream()
                .filter(u -> u != null && u.getId() != null)
                .forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private Map<Integer, Region> indexRegions() {
        Map<Integer, Region> map = new HashMap<>();
        if (regionMapper == null) {
            return map;
        }
        regionMapper.selectAll().stream()
                .filter(r -> r != null && r.getId() != null)
                .forEach(r -> map.put(r.getId(), r));
        return map;
    }

    private Map<Long, ProjectType> indexProjectTypes() {
        Map<Long, ProjectType> map = new HashMap<>();
        if (projectTypeMapper == null) {
            return map;
        }
        projectTypeMapper.selectAll().stream()
                .filter(t -> t != null && t.getId() != null)
                .forEach(t -> map.put(t.getId(), t));
        return map;
    }

    private Map<String, List<ProjectStatusHistory>> groupHistoryByProject(List<ProjectStatusHistory> all) {
        Map<String, List<ProjectStatusHistory>> map = new HashMap<>();
        if (all == null) {
            return map;
        }
        for (ProjectStatusHistory history : all) {
            if (history != null && StringUtils.hasText(history.getProjectId())) {
                map.computeIfAbsent(history.getProjectId(), key -> new ArrayList<>()).add(history);
            }
        }
        Comparator<ProjectStatusHistory> chronological = Comparator
                .comparing(ProjectStatusHistory::getCreatedAt, Comparator.nullsFirst(Date::compareTo))
                .thenComparing(h -> h.getId() == null ? Long.MAX_VALUE : h.getId());
        map.values().forEach(list -> list.sort(chronological));
        return map;
    }

    private Map<String, List<ProjectList>> groupListsByProject(List<ProjectList> all) {
        Map<String, List<ProjectList>> map = new HashMap<>();
        if (all == null) {
            return map;
        }
        for (ProjectList list : all) {
            if (list != null && StringUtils.hasText(list.getProjectId())) {
                map.computeIfAbsent(list.getProjectId(), key -> new ArrayList<>()).add(list);
            }
        }
        return map;
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
        String code = type.getTypeCode() == null ? "" : type.getTypeCode().trim().toUpperCase(Locale.ROOT);
        if ("XS".equals(code) || "XL".equals(code)) {
            return true;
        }
        if ("C".equals(code) || "LW".equals(code) || "CL".equals(code)) {
            return false;
        }
        String name = type.getTypeName() == null ? "" : type.getTypeName();
        return name.contains("销售") && !name.contains("采购") && !name.contains("劳务");
    }

    private Map<Long, String> indexLevel1SubjectCodes() {
        Map<Long, String> map = new HashMap<>();
        if (financeSubjectMapper == null) {
            return map;
        }
        for (FinanceSubject subject : financeSubjectMapper.selectAll()) {
            if (subject != null && subject.getId() != null && Objects.equals(subject.getSubjectLevel(), 1)) {
                map.put(subject.getId(), subject.getSubjectCode());
            }
        }
        return map;
    }

    private Map<String, List<ContractBasicInfo>> groupContractsByProject(
            List<ContractBasicInfo> all,
            Map<Long, ContractType> contractTypeById
    ) {
        Map<String, List<ContractBasicInfo>> map = new HashMap<>();
        if (all == null) {
            return map;
        }
        for (ContractBasicInfo contract : all) {
            if (contract != null
                    && StringUtils.hasText(contract.getProjectBasicInfoId())
                    && isSalesContract(contract, contractTypeById)) {
                map.computeIfAbsent(contract.getProjectBasicInfoId(), key -> new ArrayList<>()).add(contract);
            }
        }
        return map;
    }

    private Map<String, List<FinanceVoucher>> groupVouchersByProject(
            List<FinanceVoucher> all,
            Map<Long, String> level1SubjectCodeById
    ) {
        Map<String, List<FinanceVoucher>> map = new HashMap<>();
        if (all == null) {
            return map;
        }
        for (FinanceVoucher voucher : all) {
            if (voucher != null && StringUtils.hasText(voucher.getProjectId())) {
                String level1Code = level1SubjectCodeById.get(voucher.getLevel1SubjectId());
                if ("NON_PROJECT".equals(level1Code)) {
                    continue;
                }
                map.computeIfAbsent(voucher.getProjectId(), key -> new ArrayList<>()).add(voucher);
            }
        }
        return map;
    }

    private Map<String, List<WarehouseDocument>> groupWarehouseDocsByProject(List<WarehouseDocument> all) {
        Map<String, List<WarehouseDocument>> map = new HashMap<>();
        if (all == null) {
            return map;
        }
        for (WarehouseDocument doc : all) {
            if (doc != null && StringUtils.hasText(doc.getProjectId())) {
                map.computeIfAbsent(doc.getProjectId(), key -> new ArrayList<>()).add(doc);
            }
        }
        return map;
    }

    private String resolveUserDisplayName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private LocalDate toLocalDate(Date date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(zoneId).toLocalDate();
    }
}
