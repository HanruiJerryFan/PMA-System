package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.ProjectListItem;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.dto.ProjectListAggregateItem;
import com.jerry.salesmanagement.pojo.dto.ProjectListAggregateSource;
import com.jerry.salesmanagement.pojo.dto.ProjectListAggregateView;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.EntryAuditService;
import com.jerry.salesmanagement.service.ProjectListItemService;
import com.jerry.salesmanagement.service.ProjectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectListServiceImpl implements ProjectListService {

    private static final Set<String> LIST_TYPES = Set.of("INITIAL_SALES", "PROCUREMENT", "CHANGE");
    private static final Set<String> FINAL_LIST_TYPES = Set.of("FINAL_SALES", "FINAL_PROCUREMENT");

    @Autowired
    private ProjectListMapper mapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectListItemService projectListItemService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private EntryAuditService entryAuditService;

    @Override
    public ProjectList getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<ProjectList> getAll() {
        return mapper.selectAll();
    }

    @Override
    public List<ProjectList> getByProjectId(String projectId) {
        return mapper.selectByProjectId(projectId);
    }

    @Override
    public ProjectListAggregateView getAggregateView(String projectId, String aggregateType) {
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalArgumentException("Project is required");
        }
        Project project = projectMapper.selectByUuid(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project does not exist");
        }

        String normalizedAggregateType = normalizeAggregateType(aggregateType);
        List<ProjectList> allLists = mapper.selectByProjectId(projectId);
        List<ProjectList> sourceLists = allLists.stream()
                .filter(item -> matchesAggregateType(item.getListType(), normalizedAggregateType))
                .collect(Collectors.toList());

        ProjectListAggregateView view = new ProjectListAggregateView();
        view.setProjectId(projectId);
        view.setAggregateType(normalizedAggregateType);
        view.setListName("FINAL_SALES".equals(normalizedAggregateType) ? "最终销售清单" : "最终采购清单");
        view.setCustomerName(resolveCustomerName(project));
        view.setSourceListCount(sourceLists.size());

        for (ProjectList list : sourceLists) {
            ProjectListAggregateSource source = new ProjectListAggregateSource();
            source.setUuid(list.getUuid());
            source.setListName(list.getListName());
            source.setListType(list.getListType());
            source.setEntryDate(list.getEntryDate());
            source.setEntryUser(list.getEntryUser());
            source.setEntryUserName(list.getEntryUserName());
            source.setAuditorUser(list.getAuditorUser());
            source.setAuditorUserName(list.getAuditorUserName());
            view.getSourceLists().add(source);
        }

        Map<String, ProjectListAggregateItem> aggregateMap = new LinkedHashMap<>();
        for (ProjectList list : sourceLists) {
            List<ProjectListItem> items = projectListItemService.getByProjectListId(list.getUuid());
            for (ProjectListItem item : items) {
                String aggregateKey = buildAggregateKey(item);
                ProjectListAggregateItem aggregateItem = aggregateMap.computeIfAbsent(aggregateKey, key -> createAggregateItem(item));
                aggregateItem.setQuantity(defaultZero(aggregateItem.getQuantity()).add(defaultZero(item.getQuantity())));
                aggregateItem.setTotalAmount(defaultZero(aggregateItem.getTotalAmount()).add(defaultZero(item.getTotalAmount())));
                aggregateItem.setSourceListCount(aggregateItem.getSourceListCount() + 1);
            }
        }

        List<ProjectListAggregateItem> aggregatedItems = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ProjectListAggregateItem item : aggregateMap.values()) {
            BigDecimal quantity = defaultZero(item.getQuantity());
            BigDecimal amount = defaultZero(item.getTotalAmount());
            if (quantity.compareTo(BigDecimal.ZERO) == 0 && amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            item.setQuantity(quantity.setScale(4, RoundingMode.HALF_UP));
            item.setTotalAmount(amount.setScale(2, RoundingMode.HALF_UP));
            item.setUnitPrice(calculateAggregatedUnitPrice(quantity, amount));
            aggregatedItems.add(item);
            totalQuantity = totalQuantity.add(quantity);
            totalAmount = totalAmount.add(amount);
        }

        aggregatedItems.sort((left, right) -> {
            String leftCode = StringUtils.hasText(left.getMaterialCode()) ? left.getMaterialCode() : "";
            String rightCode = StringUtils.hasText(right.getMaterialCode()) ? right.getMaterialCode() : "";
            return leftCode.compareToIgnoreCase(rightCode);
        });

        view.setItems(aggregatedItems);
        view.setItemCount(aggregatedItems.size());
        view.setTotalQuantity(totalQuantity.setScale(4, RoundingMode.HALF_UP));
        view.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        return view;
    }

    @Override
    @Transactional
    public ProjectList create(ProjectList projectList) {
        Project project = validateProjectList(projectList);
        if (!StringUtils.hasText(projectList.getUuid())) {
            projectList.setUuid(UUID.randomUUID().toString());
        }
        Long currentUserId = currentUserService.requireCurrentUserId();
        entryAuditService.applyCreate(projectList);
        projectList.setCreateUser(currentUserId);
        mapper.insert(projectList);
        touchProjectCustomer(project);
        return projectList;
    }

    @Override
    @Transactional
    public ProjectList update(ProjectList projectList) {
        ProjectList existing = mapper.selectByUuid(projectList.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Project list does not exist");
        }
        Project project = validateProjectList(projectList);
        entryAuditService.applyUpdate(projectList, existing);
        projectList.setUpdateUser(currentUserService.requireCurrentUserId());
        mapper.updateByUuid(projectList);
        touchProjectCustomer(project);
        return mapper.selectByUuid(projectList.getUuid());
    }

    @Override
    @Transactional
    public ProjectList audit(String uuid) {
        ProjectList existing = mapper.selectByUuid(uuid);
        if (existing == null) {
            throw new IllegalArgumentException("Project list does not exist");
        }
        entryAuditService.applyAudit(existing);
        mapper.updateAuditorByUuid(uuid, existing.getAuditorUser(), currentUserService.requireCurrentUserId());
        return mapper.selectByUuid(uuid);
    }

    @Override
    public void delete(String uuid) {
        projectListItemService.deleteByProjectListId(uuid);
        mapper.deleteByUuid(uuid);
    }

    private Project validateProjectList(ProjectList projectList) {
        if (!StringUtils.hasText(projectList.getProjectId())) {
            throw new IllegalArgumentException("Project is required");
        }
        Project project = projectMapper.selectByUuid(projectList.getProjectId());
        if (project == null) {
            throw new IllegalArgumentException("Project does not exist");
        }
        if (!StringUtils.hasText(projectList.getListName())) {
            throw new IllegalArgumentException("List name is required");
        }
        if (!StringUtils.hasText(projectList.getListType()) || !LIST_TYPES.contains(projectList.getListType())) {
            throw new IllegalArgumentException("List type is invalid");
        }
        return project;
    }

    private void touchProjectCustomer(Project project) {
        if (project != null && StringUtils.hasText(project.getCustomerId())) {
            customerService.touchActivity(project.getCustomerId());
        }
    }

    private String normalizeAggregateType(String aggregateType) {
        String normalized = aggregateType == null ? "" : aggregateType.trim().toUpperCase(Locale.ROOT);
        if (!FINAL_LIST_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Aggregate type is invalid");
        }
        return normalized;
    }

    private boolean matchesAggregateType(String listType, String aggregateType) {
        if ("FINAL_SALES".equals(aggregateType)) {
            return "INITIAL_SALES".equals(listType) || "CHANGE".equals(listType);
        }
        return "PROCUREMENT".equals(listType);
    }

    private String resolveCustomerName(Project project) {
        if (project == null || !StringUtils.hasText(project.getCustomerId())) {
            return null;
        }
        Customer customer = customerMapper.selectByUuid(project.getCustomerId());
        return customer != null ? customer.getCustomerName() : null;
    }

    private String buildAggregateKey(ProjectListItem item) {
        if (StringUtils.hasText(item.getMaterialId())) {
            return "material:" + item.getMaterialId();
        }
        if (StringUtils.hasText(item.getMaterialCode())) {
            return "code:" + item.getMaterialCode();
        }
        return String.join("|",
                item.getItemName() == null ? "" : item.getItemName(),
                item.getModel() == null ? "" : item.getModel(),
                item.getBrand() == null ? "" : item.getBrand(),
                item.getUnit() == null ? "" : item.getUnit()
        );
    }

    private ProjectListAggregateItem createAggregateItem(ProjectListItem source) {
        ProjectListAggregateItem item = new ProjectListAggregateItem();
        item.setMaterialId(source.getMaterialId());
        item.setMaterialCode(source.getMaterialCode());
        item.setItemName(source.getItemName());
        item.setModel(source.getModel());
        item.setBrand(source.getBrand());
        item.setUnit(source.getUnit());
        item.setQuantity(BigDecimal.ZERO);
        item.setTotalAmount(BigDecimal.ZERO);
        item.setSourceListCount(0);
        return item;
    }

    private BigDecimal calculateAggregatedUnitPrice(BigDecimal quantity, BigDecimal amount) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return amount.divide(quantity, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
