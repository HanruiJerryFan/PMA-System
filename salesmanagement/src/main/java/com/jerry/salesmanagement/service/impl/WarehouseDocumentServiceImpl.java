package com.jerry.salesmanagement.service.impl;

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
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.EntryAuditService;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import com.jerry.salesmanagement.service.WarehouseDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class WarehouseDocumentServiceImpl implements WarehouseDocumentService {

    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList("INBOUND_NOTE", "OUTBOUND_NOTE");
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
    @Transactional
    public WarehouseDocument create(WarehouseDocument warehouseDocument) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        entryAuditService.applyCreate(warehouseDocument);
        warehouseDocument.setCreateUser(currentUserId);
        validateAndHydrate(warehouseDocument);
        warehouseDocumentMapper.insert(warehouseDocument);
        persistItemsAndTransactions(warehouseDocument);
        customerService.touchActivity(warehouseDocument.getCounterpartyCustomerId());
        return getByDocNumber(warehouseDocument.getDocNumber());
    }

    @Override
    @Transactional
    public WarehouseDocument update(WarehouseDocument warehouseDocument) {
        WarehouseDocument existing = warehouseDocumentMapper.selectByDocNumber(warehouseDocument.getDocNumber());
        if (existing == null) {
            throw new IllegalArgumentException("Warehouse document does not exist");
        }
        entryAuditService.applyUpdate(warehouseDocument, existing);
        warehouseDocument.setUpdateUser(currentUserService.requireCurrentUserId());
        validateAndHydrate(warehouseDocument);
        warehouseDocumentMapper.update(warehouseDocument);
        warehouseDocumentItemMapper.deleteByDocNumber(warehouseDocument.getDocNumber());
        inventoryTransactionMapper.deleteBySourceRef("WAREHOUSE_DOC", warehouseDocument.getDocNumber());
        persistItemsAndTransactions(warehouseDocument);
        customerService.touchActivity(warehouseDocument.getCounterpartyCustomerId());
        return getByDocNumber(warehouseDocument.getDocNumber());
    }

    @Override
    @Transactional
    public WarehouseDocument audit(String docNumber) {
        WarehouseDocument existing = warehouseDocumentMapper.selectByDocNumber(docNumber);
        if (existing == null) {
            throw new IllegalArgumentException("Warehouse document does not exist");
        }
        entryAuditService.applyAudit(existing);
        warehouseDocumentMapper.updateAuditorByDocNumber(
                docNumber,
                existing.getAuditorUser(),
                currentUserService.requireCurrentUserId()
        );
        return getByDocNumber(docNumber);
    }

    @Override
    @Transactional
    public void delete(String docNumber) {
        inventoryTransactionMapper.deleteBySourceRef("WAREHOUSE_DOC", docNumber);
        warehouseDocumentItemMapper.deleteByDocNumber(docNumber);
        warehouseDocumentMapper.deleteByDocNumber(docNumber);
    }

    private void validateAndHydrate(WarehouseDocument warehouseDocument) {
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
        if (warehouseDocument.getWarehouseId() == null) {
            List<com.jerry.salesmanagement.pojo.Warehouse> warehouses = warehouseMapper.selectAll();
            if (warehouses.size() == 1) {
                warehouseDocument.setWarehouseId(warehouses.get(0).getId());
            }
        }
        if (warehouseDocument.getWarehouseId() == null || warehouseMapper.selectById(warehouseDocument.getWarehouseId()) == null) {
            throw new IllegalArgumentException("Warehouse is required");
        }
        if (StringUtils.hasText(warehouseDocument.getProjectId())
                && projectMapper.selectByUuid(warehouseDocument.getProjectId()) == null) {
            throw new IllegalArgumentException("Project does not exist");
        }
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
        String prefix = "INBOUND_NOTE".equals(docType) ? "RKD" : "FHD";
        String yearMonth = docDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DOC_NUMBER_YEAR_MONTH);
        String annualPrefix = prefix + yearMonth.substring(0, 4);
        Integer maxSequence = warehouseDocumentMapper.selectMaxAnnualSequence(annualPrefix);
        int nextSequence = codeSequenceService.nextValue(
                "warehouse_doc:" + annualPrefix,
                maxSequence == null ? 0 : maxSequence,
                999,
                "Warehouse document annual sequence for " + annualPrefix
        );
        return prefix + yearMonth + String.format("%03d", nextSequence);
    }
}
