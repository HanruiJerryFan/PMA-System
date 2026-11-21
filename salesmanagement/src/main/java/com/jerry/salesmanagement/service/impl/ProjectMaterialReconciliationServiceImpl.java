package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.WarehouseDocumentRules;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.ProjectListItemMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentItemMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.ProjectListItem;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.pojo.dto.MaterialReconciliationItemDto;
import com.jerry.salesmanagement.pojo.dto.ProjectMaterialReconciliationDto;
import com.jerry.salesmanagement.pojo.dto.ProjectReconciliationOverviewItemDto;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.ProjectMaterialReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jerry.salesmanagement.common.FinanceVoucherMetrics.defaultZero;
import static com.jerry.salesmanagement.common.FinanceVoucherMetrics.scale2;
import static com.jerry.salesmanagement.common.FinanceVoucherMetrics.scale4;

@Service
public class ProjectMaterialReconciliationServiceImpl implements ProjectMaterialReconciliationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ProjectListMapper projectListMapper;

    @Autowired
    private ProjectListItemMapper projectListItemMapper;

    @Autowired
    private WarehouseDocumentMapper warehouseDocumentMapper;

    @Autowired
    private WarehouseDocumentItemMapper warehouseDocumentItemMapper;

    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public List<ProjectReconciliationOverviewItemDto> getOverview() {
        boolean canViewInventory = currentUserService.hasAnyAuthority("inventory.access", "inventory.manage");
        List<Project> projects = projectMapper.selectAll();
        if (projects.isEmpty()) {
            return List.of();
        }

        Map<String, String> customerNameByUuid = new HashMap<>();
        if (customerMapper != null) {
            customerMapper.selectAll().stream()
                    .filter(c -> c != null && StringUtils.hasText(c.getUuid()))
                    .forEach(c -> customerNameByUuid.put(c.getUuid(), c.getCustomerName()));
        }

        List<ProjectList> allLists = projectListMapper.selectAll();
        Map<String, List<ProjectList>> listsByProjectId = allLists.stream()
                .filter(l -> l != null && StringUtils.hasText(l.getProjectId()))
                .collect(Collectors.groupingBy(ProjectList::getProjectId));

        List<String> listIds = allLists.stream()
                .map(ProjectList::getUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, List<ProjectListItem>> itemsByListId = listIds.isEmpty()
                ? Map.of()
                : projectListItemMapper.selectByProjectListIds(listIds).stream()
                        .filter(i -> i != null && StringUtils.hasText(i.getProjectListId()))
                        .collect(Collectors.groupingBy(ProjectListItem::getProjectListId));

        Map<String, List<WarehouseDocument>> docsByProjectId = new HashMap<>();
        Map<String, List<WarehouseDocumentItem>> docItemsByDocNumber = Map.of();
        Map<String, List<InventoryTransaction>> standaloneTxnsByProjectId = new HashMap<>();

        if (canViewInventory) {
            List<WarehouseDocument> allDocs = warehouseDocumentMapper.selectAll().stream()
                    .filter(d -> d != null
                            && StringUtils.hasText(d.getProjectId())
                            && !WarehouseDocumentRules.TRANSFER_SOURCE.equals(d.getSourceRefType()))
                    .toList();
            for (WarehouseDocument doc : allDocs) {
                docsByProjectId.computeIfAbsent(doc.getProjectId(), k -> new ArrayList<>()).add(doc);
            }
            List<String> docNumbers = allDocs.stream()
                    .map(WarehouseDocument::getDocNumber)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (!docNumbers.isEmpty()) {
                docItemsByDocNumber = warehouseDocumentItemMapper.selectByDocNumbers(docNumbers).stream()
                        .filter(i -> i != null && StringUtils.hasText(i.getDocNumber()))
                        .collect(Collectors.groupingBy(WarehouseDocumentItem::getDocNumber));
            }
            if (inventoryTransactionMapper != null) {
                for (InventoryTransaction txn : inventoryTransactionMapper.selectAll()) {
                    if (txn != null
                            && StringUtils.hasText(txn.getProjectId())
                            && !"WAREHOUSE_DOC".equals(txn.getSourceRefType())) {
                        standaloneTxnsByProjectId.computeIfAbsent(txn.getProjectId(), k -> new ArrayList<>()).add(txn);
                    }
                }
            }
        }

        List<ProjectReconciliationOverviewItemDto> overview = new ArrayList<>();
        for (Project project : projects) {
            if (project == null || !StringUtils.hasText(project.getUuid())) {
                continue;
            }
            ProjectMaterialReconciliationDto detail = computeProjectReconciliation(
                    project,
                    customerNameByUuid.get(project.getCustomerId()),
                    listsByProjectId.getOrDefault(project.getUuid(), List.of()),
                    itemsByListId,
                    docsByProjectId.getOrDefault(project.getUuid(), List.of()),
                    docItemsByDocNumber,
                    standaloneTxnsByProjectId.getOrDefault(project.getUuid(), List.of()),
                    canViewInventory
            );
            ProjectReconciliationOverviewItemDto row = new ProjectReconciliationOverviewItemDto();
            row.setProjectId(detail.getProjectId());
            row.setProjectNumber(detail.getProjectNumber());
            row.setProjectName(detail.getProjectName());
            row.setCustomerName(detail.getCustomerName());
            row.setTotalMaterialLines(detail.getTotalMaterialLines());
            row.setShortageLineCount(detail.getShortageLineCount());
            row.setPendingShipmentLineCount(detail.getPendingShipmentLineCount());
            row.setOverShippedLineCount(detail.getOverShippedLineCount());
            row.setPlannedSalesAmount(detail.getPlannedSalesAmount());
            row.setPlannedProcurementAmount(detail.getPlannedProcurementAmount());
            row.setActualSuppliedAmount(detail.getActualSuppliedAmount());
            row.setActualShippedAmount(detail.getActualShippedAmount());
            row.setFulfillmentRate(detail.getFulfillmentRate());
            overview.add(row);
        }

        overview.sort(Comparator.comparing(
                r -> r.getProjectNumber() == null ? "" : r.getProjectNumber(),
                String::compareToIgnoreCase
        ));
        return overview;
    }

    @Override
    public ProjectMaterialReconciliationDto getProjectReconciliation(String projectUuid) {
        if (!StringUtils.hasText(projectUuid)) {
            throw new IllegalArgumentException("Project is required");
        }
        Project project = projectMapper.selectByUuid(projectUuid);
        if (project == null) {
            throw new IllegalArgumentException("Project does not exist");
        }

        boolean canViewInventory = currentUserService.hasAnyAuthority("inventory.access", "inventory.manage");
        String customerName = null;
        if (StringUtils.hasText(project.getCustomerId()) && customerMapper != null) {
            Customer customer = customerMapper.selectByUuid(project.getCustomerId());
            customerName = customer != null ? customer.getCustomerName() : null;
        }

        List<ProjectList> lists = projectListMapper.selectByProjectId(projectUuid);
        List<String> listIds = lists.stream()
                .map(ProjectList::getUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, List<ProjectListItem>> itemsByListId = listIds.isEmpty()
                ? Map.of()
                : projectListItemMapper.selectByProjectListIds(listIds).stream()
                        .filter(i -> i != null && StringUtils.hasText(i.getProjectListId()))
                        .collect(Collectors.groupingBy(ProjectListItem::getProjectListId));

        List<WarehouseDocument> docs = List.of();
        Map<String, List<WarehouseDocumentItem>> docItemsByDocNumber = Map.of();
        List<InventoryTransaction> standaloneTxns = List.of();

        if (canViewInventory) {
            docs = warehouseDocumentMapper.selectAll().stream()
                    .filter(d -> d != null
                            && projectUuid.equals(d.getProjectId())
                            && !WarehouseDocumentRules.TRANSFER_SOURCE.equals(d.getSourceRefType()))
                    .toList();
            List<String> docNumbers = docs.stream()
                    .map(WarehouseDocument::getDocNumber)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (!docNumbers.isEmpty()) {
                docItemsByDocNumber = warehouseDocumentItemMapper.selectByDocNumbers(docNumbers).stream()
                        .filter(i -> i != null && StringUtils.hasText(i.getDocNumber()))
                        .collect(Collectors.groupingBy(WarehouseDocumentItem::getDocNumber));
            }
            if (inventoryTransactionMapper != null) {
                standaloneTxns = inventoryTransactionMapper.selectAll().stream()
                        .filter(t -> t != null
                                && projectUuid.equals(t.getProjectId())
                                && !"WAREHOUSE_DOC".equals(t.getSourceRefType()))
                        .toList();
            }
        }

        return computeProjectReconciliation(
                project,
                customerName,
                lists,
                itemsByListId,
                docs,
                docItemsByDocNumber,
                standaloneTxns,
                canViewInventory
        );
    }

    private ProjectMaterialReconciliationDto computeProjectReconciliation(
            Project project,
            String customerName,
            List<ProjectList> lists,
            Map<String, List<ProjectListItem>> itemsByListId,
            List<WarehouseDocument> docs,
            Map<String, List<WarehouseDocumentItem>> docItemsByDocNumber,
            List<InventoryTransaction> standaloneTxns,
            boolean canViewInventory
    ) {
        Map<String, MaterialReconciliationItemDto> lines = new LinkedHashMap<>();

        for (ProjectList list : lists) {
            String listType = list.getListType();
            boolean salesSide = "INITIAL_SALES".equals(listType) || "CHANGE".equals(listType);
            boolean procurementSide = "PROCUREMENT".equals(listType);
            if (!salesSide && !procurementSide) {
                continue;
            }
            for (ProjectListItem item : itemsByListId.getOrDefault(list.getUuid(), List.of())) {
                String key = buildMaterialKey(
                        item.getMaterialId(),
                        item.getMaterialCode(),
                        item.getItemName(),
                        item.getModel(),
                        item.getBrand(),
                        item.getUnit()
                );
                MaterialReconciliationItemDto line = lines.computeIfAbsent(key, k -> initLine(
                        key,
                        item.getMaterialId(),
                        item.getMaterialCode(),
                        item.getItemName(),
                        item.getModel(),
                        item.getBrand(),
                        item.getUnit()
                ));
                BigDecimal qty = defaultZero(item.getQuantity());
                BigDecimal amt = resolveAmount(item.getTotalAmount(), item.getQuantity(), item.getUnitPrice());
                if (salesSide) {
                    line.setPlannedSalesQty(line.getPlannedSalesQty().add(qty));
                    line.setPlannedSalesAmount(line.getPlannedSalesAmount().add(amt));
                } else {
                    line.setPlannedProcurementQty(line.getPlannedProcurementQty().add(qty));
                    line.setPlannedProcurementAmount(line.getPlannedProcurementAmount().add(amt));
                }
            }
        }

        if (canViewInventory) {
            for (WarehouseDocument doc : docs) {
                String docType = doc.getDocType();
                for (WarehouseDocumentItem item : docItemsByDocNumber.getOrDefault(doc.getDocNumber(), List.of())) {
                    String key = buildMaterialKey(
                            item.getMaterialId(),
                            item.getMaterialCode(),
                            item.getMaterialName(),
                            item.getModel(),
                            item.getBrand(),
                            item.getUnit()
                    );
                    MaterialReconciliationItemDto line = lines.computeIfAbsent(key, k -> initLine(
                            key,
                            item.getMaterialId(),
                            item.getMaterialCode(),
                            item.getMaterialName(),
                            item.getModel(),
                            item.getBrand(),
                            item.getUnit()
                    ));
                    BigDecimal qty = defaultZero(item.getQuantity());
                    BigDecimal amt = resolveAmount(item.getAmount(), item.getQuantity(), item.getUnitPrice());
                    if ("INBOUND_NOTE".equals(docType)) {
                        line.setPurchasedQty(line.getPurchasedQty().add(qty));
                        line.setPurchasedAmount(line.getPurchasedAmount().add(amt));
                    } else if ("TRANSFER_JTX".equals(docType)) {
                        line.setTransferInQty(line.getTransferInQty().add(qty));
                        line.setTransferInAmount(line.getTransferInAmount().add(amt));
                    } else if ("TRANSFER_XTJ".equals(docType)) {
                        line.setTransferOutQty(line.getTransferOutQty().add(qty));
                        line.setTransferOutAmount(line.getTransferOutAmount().add(amt));
                    } else if ("OUTBOUND_NOTE".equals(docType)) {
                        line.setShippedQty(line.getShippedQty().add(qty));
                        line.setShippedAmount(line.getShippedAmount().add(amt));
                    }
                }
            }

            for (InventoryTransaction txn : standaloneTxns) {
                String key = buildMaterialKey(
                        txn.getMaterialId(),
                        txn.getMaterialCode(),
                        txn.getMaterialName(),
                        txn.getModel(),
                        txn.getBrand(),
                        null
                );
                MaterialReconciliationItemDto line = lines.computeIfAbsent(key, k -> initLine(
                        key,
                        txn.getMaterialId(),
                        txn.getMaterialCode(),
                        txn.getMaterialName(),
                        txn.getModel(),
                        txn.getBrand(),
                        null
                ));
                BigDecimal qty = defaultZero(txn.getQuantity());
                BigDecimal amt = resolveAmount(txn.getAmount(), txn.getQuantity(), txn.getUnitPrice());
                String cat = txn.getCategory();
                String txnType = txn.getTxnType();
                if ("IN".equals(txnType)) {
                    if ("WAREHOUSE_TRANSFER_TO_PROJECT".equals(cat)) {
                        line.setTransferInQty(line.getTransferInQty().add(qty));
                        line.setTransferInAmount(line.getTransferInAmount().add(amt));
                    } else {
                        line.setPurchasedQty(line.getPurchasedQty().add(qty));
                        line.setPurchasedAmount(line.getPurchasedAmount().add(amt));
                    }
                } else if ("OUT".equals(txnType)) {
                    if ("PROJECT_TRANSFER_TO_WAREHOUSE".equals(cat)) {
                        line.setTransferOutQty(line.getTransferOutQty().add(qty));
                        line.setTransferOutAmount(line.getTransferOutAmount().add(amt));
                    } else {
                        line.setShippedQty(line.getShippedQty().add(qty));
                        line.setShippedAmount(line.getShippedAmount().add(amt));
                    }
                }
            }
        }

        List<MaterialReconciliationItemDto> finalizedLines = new ArrayList<>();
        int shortageCount = 0;
        int pendingShipmentCount = 0;
        int overShippedCount = 0;
        int overProcuredCount = 0;
        int matchedCount = 0;

        BigDecimal totalPlannedSalesQty = BigDecimal.ZERO;
        BigDecimal totalShippedQty = BigDecimal.ZERO;
        BigDecimal totalPlannedSalesAmount = BigDecimal.ZERO;
        BigDecimal totalPlannedProcurementAmount = BigDecimal.ZERO;
        BigDecimal totalActualSuppliedAmount = BigDecimal.ZERO;
        BigDecimal totalActualShippedAmount = BigDecimal.ZERO;

        for (MaterialReconciliationItemDto line : lines.values()) {
            BigDecimal plannedSalesQty = scale4(line.getPlannedSalesQty());
            BigDecimal plannedProcurementQty = scale4(line.getPlannedProcurementQty());
            BigDecimal purchasedQty = scale4(line.getPurchasedQty());
            BigDecimal transferInQty = scale4(line.getTransferInQty());
            BigDecimal transferOutQty = scale4(line.getTransferOutQty());
            BigDecimal shippedQty = scale4(line.getShippedQty());

            if (plannedSalesQty.compareTo(BigDecimal.ZERO) == 0
                    && plannedProcurementQty.compareTo(BigDecimal.ZERO) == 0
                    && purchasedQty.compareTo(BigDecimal.ZERO) == 0
                    && transferInQty.compareTo(BigDecimal.ZERO) == 0
                    && transferOutQty.compareTo(BigDecimal.ZERO) == 0
                    && shippedQty.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal netSuppliedQty = scale4(purchasedQty.add(transferInQty).subtract(transferOutQty));
            BigDecimal procurementGapQty = scale4(plannedProcurementQty.subtract(netSuppliedQty));
            BigDecimal shipmentGapQty = scale4(plannedSalesQty.subtract(shippedQty));
            BigDecimal unshippedOnHandQty = scale4(netSuppliedQty.subtract(shippedQty));

            line.setPlannedSalesQty(plannedSalesQty);
            line.setPlannedSalesAmount(scale2(line.getPlannedSalesAmount()));
            line.setPlannedProcurementQty(plannedProcurementQty);
            line.setPlannedProcurementAmount(scale2(line.getPlannedProcurementAmount()));
            line.setPurchasedQty(purchasedQty);
            line.setPurchasedAmount(scale2(line.getPurchasedAmount()));
            line.setTransferInQty(transferInQty);
            line.setTransferInAmount(scale2(line.getTransferInAmount()));
            line.setTransferOutQty(transferOutQty);
            line.setTransferOutAmount(scale2(line.getTransferOutAmount()));
            line.setNetSuppliedQty(netSuppliedQty);
            line.setShippedQty(shippedQty);
            line.setShippedAmount(scale2(line.getShippedAmount()));
            line.setProcurementGapQty(procurementGapQty);
            line.setShipmentGapQty(shipmentGapQty);
            line.setUnshippedOnHandQty(unshippedOnHandQty);

            String status = resolveStatus(
                    canViewInventory,
                    plannedSalesQty,
                    plannedProcurementQty,
                    netSuppliedQty,
                    shippedQty,
                    procurementGapQty,
                    shipmentGapQty
            );
            line.setStatus(status);

            switch (status) {
                case "PROCUREMENT_SHORTAGE" -> shortageCount++;
                case "PENDING_SHIPMENT" -> pendingShipmentCount++;
                case "OVER_SHIPPED" -> overShippedCount++;
                case "OVER_PROCURED" -> overProcuredCount++;
                case "MATCHED" -> matchedCount++;
                default -> {
                }
            }

            totalPlannedSalesQty = totalPlannedSalesQty.add(plannedSalesQty);
            totalShippedQty = totalShippedQty.add(shippedQty);
            totalPlannedSalesAmount = totalPlannedSalesAmount.add(line.getPlannedSalesAmount());
            totalPlannedProcurementAmount = totalPlannedProcurementAmount.add(line.getPlannedProcurementAmount());
            BigDecimal suppliedAmt = line.getPurchasedAmount()
                    .add(line.getTransferInAmount())
                    .subtract(line.getTransferOutAmount());
            totalActualSuppliedAmount = totalActualSuppliedAmount.add(suppliedAmt);
            totalActualShippedAmount = totalActualShippedAmount.add(line.getShippedAmount());

            finalizedLines.add(line);
        }

        finalizedLines.sort(Comparator.comparing(
                l -> l.getMaterialCode() != null ? l.getMaterialCode() : (l.getItemName() != null ? l.getItemName() : ""),
                String::compareToIgnoreCase
        ));

        ProjectMaterialReconciliationDto dto = new ProjectMaterialReconciliationDto();
        dto.setProjectId(project.getUuid());
        dto.setProjectNumber(project.getProjectNumber());
        dto.setProjectName(project.getProjectName());
        dto.setCustomerName(customerName);
        dto.setCanViewInventory(canViewInventory);
        dto.setTotalMaterialLines(finalizedLines.size());
        dto.setShortageLineCount(shortageCount);
        dto.setPendingShipmentLineCount(pendingShipmentCount);
        dto.setOverShippedLineCount(overShippedCount);
        dto.setOverProcuredLineCount(overProcuredCount);
        dto.setMatchedLineCount(matchedCount);
        dto.setPlannedSalesAmount(scale2(totalPlannedSalesAmount));
        dto.setPlannedProcurementAmount(scale2(totalPlannedProcurementAmount));
        dto.setActualSuppliedAmount(canViewInventory ? scale2(totalActualSuppliedAmount) : null);
        dto.setActualShippedAmount(canViewInventory ? scale2(totalActualShippedAmount) : null);
        if (canViewInventory && totalPlannedSalesQty.compareTo(BigDecimal.ZERO) > 0) {
            dto.setFulfillmentRate(
                    totalShippedQty.multiply(HUNDRED).divide(totalPlannedSalesQty, 2, RoundingMode.HALF_UP)
            );
        }
        dto.setItems(finalizedLines);
        return dto;
    }

    private String resolveStatus(
            boolean canViewInventory,
            BigDecimal plannedSalesQty,
            BigDecimal plannedProcurementQty,
            BigDecimal netSuppliedQty,
            BigDecimal shippedQty,
            BigDecimal procurementGapQty,
            BigDecimal shipmentGapQty
    ) {
        if (!canViewInventory) {
            return "NO_INVENTORY_PERMISSION";
        }
        if (plannedSalesQty.compareTo(BigDecimal.ZERO) == 0
                && plannedProcurementQty.compareTo(BigDecimal.ZERO) == 0
                && (netSuppliedQty.compareTo(BigDecimal.ZERO) > 0 || shippedQty.compareTo(BigDecimal.ZERO) > 0)) {
            return "UNPLANNED";
        }
        if (shipmentGapQty.compareTo(BigDecimal.ZERO) < 0) {
            return "OVER_SHIPPED";
        }
        if (procurementGapQty.compareTo(BigDecimal.ZERO) > 0) {
            return "PROCUREMENT_SHORTAGE";
        }
        if (shipmentGapQty.compareTo(BigDecimal.ZERO) > 0) {
            return "PENDING_SHIPMENT";
        }
        if (procurementGapQty.compareTo(BigDecimal.ZERO) < 0) {
            return "OVER_PROCURED";
        }
        return "MATCHED";
    }

    private MaterialReconciliationItemDto initLine(
            String key,
            String materialId,
            String materialCode,
            String itemName,
            String model,
            String brand,
            String unit
    ) {
        MaterialReconciliationItemDto line = new MaterialReconciliationItemDto();
        line.setMaterialKey(key);
        line.setMaterialId(materialId);
        line.setMaterialCode(materialCode);
        line.setItemName(itemName);
        line.setModel(model);
        line.setBrand(brand);
        line.setUnit(unit);
        line.setPlannedSalesQty(BigDecimal.ZERO);
        line.setPlannedSalesAmount(BigDecimal.ZERO);
        line.setPlannedProcurementQty(BigDecimal.ZERO);
        line.setPlannedProcurementAmount(BigDecimal.ZERO);
        line.setPurchasedQty(BigDecimal.ZERO);
        line.setPurchasedAmount(BigDecimal.ZERO);
        line.setTransferInQty(BigDecimal.ZERO);
        line.setTransferInAmount(BigDecimal.ZERO);
        line.setTransferOutQty(BigDecimal.ZERO);
        line.setTransferOutAmount(BigDecimal.ZERO);
        line.setShippedQty(BigDecimal.ZERO);
        line.setShippedAmount(BigDecimal.ZERO);
        return line;
    }

    private String buildMaterialKey(
            String materialId,
            String materialCode,
            String itemName,
            String model,
            String brand,
            String unit
    ) {
        if (StringUtils.hasText(materialId)) {
            return "material:" + materialId.trim();
        }
        if (StringUtils.hasText(materialCode)) {
            return "code:" + materialCode.trim().toUpperCase();
        }
        return String.join("|",
                itemName == null ? "" : itemName.trim(),
                model == null ? "" : model.trim(),
                brand == null ? "" : brand.trim(),
                unit == null ? "" : unit.trim()
        );
    }

    private BigDecimal resolveAmount(BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice) {
        if (amount != null) {
            return amount;
        }
        if (quantity != null && unitPrice != null) {
            return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
