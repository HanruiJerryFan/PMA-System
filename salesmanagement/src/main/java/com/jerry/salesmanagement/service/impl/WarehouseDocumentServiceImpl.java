package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.WarehouseDocumentRules;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentItemMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.EntryAuditService;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import com.jerry.salesmanagement.service.SystemConfigService;
import com.jerry.salesmanagement.service.WarehouseDocumentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WarehouseDocumentServiceImpl implements WarehouseDocumentService {

    private static final String CONFIG_ALLOW_NEGATIVE_STOCK = "inventory.allow_negative_stock";
    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList("INBOUND_NOTE", "OUTBOUND_NOTE", "TRANSFER_JTX", "TRANSFER_XTJ");
    private static final List<String> ALLOWED_CATEGORIES = Arrays.asList(
            "PROJECT_PURCHASE",
            "PROJECT_SALES",
            "CENTRALIZED_PURCHASE",
            "WAREHOUSE_TRANSFER_TO_PROJECT",
            "PROJECT_TRANSFER_TO_WAREHOUSE"
    );
    private static final List<String> PROJECT_REQUIRED_CATEGORIES = Arrays.asList(
            "PROJECT_PURCHASE",
            "PROJECT_SALES",
            "WAREHOUSE_TRANSFER_TO_PROJECT",
            "PROJECT_TRANSFER_TO_WAREHOUSE"
    );
    private static final List<String> INBOUND_ALLOWED_CUSTOMER_TYPE_CODES = Arrays.asList("3", "4");
    private static final List<String> OUTBOUND_ALLOWED_CUSTOMER_TYPE_CODES = Arrays.asList("2", "3", "4", "5");
    private static final DateTimeFormatter DOC_NUMBER_YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    private WarehouseDocumentMapper warehouseDocumentMapper;

    @Autowired
    private WarehouseDocumentItemMapper warehouseDocumentItemMapper;

    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerTypeMapper customerTypeMapper;

    @Autowired
    private MaterialMasterMapper materialMasterMapper;

    @Autowired
    private ProductBrandMapper productBrandMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private CodeSequenceService codeSequenceService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private EntryAuditService entryAuditService;

    @Autowired(required = false)
    private SystemConfigService systemConfigService;

    @Autowired(required = false)
    private AttachmentService attachmentService;

    @Override
    public List<WarehouseDocument> getAll() {
        List<WarehouseDocument> documents = warehouseDocumentMapper.selectAll();
        if (documents.isEmpty()) {
            return documents;
        }
        List<String> docNumbers = documents.stream()
                .map(WarehouseDocument::getDocNumber)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (docNumbers.isEmpty()) {
            documents.forEach(document -> document.setItems(new ArrayList<>()));
            return documents;
        }
        Map<String, List<WarehouseDocumentItem>> itemsByDocNumber = warehouseDocumentItemMapper
                .selectByDocNumbers(docNumbers)
                .stream()
                .collect(Collectors.groupingBy(WarehouseDocumentItem::getDocNumber));
        documents.forEach(document -> document.setItems(
                new ArrayList<>(itemsByDocNumber.getOrDefault(document.getDocNumber(), List.of()))
        ));
        return documents;
    }

    @Override
    public WarehouseDocument getByDocNumber(String docNumber) {
        WarehouseDocument document = warehouseDocumentMapper.selectByDocNumber(docNumber);
        attachItems(document);
        return document;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WarehouseDocument create(WarehouseDocument warehouseDocument) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        entryAuditService.applyCreate(warehouseDocument);
        warehouseDocument.setCreateUser(currentUserId);
        // Never allow client-supplied docNumber on create so CodeSequenceService serializes allocation.
        warehouseDocument.setDocNumber(null);
        validateAndHydrate(warehouseDocument, null);
        warehouseDocumentMapper.insert(warehouseDocument);
        persistItemsAndTransactions(warehouseDocument);
        synchronizeTransfer(warehouseDocument);
        assertNonNegativeStock(collectAffectedStockKeys(warehouseDocument, null, linkedInbound(warehouseDocument.getDocNumber())));
        touchCounterparty(warehouseDocument);
        return getByDocNumber(warehouseDocument.getDocNumber());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WarehouseDocument update(WarehouseDocument warehouseDocument) {
        codeSequenceService.withLock(sequenceKey(warehouseDocument.getDocNumber()), () -> {
            WarehouseDocument identity = warehouseDocumentMapper.selectIdentityForUpdate(warehouseDocument.getDocNumber());
            requireEditable(identity);
            WarehouseDocument existing = warehouseDocumentMapper.selectByDocNumber(warehouseDocument.getDocNumber());
            if (!StringUtils.hasText(warehouseDocument.getDocType())) {
                throw new IllegalArgumentException("Document type is required");
            }
            String normalizedDocType = warehouseDocument.getDocType().trim().toUpperCase(Locale.ROOT);
            if (!Objects.equals(existing.getDocType(), normalizedDocType)) {
                throw new IllegalArgumentException("单据类型创建后不可修改，请新建对应类型单据");
            }
            List<WarehouseDocumentItem> oldItems = warehouseDocumentItemMapper.selectByDocNumber(existing.getDocNumber());
            WarehouseDocument oldInbound = linkedInbound(existing.getDocNumber());
            Set<StockKey> affected = new LinkedHashSet<>();
            addStockKeys(affected, existing.getWarehouseId(), oldItems);
            if (oldInbound != null) {
                addStockKeys(affected, oldInbound.getWarehouseId(), warehouseDocumentItemMapper.selectByDocNumber(oldInbound.getDocNumber()));
            }

            entryAuditService.applyUpdate(warehouseDocument, existing);
            warehouseDocument.setCreateUser(existing.getCreateUser());
            warehouseDocument.setUpdateUser(currentUserService.requireCurrentUserId());
            validateAndHydrate(warehouseDocument, existing, oldItems);
            if (warehouseDocumentMapper.update(warehouseDocument) != 1) throw new IllegalStateException("单据更新失败");
            replaceItemsAndTransactions(warehouseDocument);
            synchronizeTransfer(warehouseDocument);
            WarehouseDocument currentInbound = linkedInbound(warehouseDocument.getDocNumber());
            addStockKeys(affected, warehouseDocument.getWarehouseId(), warehouseDocument.getItems());
            if (currentInbound != null) {
                addStockKeys(affected, currentInbound.getWarehouseId(), warehouseDocumentItemMapper.selectByDocNumber(currentInbound.getDocNumber()));
            }
            assertNonNegativeStock(affected);
            touchCounterparty(warehouseDocument);
        });
        return getByDocNumber(warehouseDocument.getDocNumber());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WarehouseDocument audit(String docNumber) {
        codeSequenceService.withLock(sequenceKey(docNumber), () -> {
            WarehouseDocument identity = warehouseDocumentMapper.selectIdentityForUpdate(docNumber);
            requireEditable(identity);
            WarehouseDocument existing = warehouseDocumentMapper.selectByDocNumber(docNumber);
            if (existing == null) {
                throw new IllegalArgumentException("Warehouse document does not exist");
            }
            entryAuditService.applyAudit(existing);
            Long userId = currentUserService.requireCurrentUserId();
            warehouseDocumentMapper.updateAuditorByDocNumber(docNumber, existing.getAuditorUser(), userId);
            WarehouseDocument inbound = linkedInbound(docNumber);
            if (inbound != null) warehouseDocumentMapper.updateAuditorByDocNumber(inbound.getDocNumber(), existing.getAuditorUser(), userId);
        });
        return getByDocNumber(docNumber);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(String docNumber) {
        Runnable deleteAction = () -> {
            WarehouseDocument existing = warehouseDocumentMapper.selectIdentityForUpdate(docNumber);
            if (existing == null) return;
            requireEditable(existing);
            WarehouseDocument fullDoc = (entryAuditService != null || inventoryTransactionService != null)
                    ? warehouseDocumentMapper.selectByDocNumber(docNumber)
                    : null;
            if (entryAuditService != null) {
                entryAuditService.assertCanDelete(fullDoc != null ? fullDoc : existing);
            }
            Set<StockKey> affected = new LinkedHashSet<>();
            WarehouseDocument inbound = linkedInbound(docNumber);
            if (inventoryTransactionService != null) {
                Long parentWarehouseId = fullDoc != null && fullDoc.getWarehouseId() != null
                        ? fullDoc.getWarehouseId()
                        : existing.getWarehouseId();
                addStockKeys(affected, parentWarehouseId, warehouseDocumentItemMapper.selectByDocNumber(docNumber));
                if (inbound != null) {
                    addStockKeys(affected, inbound.getWarehouseId(), warehouseDocumentItemMapper.selectByDocNumber(inbound.getDocNumber()));
                }
            }
            if (inbound != null) {
                if (attachmentService != null) {
                    attachmentService.deleteByBusiness("warehouse-documents", inbound.getDocNumber());
                }
                deleteLinkedInbound(inbound);
            }
            if (attachmentService != null) {
                attachmentService.deleteByBusiness("warehouse-documents", docNumber);
            }
            deleteRecords(docNumber);
            assertNonNegativeStock(affected);
        };
        if (docNumber == null || !docNumber.matches("^(RKD|FHD|HBJ|HBX)[0-9]{9}$")) {
            codeSequenceService.withLock(sequenceKey(docNumber), deleteAction);
            return;
        }
        String annualPrefix = docNumber.substring(0, 7);
        codeSequenceService.deleteAndReclaimTail("warehouse_doc:" + annualPrefix, deleteAction,
                () -> warehouseDocumentMapper.selectMaxAnnualSequenceForUpdate(annualPrefix));
    }

    private String sequenceKey(String docNumber) {
        if (docNumber == null) throw new IllegalArgumentException("单据编号不能为空");
        return "warehouse_doc:" + (docNumber.matches("^(RKD|FHD|HBJ|HBX)[0-9]{9}$") ? docNumber.substring(0, 7) : "legacy:" + docNumber);
    }

    private void requireEditable(WarehouseDocument document) {
        if (document == null) throw new IllegalArgumentException("Warehouse document does not exist");
        if (WarehouseDocumentRules.TRANSFER_SOURCE.equals(document.getSourceRefType())) {
            throw new IllegalArgumentException("自动入库单由划拨单 " + document.getSourceRefId() + " 统一修改、审核和删除");
        }
    }

    private WarehouseDocument linkedInbound(String docNumber) {
        List<WarehouseDocument> linked = warehouseDocumentMapper.selectByTransferSource(docNumber);
        if (linked.size() > 1) throw new IllegalStateException("划拨单存在多张关联入库单，请先核对数据");
        return linked.isEmpty() ? null : linked.get(0);
    }

    private void deleteRecords(String docNumber) {
        inventoryTransactionMapper.deleteBySourceRef("WAREHOUSE_DOC", docNumber);
        warehouseDocumentItemMapper.deleteByDocNumber(docNumber);
        warehouseDocumentMapper.deleteByDocNumber(docNumber);
    }

    private void deleteLinkedInbound(WarehouseDocument inbound) {
        String prefix = inbound.getDocNumber().substring(0, 7);
        codeSequenceService.deleteAndReclaimTail(sequenceKey(inbound.getDocNumber()),
                () -> deleteRecords(inbound.getDocNumber()),
                () -> warehouseDocumentMapper.selectMaxAnnualSequenceForUpdate(prefix));
    }

    private void replaceItemsAndTransactions(WarehouseDocument document) {
        warehouseDocumentItemMapper.deleteByDocNumber(document.getDocNumber());
        inventoryTransactionMapper.deleteBySourceRef("WAREHOUSE_DOC", document.getDocNumber());
        persistItemsAndTransactions(document);
    }

    private Warehouse warehouseByCode(String code) {
        List<Warehouse> matches = warehouseMapper.selectAll().stream()
                .filter(item -> code.equalsIgnoreCase(item.getWarehouseCode() == null ? "" : item.getWarehouseCode().trim())
                        || code.equalsIgnoreCase(item.getWarehouseName() == null ? "" : item.getWarehouseName().trim())).toList();
        if (matches.size() != 1) throw new IllegalArgumentException("请核对已有仓库 " + code + "（仓库编码或名称需唯一匹配）");
        return matches.get(0);
    }

    private void touchCounterparty(WarehouseDocument document) {
        if (StringUtils.hasText(document.getCounterpartyCustomerId())) customerService.touchActivity(document.getCounterpartyCustomerId());
    }

    private String warehouseContact(Warehouse warehouse) {
        return ((warehouse.getContactName() == null ? "" : warehouse.getContactName()) + " "
                + (warehouse.getContactPhone() == null ? "" : warehouse.getContactPhone())).trim();
    }

    private void synchronizeTransfer(WarehouseDocument parent) {
        WarehouseDocument existingInbound = linkedInbound(parent.getDocNumber());
        if (!WarehouseDocumentRules.isTransfer(parent.getDocType())) {
            if (existingInbound != null) deleteLinkedInbound(existingInbound);
            return;
        }
        Warehouse destination = warehouseByCode(WarehouseDocumentRules.destinationWarehouse(parent.getDocType()));
        Warehouse source = warehouseMapper.selectById(parent.getWarehouseId());
        WarehouseDocument inbound = new WarehouseDocument();
        BeanUtils.copyProperties(parent, inbound);
        inbound.setDocNumber(existingInbound == null ? generateDocNumber("INBOUND_NOTE", parent.getDocDate()) : existingInbound.getDocNumber());
        inbound.setDocType("INBOUND_NOTE");
        inbound.setWarehouseId(destination.getId());
        inbound.setSourceRefType(WarehouseDocumentRules.TRANSFER_SOURCE);
        inbound.setSourceRefId(parent.getDocNumber());
        inbound.setCounterpartyCustomerId(null);
        inbound.setCounterpartyName(source.getWarehouseCode() + " - " + source.getWarehouseName());
        inbound.setCounterpartyAddress(source.getWarehouseAddress());
        inbound.setCounterpartyContact(warehouseContact(source));
        inbound.setItems(parent.getItems().stream().map(item -> {
            WarehouseDocumentItem copy = new WarehouseDocumentItem();
            BeanUtils.copyProperties(item, copy);
            copy.setId(null);
            copy.setUuid(UUID.randomUUID().toString());
            copy.setDocNumber(inbound.getDocNumber());
            return copy;
        }).toList());
        if (existingInbound == null) {
            warehouseDocumentMapper.insert(inbound);
            persistItemsAndTransactions(inbound);
        } else {
            codeSequenceService.withLock(sequenceKey(inbound.getDocNumber()), () -> {
                if (warehouseDocumentMapper.update(inbound) != 1) throw new IllegalStateException("关联入库单更新失败");
                replaceItemsAndTransactions(inbound);
            });
        }
    }

    private void validateAndHydrate(WarehouseDocument warehouseDocument, WarehouseDocument existing) {
        validateAndHydrate(warehouseDocument, existing, List.of());
    }

    private void validateAndHydrate(WarehouseDocument warehouseDocument, WarehouseDocument existing, List<WarehouseDocumentItem> oldItems) {
        if (!StringUtils.hasText(warehouseDocument.getDocType())) {
            throw new IllegalArgumentException("Document type is required");
        }
        warehouseDocument.setDocType(warehouseDocument.getDocType().trim().toUpperCase(Locale.ROOT));
        if (!ALLOWED_DOC_TYPES.contains(warehouseDocument.getDocType())) {
            throw new IllegalArgumentException("Document type is invalid");
        }
        if (!StringUtils.hasText(warehouseDocument.getBusinessCategory())) {
            throw new IllegalArgumentException("Business category is required");
        }
        warehouseDocument.setBusinessCategory(warehouseDocument.getBusinessCategory().trim().toUpperCase(Locale.ROOT));
        if (!ALLOWED_CATEGORIES.contains(warehouseDocument.getBusinessCategory())) {
            throw new IllegalArgumentException("Business category is invalid");
        }
        boolean legacyCategory = existing != null
                && Objects.equals(existing.getBusinessCategory(), warehouseDocument.getBusinessCategory())
                && !WarehouseDocumentRules.categories(existing.getDocType()).contains(existing.getBusinessCategory());
        if (!legacyCategory && !WarehouseDocumentRules.categories(warehouseDocument.getDocType()).contains(warehouseDocument.getBusinessCategory())) {
            throw new IllegalArgumentException("业务类别与单据类型不匹配");
        }
        if (legacyCategory) {
            warehouseDocument.setWarehouseId(existing.getWarehouseId());
            if (existing.getWarehouseId() == null || warehouseMapper.selectById(existing.getWarehouseId()) == null) {
                throw new IllegalArgumentException("请核对历史单据的仓库");
            }
        } else {
            warehouseDocument.setWarehouseId(warehouseByCode(WarehouseDocumentRules.sourceWarehouse(warehouseDocument.getBusinessCategory())).getId());
        }
        if ("CENTRALIZED_PURCHASE".equals(warehouseDocument.getBusinessCategory())) {
            warehouseDocument.setProjectId(null);
        } else if (PROJECT_REQUIRED_CATEGORIES.contains(warehouseDocument.getBusinessCategory()) && !legacyCategory) {
            if (!StringUtils.hasText(warehouseDocument.getProjectId())) {
                throw new IllegalArgumentException("该项目类或划拨单据必须选择关联项目");
            }
        }
        if (StringUtils.hasText(warehouseDocument.getProjectId())) {
            warehouseDocument.setProjectId(warehouseDocument.getProjectId().trim());
            if (projectMapper.selectByUuid(warehouseDocument.getProjectId()) == null) {
                throw new IllegalArgumentException("Project does not exist");
            }
        } else {
            warehouseDocument.setProjectId(null);
        }
        if (WarehouseDocumentRules.isTransfer(warehouseDocument.getDocType())) {
            Warehouse destination = warehouseByCode(WarehouseDocumentRules.destinationWarehouse(warehouseDocument.getDocType()));
            warehouseDocument.setCounterpartyCustomerId(null);
            warehouseDocument.setCounterpartyName(destination.getWarehouseCode() + " - " + destination.getWarehouseName());
            warehouseDocument.setCounterpartyAddress(destination.getWarehouseAddress());
            warehouseDocument.setCounterpartyContact(warehouseContact(destination));
        } else {
            if (!StringUtils.hasText(warehouseDocument.getCounterpartyCustomerId())) {
                throw new IllegalArgumentException("Counterparty customer is required");
            }
            Customer customer = customerMapper.selectByUuid(warehouseDocument.getCounterpartyCustomerId());
            if (customer == null) {
                throw new IllegalArgumentException("Counterparty customer does not exist");
            }
            CustomerType customerType = customer.getCustomerTypeId() != null
                    ? customerTypeMapper.selectById(customer.getCustomerTypeId())
                    : null;
            String customerTypeCode = customerType != null ? customerType.getTypeCode() : null;
            List<String> allowedCustomerTypeCodes = "INBOUND_NOTE".equals(warehouseDocument.getDocType())
                    ? INBOUND_ALLOWED_CUSTOMER_TYPE_CODES
                    : OUTBOUND_ALLOWED_CUSTOMER_TYPE_CODES;
            if (!StringUtils.hasText(customerTypeCode) || !allowedCustomerTypeCodes.contains(customerTypeCode)) {
                throw new IllegalArgumentException("Counterparty customer type is invalid for the selected document type");
            }
            warehouseDocument.setCounterpartyName(customer.getCustomerName());
            if (!StringUtils.hasText(warehouseDocument.getCounterpartyAddress())) {
                warehouseDocument.setCounterpartyAddress(
                        StringUtils.hasText(customer.getOfficeAddress())
                                ? customer.getOfficeAddress()
                                : customer.getRegisterAddress()
                );
            }
        }
        if (warehouseDocument.getDocDate() == null) {
            warehouseDocument.setDocDate(new Date());
        }
        if (existing == null || !StringUtils.hasText(warehouseDocument.getDocNumber())) {
            warehouseDocument.setDocNumber(generateDocNumber(warehouseDocument.getDocType(), warehouseDocument.getDocDate()));
        }
        if (warehouseDocument.getItems() == null || warehouseDocument.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one document item is required");
        }

        Set<String> previouslyUsedMaterialIds = oldItems.stream()
                .map(WarehouseDocumentItem::getMaterialId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int index = 0; index < warehouseDocument.getItems().size(); index++) {
            WarehouseDocumentItem item = warehouseDocument.getItems().get(index);
            hydrateItem(warehouseDocument, item, index + 1, existing == null, previouslyUsedMaterialIds);
            if (item.getAmount() != null) {
                totalAmount = totalAmount.add(item.getAmount());
            }
        }
        warehouseDocument.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
    }

    private void hydrateItem(
            WarehouseDocument warehouseDocument,
            WarehouseDocumentItem item,
            int lineNo,
            boolean creatingDoc,
            Set<String> previouslyUsedMaterialIds
    ) {
        if (!StringUtils.hasText(item.getMaterialId())) {
            throw new IllegalArgumentException("Material is required");
        }
        MaterialMaster materialMaster = materialMasterMapper.selectByUuid(item.getMaterialId());
        if (materialMaster == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        boolean allowInactiveHistorical = !creatingDoc && previouslyUsedMaterialIds.contains(item.getMaterialId());
        if (!allowInactiveHistorical && Boolean.FALSE.equals(materialMaster.getIsActive())) {
            throw new IllegalArgumentException("物料“" + materialMaster.getProductName() + "”已停用，不可用于新建单据");
        }
        if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        ProductBrand brand = materialMaster.getBrandId() != null ? productBrandMapper.selectById(materialMaster.getBrandId()) : null;
        item.setUuid(creatingDoc || !StringUtils.hasText(item.getUuid()) ? UUID.randomUUID().toString() : item.getUuid());
        item.setDocNumber(warehouseDocument.getDocNumber());
        item.setLineNo(lineNo);
        item.setMaterialCode(materialMaster.getMaterialCode());
        item.setMaterialName(materialMaster.getProductName());
        item.setModel(materialMaster.getProductModel());
        if (!StringUtils.hasText(item.getDisplayName())) {
            item.setDisplayName(materialMaster.getProductName());
        }
        if (!StringUtils.hasText(item.getDisplayModel())) {
            item.setDisplayModel(materialMaster.getProductModel());
        }
        if (!StringUtils.hasText(item.getDisplayName())) {
            throw new IllegalArgumentException("Display name is required");
        }
        if (!StringUtils.hasText(item.getDisplayModel())) {
            throw new IllegalArgumentException("Display model is required");
        }
        item.setBrand(brand != null ? brand.getName() : null);
        item.setUnit(materialMaster.getUnit());
        item.setAmount(calculateAmount(item));
        item.setCreateUser(warehouseDocument.getCreateUser());
        item.setUpdateUser(warehouseDocument.getUpdateUser());
    }

    private BigDecimal calculateAmount(WarehouseDocumentItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return null;
        }
        return item.getUnitPrice()
                .multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void persistItemsAndTransactions(WarehouseDocument warehouseDocument) {
        Long operatorId = warehouseDocument.getUpdateUser() != null ? warehouseDocument.getUpdateUser() : warehouseDocument.getCreateUser();
        for (WarehouseDocumentItem item : warehouseDocument.getItems()) {
            item.setDocNumber(warehouseDocument.getDocNumber());
            warehouseDocumentItemMapper.insert(item);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setMaterialId(item.getMaterialId());
            transaction.setCategory(warehouseDocument.getBusinessCategory());
            transaction.setProjectId(warehouseDocument.getProjectId());
            transaction.setWarehouseId(warehouseDocument.getWarehouseId());
            transaction.setTxnType("INBOUND_NOTE".equals(warehouseDocument.getDocType()) ? "IN" : "OUT");
            transaction.setTxnTime(warehouseDocument.getDocDate());
            transaction.setQuantity(item.getQuantity());
            transaction.setUnitPrice(item.getUnitPrice());
            transaction.setSourceRefType("WAREHOUSE_DOC");
            transaction.setSourceRefId(warehouseDocument.getDocNumber());
            transaction.setCreatedBy(operatorId);
            inventoryTransactionService.createForWarehouseDocument(transaction);
        }
    }

    private Set<StockKey> collectAffectedStockKeys(WarehouseDocument parent, WarehouseDocument oldInbound, WarehouseDocument currentInbound) {
        Set<StockKey> keys = new LinkedHashSet<>();
        addStockKeys(keys, parent.getWarehouseId(), parent.getItems());
        if (currentInbound != null) {
            addStockKeys(keys, currentInbound.getWarehouseId(), currentInbound.getItems());
        }
        if (oldInbound != null) {
            addStockKeys(keys, oldInbound.getWarehouseId(), oldInbound.getItems());
        }
        return keys;
    }

    private void addStockKeys(Set<StockKey> target, Long warehouseId, List<WarehouseDocumentItem> items) {
        if (warehouseId == null || items == null) {
            return;
        }
        for (WarehouseDocumentItem item : items) {
            if (item != null && StringUtils.hasText(item.getMaterialId())) {
                target.add(new StockKey(warehouseId, item.getMaterialId()));
            }
        }
    }

    private void assertNonNegativeStock(Set<StockKey> keys) {
        if (keys == null || keys.isEmpty() || inventoryTransactionService == null || inventoryTransactionMapper == null || isAllowNegativeStock()) {
            return;
        }
        for (StockKey key : keys) {
            BigDecimal balance = inventoryTransactionMapper.selectWarehouseMaterialBalance(key.warehouseId(), key.materialId());
            if (balance != null && balance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("库存不足：仓库中该物料可用库存不足，当前操作将导致负库存（剩余可用 " + balance.stripTrailingZeros().toPlainString() + "）");
            }
        }
    }

    private boolean isAllowNegativeStock() {
        if (systemConfigService == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(systemConfigService.getString(CONFIG_ALLOW_NEGATIVE_STOCK, "false")).trim());
    }

    private void attachItems(WarehouseDocument document) {
        if (document == null) {
            return;
        }
        document.setItems(warehouseDocumentItemMapper.selectByDocNumber(document.getDocNumber()));
    }

    private String generateDocNumber(String docType, Date docDate) {
        String prefix = WarehouseDocumentRules.numberPrefix(docType);
        String yearMonth = docDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DOC_NUMBER_YEAR_MONTH);
        String annualPrefix = prefix + yearMonth.substring(0, 4);
        int nextSequence = codeSequenceService.nextValue(
                "warehouse_doc:" + annualPrefix,
                () -> warehouseDocumentMapper.selectMaxAnnualSequenceForUpdate(annualPrefix),
                999,
                "Warehouse document annual sequence for " + annualPrefix
        );
        return prefix + yearMonth + String.format("%03d", nextSequence);
    }

    private record StockKey(Long warehouseId, String materialId) {
    }
}
