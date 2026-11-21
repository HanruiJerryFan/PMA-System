package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.common.PdfExportSupport;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductHistoryPriceMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentItemMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductHistoryPrice;
import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonetaryPrecisionTests {

    @Test
    void inventoryTransactionComputesExactBigDecimalAmountWithHalfUpRounding() {
        InventoryTransactionServiceImpl service = new InventoryTransactionServiceImpl();
        WarehouseMapper warehouseMapper = mock(WarehouseMapper.class);
        MaterialMasterMapper materialMasterMapper = mock(MaterialMasterMapper.class);
        InventoryTransactionMapper mapper = mock(InventoryTransactionMapper.class);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

        MaterialMaster material = new MaterialMaster();
        material.setUuid("mat-1");
        material.setMaterialCode("010101001AAA");
        material.setProductName("Precision Material");
        material.setProductModel("PM-1");
        when(materialMasterMapper.selectByUuid("mat-1")).thenReturn(material);

        ReflectionTestUtils.setField(service, "warehouseMapper", warehouseMapper);
        ReflectionTestUtils.setField(service, "materialMasterMapper", materialMasterMapper);
        ReflectionTestUtils.setField(service, "inventoryTransactionMapper", mapper);

        InventoryTransaction txn1 = baseTransaction();
        txn1.setQuantity(new BigDecimal("3"));
        txn1.setUnitPrice(new BigDecimal("0.1"));
        service.create(txn1);
        assertEquals(new BigDecimal("0.30"), txn1.getAmount());

        InventoryTransaction txn2 = baseTransaction();
        txn2.setQuantity(new BigDecimal("3"));
        txn2.setUnitPrice(new BigDecimal("19.99"));
        service.create(txn2);
        assertEquals(new BigDecimal("59.97"), txn2.getAmount());

        InventoryTransaction txn3 = baseTransaction();
        txn3.setQuantity(new BigDecimal("1.005"));
        txn3.setUnitPrice(new BigDecimal("2"));
        service.create(txn3);
        assertEquals(new BigDecimal("2.01"), txn3.getAmount());
    }

    @Test
    void warehouseDocumentSumsLineItemsWithoutBinaryFloatingPointDrift() {
        WarehouseDocumentServiceImpl service = new WarehouseDocumentServiceImpl();

        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        EntryAuditServiceImpl auditService = new EntryAuditServiceImpl();
        ReflectionTestUtils.setField(auditService, "currentUserService", currentUserService);

        WarehouseMapper warehouseMapper = mock(WarehouseMapper.class);
        Warehouse projectWarehouse = new Warehouse();
        projectWarehouse.setId(1L);
        projectWarehouse.setWarehouseCode("CK-XMCG");
        Warehouse centralWarehouse = new Warehouse();
        centralWarehouse.setId(2L);
        centralWarehouse.setWarehouseCode("CK-JZCG");
        when(warehouseMapper.selectAll()).thenReturn(List.of(projectWarehouse, centralWarehouse));

        ProjectMapper projectMapper = mock(ProjectMapper.class);
        when(projectMapper.selectByUuid("proj-1")).thenReturn(new com.jerry.salesmanagement.pojo.Project());

        CustomerMapper customerMapper = mock(CustomerMapper.class);
        Customer supplier = new Customer();
        supplier.setCustomerName("Supplier");
        supplier.setCustomerTypeId(3L);
        when(customerMapper.selectByUuid("cust-1")).thenReturn(supplier);

        CustomerTypeMapper customerTypeMapper = mock(CustomerTypeMapper.class);
        CustomerType supplierType = new CustomerType();
        supplierType.setTypeCode("3");
        when(customerTypeMapper.selectById(3L)).thenReturn(supplierType);

        MaterialMasterMapper materialMasterMapper = mock(MaterialMasterMapper.class);
        MaterialMaster material = new MaterialMaster();
        material.setUuid("mat-1");
        material.setMaterialCode("010101001AAA");
        material.setProductName("Item");
        material.setProductModel("Model");
        material.setUnit("台");
        when(materialMasterMapper.selectByUuid("mat-1")).thenReturn(material);

        CodeSequenceService sequenceService = mock(CodeSequenceService.class);
        when(sequenceService.nextValue(anyString(), any(), anyInt(), anyString())).thenReturn(1);

        WarehouseDocumentMapper warehouseDocumentMapper = mock(WarehouseDocumentMapper.class);
        WarehouseDocumentItemMapper warehouseDocumentItemMapper = mock(WarehouseDocumentItemMapper.class);
        ReflectionTestUtils.setField(service, "currentUserService", currentUserService);
        ReflectionTestUtils.setField(service, "entryAuditService", auditService);
        ReflectionTestUtils.setField(service, "warehouseMapper", warehouseMapper);
        ReflectionTestUtils.setField(service, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "customerTypeMapper", customerTypeMapper);
        ReflectionTestUtils.setField(service, "materialMasterMapper", materialMasterMapper);
        ReflectionTestUtils.setField(service, "codeSequenceService", sequenceService);
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", warehouseDocumentMapper);
        ReflectionTestUtils.setField(service, "warehouseDocumentItemMapper", warehouseDocumentItemMapper);
        ReflectionTestUtils.setField(service, "inventoryTransactionService", mock(InventoryTransactionService.class));
        ReflectionTestUtils.setField(service, "customerService", mock(CustomerService.class));

        WarehouseDocumentItem item1 = new WarehouseDocumentItem();
        item1.setMaterialId("mat-1");
        item1.setQuantity(new BigDecimal("3"));
        item1.setUnitPrice(new BigDecimal("0.1"));

        WarehouseDocumentItem item2 = new WarehouseDocumentItem();
        item2.setMaterialId("mat-1");
        item2.setQuantity(new BigDecimal("3"));
        item2.setUnitPrice(new BigDecimal("19.99"));

        WarehouseDocument doc = new WarehouseDocument();
        doc.setDocType("INBOUND_NOTE");
        doc.setBusinessCategory("PROJECT_PURCHASE");
        doc.setProjectId("proj-1");
        doc.setCounterpartyCustomerId("cust-1");
        doc.setDocDate(new Date());
        doc.setItems(List.of(item1, item2));

        when(warehouseDocumentMapper.selectByDocNumber(anyString())).thenReturn(doc);
        when(warehouseDocumentItemMapper.selectByDocNumber(anyString())).thenReturn(List.of(item1, item2));

        WarehouseDocument created = service.create(doc);
        assertEquals(new BigDecimal("0.30"), created.getItems().get(0).getAmount());
        assertEquals(new BigDecimal("59.97"), created.getItems().get(1).getAmount());
        assertEquals(new BigDecimal("60.27"), created.getTotalAmount());
    }

    @Test
    void productHistoryPriceRejectsNegativeBigDecimalAndAcceptsZero() {
        ProductHistoryPriceServiceImpl service = new ProductHistoryPriceServiceImpl();
        MaterialMasterMapper materialMasterMapper = mock(MaterialMasterMapper.class);
        when(materialMasterMapper.selectByUuid("mat-1")).thenReturn(new MaterialMaster());
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        Customer supplier = new Customer();
        supplier.setCustomerTypeId(3L);
        when(customerMapper.selectByUuid("cust-1")).thenReturn(supplier);
        CustomerTypeMapper customerTypeMapper = mock(CustomerTypeMapper.class);
        CustomerType supplierType = new CustomerType();
        supplierType.setTypeName("供应商");
        when(customerTypeMapper.selectById(3L)).thenReturn(supplierType);

        ReflectionTestUtils.setField(service, "materialMasterMapper", materialMasterMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "customerTypeMapper", customerTypeMapper);
        ReflectionTestUtils.setField(service, "mapper", mock(ProductHistoryPriceMapper.class));
        ReflectionTestUtils.setField(service, "customerService", mock(CustomerService.class));

        ProductHistoryPrice negative = new ProductHistoryPrice();
        negative.setProductBasicInfoUuid("mat-1");
        negative.setSupplierCustomerId("cust-1");
        negative.setReferencePrice(new BigDecimal("-0.01"));
        assertThrows(IllegalArgumentException.class, () -> service.create(negative));

        ProductHistoryPrice zero = new ProductHistoryPrice();
        zero.setProductBasicInfoUuid("mat-1");
        zero.setSupplierCustomerId("cust-1");
        zero.setReferencePrice(BigDecimal.ZERO);
        assertDoesNotThrow(() -> service.create(zero));
    }

    @Test
    void pdfSupportAndWarehousePdfHelpersFormatBigDecimalAccurately() {
        TestPdfSupport support = new TestPdfSupport();
        assertEquals("0.30", support.money(new BigDecimal("0.3")));
        assertEquals("19.99", support.money(new BigDecimal("19.994")));
        assertEquals("人民币壹拾玖元玖角玖分", support.rmb(new BigDecimal("19.99")));
        assertEquals("人民币零元整", support.rmb(BigDecimal.ZERO));

        WarehouseDocumentPdfExportServiceImpl pdfService = new WarehouseDocumentPdfExportServiceImpl(null, "");
        assertEquals("2.5", ReflectionTestUtils.invokeMethod(pdfService, "trimTrailingZero", new BigDecimal("2.5000")));
        assertEquals("0", ReflectionTestUtils.invokeMethod(pdfService, "trimTrailingZero", new BigDecimal("0.0000")));
        assertEquals("12.3456", ReflectionTestUtils.invokeMethod(pdfService, "formatUnitPrice", new BigDecimal("12.34564")));

        WarehouseDocument doc = new WarehouseDocument();
        WarehouseDocumentItem item = new WarehouseDocumentItem();
        item.setQuantity(new BigDecimal("3"));
        item.setUnitPrice(new BigDecimal("0.1"));
        doc.setItems(List.of(item));
        BigDecimal resolved = ReflectionTestUtils.invokeMethod(pdfService, "resolveTotalAmount", doc);
        assertEquals(new BigDecimal("0.30"), resolved);
    }

    private InventoryTransaction baseTransaction() {
        InventoryTransaction txn = new InventoryTransaction();
        txn.setMaterialId("mat-1");
        txn.setWarehouseId(1L);
        txn.setCategory("PROJECT_PURCHASE");
        txn.setTxnType("IN");
        return txn;
    }

    private static class TestPdfSupport extends PdfExportSupport {
        TestPdfSupport() {
            super(null, "");
        }

        String money(BigDecimal value) {
            return formatMoney(value);
        }

        String rmb(BigDecimal value) {
            return toChineseUppercaseRmb(value);
        }
    }
}
