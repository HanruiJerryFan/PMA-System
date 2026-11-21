package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.WarehouseDocumentRules;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalItemDto;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalSummaryDto;
import com.jerry.salesmanagement.service.ApprovalWorkbenchService;
import com.jerry.salesmanagement.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalWorkbenchServiceImpl implements ApprovalWorkbenchService {

    @Autowired
    private ProjectListMapper projectListMapper;

    @Autowired
    private WarehouseDocumentMapper warehouseDocumentMapper;

    @Autowired
    private FinanceVoucherMapper financeVoucherMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public PendingApprovalSummaryDto getPendingApprovals() {
        boolean canAuditLists = currentUserService.hasAnyAuthority("project.list.audit", "project.manage");
        boolean canAuditWarehouseDocs = currentUserService.hasAnyAuthority("inventory.warehouse-doc.audit", "inventory.manage");
        boolean canAuditFinanceVouchers = currentUserService.hasAnyAuthority("finance.voucher.audit", "finance.manage");

        List<ProjectList> pendingLists = canAuditLists
                ? projectListMapper.selectAll().stream()
                .filter(item -> item != null && item.getAuditorUser() == null)
                .toList()
                : List.of();

        List<WarehouseDocument> pendingDocs = canAuditWarehouseDocs
                ? warehouseDocumentMapper.selectAll().stream()
                .filter(item -> item != null
                        && item.getAuditorUser() == null
                        && !WarehouseDocumentRules.TRANSFER_SOURCE.equals(item.getSourceRefType()))
                .toList()
                : List.of();

        List<FinanceVoucher> pendingVouchers = canAuditFinanceVouchers
                ? financeVoucherMapper.selectAll().stream()
                .filter(item -> item != null && item.getAuditorUser() == null)
                .toList()
                : List.of();

        Map<String, Project> projectByUuid = (!pendingLists.isEmpty() || !pendingDocs.isEmpty() || !pendingVouchers.isEmpty())
                ? indexProjects()
                : Map.of();

        PendingApprovalSummaryDto summary = new PendingApprovalSummaryDto();
        summary.setCanAuditProjectLists(canAuditLists);
        summary.setCanAuditWarehouseDocs(canAuditWarehouseDocs);
        summary.setCanAuditFinanceVouchers(canAuditFinanceVouchers);

        List<PendingApprovalItemDto> listItems = new ArrayList<>();
        for (ProjectList item : pendingLists) {
            listItems.add(toProjectListItem(item, projectByUuid.get(item.getProjectId())));
        }

        List<PendingApprovalItemDto> docItems = new ArrayList<>();
        for (WarehouseDocument item : pendingDocs) {
            docItems.add(toWarehouseDocItem(item, projectByUuid.get(item.getProjectId())));
        }

        List<PendingApprovalItemDto> voucherItems = new ArrayList<>();
        for (FinanceVoucher item : pendingVouchers) {
            voucherItems.add(toFinanceVoucherItem(item, projectByUuid.get(item.getProjectId())));
        }

        summary.setProjectLists(listItems);
        summary.setWarehouseDocs(docItems);
        summary.setFinanceVouchers(voucherItems);
        summary.setProjectListCount(listItems.size());
        summary.setWarehouseDocCount(docItems.size());
        summary.setFinanceVoucherCount(voucherItems.size());
        summary.setTotalPendingCount(listItems.size() + docItems.size() + voucherItems.size());
        return summary;
    }

    private PendingApprovalItemDto toProjectListItem(ProjectList item, Project project) {
        PendingApprovalItemDto dto = new PendingApprovalItemDto();
        dto.setModule("PROJECT_LIST");
        dto.setId(item.getUuid());
        dto.setCode(item.getListType());
        dto.setTitle(item.getListName());
        dto.setSubType(item.getListType());
        dto.setSubTypeLabel(resolveListTypeLabel(item.getListType()));
        dto.setProjectId(item.getProjectId());
        dto.setProjectNumber(project != null ? project.getProjectNumber() : null);
        dto.setProjectName(project != null ? project.getProjectName() : null);
        dto.setCounterpartyName(item.getCustomerName());
        dto.setBizDate(formatDate(item.getEntryDate()));
        dto.setEntryUser(item.getEntryUser());
        dto.setEntryUserName(item.getEntryUserName());
        dto.setCreateTime(item.getCreateTime());
        dto.setDetailRoute("/project/list-dashboard?projectUuid=" + item.getProjectId() + "&listUuid=" + item.getUuid());
        return dto;
    }

    private PendingApprovalItemDto toWarehouseDocItem(WarehouseDocument item, Project project) {
        PendingApprovalItemDto dto = new PendingApprovalItemDto();
        dto.setModule("WAREHOUSE_DOC");
        dto.setId(item.getDocNumber());
        dto.setCode(item.getDocNumber());
        dto.setTitle(item.getDocNumber());
        dto.setSubType(item.getDocType());
        dto.setSubTypeLabel(WarehouseDocumentRules.label(item.getDocType()));
        dto.setProjectId(item.getProjectId());
        dto.setProjectNumber(project != null ? project.getProjectNumber() : null);
        dto.setProjectName(project != null ? project.getProjectName() : null);
        dto.setCounterpartyName(item.getCounterpartyName());
        dto.setAmount(item.getTotalAmount());
        dto.setBizDate(formatDate(item.getDocDate()));
        dto.setEntryUser(item.getEntryUser());
        dto.setEntryUserName(item.getEntryUserName());
        dto.setCreateTime(item.getCreateTime());
        dto.setDetailRoute("/inventory/warehouse");
        return dto;
    }

    private PendingApprovalItemDto toFinanceVoucherItem(FinanceVoucher item, Project project) {
        PendingApprovalItemDto dto = new PendingApprovalItemDto();
        dto.setModule("FINANCE_VOUCHER");
        dto.setId(item.getUuid());
        dto.setCode(item.getVoucherNo());
        dto.setTitle(item.getSummary());
        dto.setSubType(item.getTransactionDirection());
        dto.setSubTypeLabel("RECEIVE".equals(item.getTransactionDirection()) ? "收款凭证" : "付款凭证");
        dto.setProjectId(item.getProjectId());
        dto.setProjectNumber(project != null ? project.getProjectNumber() : null);
        dto.setProjectName(project != null ? project.getProjectName() : null);
        dto.setCounterpartyName(item.getCounterpartyNameSnapshot());
        dto.setAmount("RECEIVE".equals(item.getTransactionDirection())
                ? item.getActualIncomeAmount()
                : item.getActualExpenseAmount());
        dto.setBizDate(item.getOccurredOn() != null ? item.getOccurredOn().toString() : null);
        dto.setEntryUser(item.getEntryUser());
        dto.setEntryUserName(item.getEntryUserName());
        dto.setCreateTime(item.getCreatedAt());
        dto.setDetailRoute(StringUtils.hasText(item.getProjectId())
                ? "/finance/vouchers?projectUuid=" + item.getProjectId()
                : "/finance/vouchers");
        return dto;
    }

    private Map<String, Project> indexProjects() {
        Map<String, Project> map = new HashMap<>();
        if (projectMapper == null) {
            return map;
        }
        projectMapper.selectAll().stream()
                .filter(p -> p != null && StringUtils.hasText(p.getUuid()))
                .forEach(p -> map.put(p.getUuid(), p));
        return map;
    }

    private String resolveListTypeLabel(String listType) {
        if ("INITIAL_SALES".equals(listType)) {
            return "初始销售清单";
        }
        if ("PROCUREMENT".equals(listType)) {
            return "采购清单";
        }
        if ("CHANGE".equals(listType)) {
            return "销售变更清单";
        }
        return listType;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        LocalDate localDate = date instanceof java.sql.Date sqlDate
                ? sqlDate.toLocalDate()
                : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.toString();
    }
}
