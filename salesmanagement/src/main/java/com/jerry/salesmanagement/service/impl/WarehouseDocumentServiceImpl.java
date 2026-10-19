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
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.EntryAuditService;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import com.jerry.salesmanagement.service.WarehouseDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class WarehouseDocumentServiceImpl implements WarehouseDocumentService {

    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList("INBOUND_NOTE", "OUTBOUND_NOTE", "TRANSFER_JTX", "TRANSFER_XTJ");
    private static final List<String> ALLOWED_CATEGORIES = Arrays.asList(
            "PROJECT_PURCHASE",
            "PROJECT_SALES",
            "CENTRALIZED_PURCHASE",
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

    @Override
    public List<WarehouseDocument> getAll() {
        List<WarehouseDocument> documents = warehouseDocumentMapper.selectAll();
        documents.forEach(this::attachItems);
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
        validateAndHydrate(warehouseDocument, null);
        warehouseDocumentMapper.insert(warehouseDocument);
        persistItemsAndTransactions(warehouseDocument);
        synchronizeTransfer(warehouseDocument);
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
            if (!existing.getDocType().equals(warehouseDocument.getDocType())) {
                throw new IllegalArgumentException("单据类型创建后不可修改，请新建对应类型单据");
            }
            entryAuditService.applyUpdate(warehouseDocument, existing);
            warehouseDocument.setCreateUser(existing.getCreateUser());
            warehouseDocument.setUpdateUser(currentUserService.requireCurrentUserId());
            validateAndHydrate(warehouseDocument, existing);
            if (warehouseDocumentMapper.update(warehouseDocument) != 1) throw new IllegalStateException("单据更新失败");
            replaceItemsAndTransactions(warehouseDocument);
            synchronizeTransfer(warehouseDocument);
            touchCounterparty(warehouseDocument);
        });
        return getByDocNumber(warehouseDocument.getDocNumber());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WarehouseDocument audit(String docNumber) {
        codeSequenceService.withLock(sequenceKey(docNumber), () -> {
            WarehouseDocument existing = warehouseDocumentMapper.selectIdentityForUpdate(docNumber);
            requireEditable(existing);
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
            WarehouseDocument inbound = linkedInbound(docNumber);
            if (inbound != null) deleteLinkedInbound(inbound);
            deleteRecords(docNumber);
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
        if (!StringUtils.hasText(warehouseDocument.getDocType())) {
            throw new IllegalArgumentException("Document type is required");
        }
        warehouseDocument.setDocType(warehouseDocument.getDocType().trim().toUpperCase());
        if (!ALLOWED_DOC_TYPES.contains(warehouseDocument.getDocType())) {
            throw new IllegalArgumentException("Document type is invalid");
        }
        if (!StringUtils.hasText(warehouseDocument.getBusinessCategory())) {
            throw new IllegalArgumentException("Business category is required");
        }
        warehouseDocument.setBusinessCategory(warehouseDocument.getBusinessCategory().trim().toUpperCase());
        if (!ALLOWED_CATEGORIES.contains(warehouseDocument.getBusinessCategory())) {
            throw new IllegalArgumentException("Business category is invalid");
        }
        boolean legacyCategory = existing != null
                && existing.getBusinessCategory().equals(warehouseDocument.getBusinessCategory())
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
        if (StringUtils.hasText(warehouseDocument.getProjectId())
                && projectMapper.selectByUuid(warehouseDocument.getProjectId()) == null) {
            throw new IllegalArgumentException("Project does not exist");
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
        if (!StringUtils.hasText(warehouseDocument.getDocNumber())) {
            warehouseDocument.setDocNumber(generateDocNumber(warehouseDocument.getDocType(), warehouseDocument.getDocDate()));
        }
        if (warehouseDocument.getItems() == null || warehouseDocument.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one document item is required");
        }

        double totalAmount = 0D;
        for (int index = 0; index < warehouseDocument.getItems().size(); index++) {
            WarehouseDocumentItem item = warehouseDocument.getItems().get(index);
            hydrateItem(warehouseDocument, item, index + 1);
            totalAmount += item.getAmount() != null ? item.getAmount() : 0D;
        }
        warehouseDocument.setTotalAmount(totalAmount);
    }

    private void hydrateItem(WarehouseDocument warehouseDocument, WarehouseDocumentItem item, int lineNo) {
        if (!StringUtils.hasText(item.getMaterialId())) {
            throw new IllegalArgumentException("Material is required");
        }
        MaterialMaster materialMaster = materialMasterMapper.selectByUuid(item.getMaterialId());
        if (materialMaster == null) {
            throw new IllegalArgumentException("Material does not exist");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (item.getUnitPrice() != null && item.getUnitPrice() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        ProductBrand brand = materialMaster.getBrandId() != null ? productBrandMapper.selectById(materialMaster.getBrandId()) : null;
        item.setUuid(StringUtils.hasText(item.getUuid()) ? item.getUuid() : UUID.randomUUID().toString());
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
    }

    private Double calculateAmount(WarehouseDocumentItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return null;
        }
        return item.getUnitPrice() * item.getQuantity();
    }

    private void persistItemsAndTransactions(WarehouseDocument warehouseDocument) {
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
            transaction.setCreatedBy(warehouseDocument.getCreateUser());
            inventoryTransactionService.create(transaction);
        }
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
}
