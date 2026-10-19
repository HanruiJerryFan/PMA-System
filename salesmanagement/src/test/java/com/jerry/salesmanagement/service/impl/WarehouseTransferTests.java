package com.jerry.salesmanagement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerry.salesmanagement.mapper.*;
import com.jerry.salesmanagement.pojo.*;
import com.jerry.salesmanagement.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

class WarehouseTransferTests {
    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;
    private DataSourceTransactionManager transactionManager;
    private WarehouseDocumentServiceImpl target;
    private WarehouseDocumentService service;
    private WarehouseMapper warehouses;
    private Warehouse projectWarehouse;
    private Warehouse centralWarehouse;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        jdbc.execute("CREATE TABLE code_sequence (sequence_key VARCHAR(128) PRIMARY KEY, current_value INT, description VARCHAR(255), create_time TIMESTAMP, update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, real_name VARCHAR(64), username VARCHAR(64))");
        jdbc.execute("""
                CREATE TABLE warehouse_doc (
                  doc_number VARCHAR(50) PRIMARY KEY, doc_type VARCHAR(50), business_category VARCHAR(64),
                  project_id VARCHAR(36), warehouse_id BIGINT, counterparty_customer_id VARCHAR(36),
                  counterparty_name VARCHAR(200), counterparty_address VARCHAR(255), counterparty_contact VARCHAR(255),
                  contract_number VARCHAR(64), doc_date TIMESTAMP, total_amount DECIMAL(18,2),
                  source_ref_type VARCHAR(100), source_ref_id VARCHAR(100), entry_user BIGINT, auditor_user BIGINT,
                  remark TEXT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT
                )
                """);
        jdbc.execute("""
                CREATE TABLE warehouse_doc_item (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) UNIQUE, doc_number VARCHAR(50), line_no INT,
                  material_id VARCHAR(36), material_code VARCHAR(12), material_name VARCHAR(200), model VARCHAR(200),
                  display_name VARCHAR(200), display_model VARCHAR(200), brand VARCHAR(128), unit VARCHAR(32),
                  quantity DECIMAL(18,4), unit_price DECIMAL(18,4), amount DECIMAL(18,2), remark TEXT,
                  create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT
                )
                """);
        jdbc.execute("""
                CREATE TABLE goods_transaction_dynamic_info (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), material_id VARCHAR(36), material_code VARCHAR(12),
                  material_name VARCHAR(200), model VARCHAR(200), brand VARCHAR(128), category VARCHAR(64),
                  project_id VARCHAR(36), warehouse_id BIGINT, txn_type VARCHAR(20), txn_time TIMESTAMP,
                  quantity DECIMAL(15,2), unit_price DECIMAL(15,4), amount DECIMAL(15,2), source_ref_type VARCHAR(64),
                  source_ref_id VARCHAR(64), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT
                )
                """);
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[] {
                new ClassPathResource("mapper/CodeSequenceMapper.xml"), new ClassPathResource("mapper/WarehouseDocumentMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentItemMapper.xml"), new ClassPathResource("mapper/InventoryTransactionMapper.xml"),
                new ClassPathResource("mapper/GoodsInventoryMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());
        CodeSequenceServiceImpl sequenceTarget = new CodeSequenceServiceImpl();
        ReflectionTestUtils.setField(sequenceTarget, "mapper", sql.getMapper(CodeSequenceMapper.class));
        CodeSequenceService sequences = transactional(sequenceTarget, CodeSequenceService.class);

        projectWarehouse = warehouse(1L, "CK-XMCG");
        centralWarehouse = warehouse(2L, "CK-JZCG");
        warehouses = mock(WarehouseMapper.class);
        when(warehouses.selectAll()).thenReturn(List.of(projectWarehouse, centralWarehouse));
        when(warehouses.selectById(1L)).thenReturn(projectWarehouse);
        when(warehouses.selectById(2L)).thenReturn(centralWarehouse);
        ProjectMapper projects = mock(ProjectMapper.class);
        when(projects.selectByUuid("project")).thenReturn(new Project());
        MaterialMaster material = new MaterialMaster();
        material.setUuid("material"); material.setMaterialCode("M001"); material.setProductName("设备");
        material.setProductModel("型号"); material.setUnit("台");
        MaterialMasterMapper materials = mock(MaterialMasterMapper.class);
        when(materials.selectByUuid("material")).thenReturn(material);

        InventoryTransactionServiceImpl inventory = new InventoryTransactionServiceImpl();
        ReflectionTestUtils.setField(inventory, "inventoryTransactionMapper", sql.getMapper(InventoryTransactionMapper.class));
        ReflectionTestUtils.setField(inventory, "warehouseMapper", warehouses);
        ReflectionTestUtils.setField(inventory, "projectMapper", projects);
        ReflectionTestUtils.setField(inventory, "materialMasterMapper", materials);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        when(currentUser.requireCurrentUserId()).thenReturn(1L);
        EntryAuditServiceImpl audits = new EntryAuditServiceImpl();
        ReflectionTestUtils.setField(audits, "currentUserService", currentUser);

        CustomerMapper customers = mock(CustomerMapper.class);
        Customer supplier = new Customer(); supplier.setCustomerName("供应商"); supplier.setCustomerTypeId(3L);
        Customer receiver = new Customer(); receiver.setCustomerName("收货公司"); receiver.setCustomerTypeId(2L);
        when(customers.selectByUuid("supplier")).thenReturn(supplier);
        when(customers.selectByUuid("receiver")).thenReturn(receiver);
        CustomerTypeMapper customerTypes = mock(CustomerTypeMapper.class);
        CustomerType supplierType = new CustomerType(); supplierType.setTypeCode("3");
        CustomerType receiverType = new CustomerType(); receiverType.setTypeCode("2");
        when(customerTypes.selectById(3L)).thenReturn(supplierType);
        when(customerTypes.selectById(2L)).thenReturn(receiverType);

        target = new WarehouseDocumentServiceImpl();
        ReflectionTestUtils.setField(target, "warehouseDocumentMapper", sql.getMapper(WarehouseDocumentMapper.class));
        ReflectionTestUtils.setField(target, "warehouseDocumentItemMapper", sql.getMapper(WarehouseDocumentItemMapper.class));
        ReflectionTestUtils.setField(target, "inventoryTransactionMapper", sql.getMapper(InventoryTransactionMapper.class));
        ReflectionTestUtils.setField(target, "inventoryTransactionService", inventory);
        ReflectionTestUtils.setField(target, "warehouseMapper", warehouses);
        ReflectionTestUtils.setField(target, "projectMapper", projects);
        ReflectionTestUtils.setField(target, "materialMasterMapper", materials);
        ReflectionTestUtils.setField(target, "customerMapper", customers);
        ReflectionTestUtils.setField(target, "customerTypeMapper", customerTypes);
        ReflectionTestUtils.setField(target, "customerService", mock(CustomerService.class));
        ReflectionTestUtils.setField(target, "currentUserService", currentUser);
        ReflectionTestUtils.setField(target, "entryAuditService", audits);
        ReflectionTestUtils.setField(target, "codeSequenceService", sequences);
        service = transactional(target, WarehouseDocumentService.class);
    }

    @ParameterizedTest
    @CsvSource({"TRANSFER_JTX,WAREHOUSE_TRANSFER_TO_PROJECT,HBJ,2,1", "TRANSFER_XTJ,PROJECT_TRANSFER_TO_WAREHOUSE,HBX,1,2"})
    void createsOneOutboundAndOneLinkedInbound(String type, String category, String prefix, long source, long destination) {
        WarehouseDocument parent = service.create(document(type, category));
        WarehouseDocument inbound = inbound(parent);
        assertEquals(prefix + "202610001", parent.getDocNumber());
        assertEquals("RKD202610001", inbound.getDocNumber());
        assertEquals(source, parent.getWarehouseId());
        assertEquals(destination, inbound.getWarehouseId());
        assertEquals("WAREHOUSE_TRANSFER", inbound.getSourceRefType());
        assertEquals(parent.getDocNumber(), inbound.getSourceRefId());
        assertNull(parent.getCounterpartyCustomerId());
        assertNull(inbound.getCounterpartyCustomerId());
        assertEquals(parent.getTotalAmount(), inbound.getTotalAmount());
        assertNotEquals(parent.getItems().get(0).getUuid(), inbound.getItems().get(0).getUuid());
        assertEquals(2, count("warehouse_doc"));
        assertEquals(2, count("goods_transaction_dynamic_info"));
        assertEquals("OUT", jdbc.queryForObject("SELECT txn_type FROM goods_transaction_dynamic_info WHERE source_ref_id = ?", String.class, parent.getDocNumber()));
        assertEquals("IN", jdbc.queryForObject("SELECT txn_type FROM goods_transaction_dynamic_info WHERE source_ref_id = ?", String.class, inbound.getDocNumber()));
    }

    @ParameterizedTest
    @CsvSource({"INBOUND_NOTE,PROJECT_PURCHASE,1,supplier", "INBOUND_NOTE,CENTRALIZED_PURCHASE,2,supplier", "OUTBOUND_NOTE,PROJECT_SALES,1,receiver"})
    void ordinaryDocumentsUseTheirDesignatedWarehouse(String type, String category, long warehouseId, String customer) {
        WarehouseDocument document = document(type, category);
        document.setWarehouseId(999L);
        document.setCounterpartyCustomerId(customer);
        WarehouseDocument created = service.create(document);
        assertEquals(warehouseId, created.getWarehouseId());
        assertEquals(1, count("warehouse_doc"));
    }

    @Test
    void editsAndAuditsBothSidesWithoutDuplicatingInbound() {
        WarehouseDocument parent = service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT"));
        String inboundNumber = inbound(parent).getDocNumber();
        parent.getItems().get(0).setQuantity(6D);
        WarehouseDocument updated = service.update(parent);
        assertEquals(600D, updated.getTotalAmount());
        assertEquals(inboundNumber, inbound(updated).getDocNumber());
        assertEquals(6D, inbound(updated).getItems().get(0).getQuantity());
        assertEquals(2, count("warehouse_doc"));
        assertEquals(2, count("goods_transaction_dynamic_info"));
        service.audit(parent.getDocNumber());
        assertEquals(1L, service.getByDocNumber(inboundNumber).getAuditorUser());
        assertEquals(1L, service.getByDocNumber(parent.getDocNumber()).getAuditorUser());
    }

    @Test
    void generatedInboundCannotBeIndependentlyEditedAuditedOrDeleted() throws Exception {
        WarehouseDocument parent = service.create(document("TRANSFER_XTJ", "PROJECT_TRANSFER_TO_WAREHOUSE"));
        WarehouseDocument inbound = inbound(parent);
        assertThrows(IllegalArgumentException.class, () -> service.update(inbound));
        assertThrows(IllegalArgumentException.class, () -> service.audit(inbound.getDocNumber()));
        assertThrows(IllegalArgumentException.class, () -> service.delete(inbound.getDocNumber()));
        assertEquals(2, count("warehouse_doc"));
        WarehouseDocument forged = new ObjectMapper().readValue("{\"sourceRefType\":\"WAREHOUSE_TRANSFER\",\"sourceRefId\":\"fake\"}", WarehouseDocument.class);
        assertNull(forged.getSourceRefType());
        assertNull(forged.getSourceRefId());
    }

    @Test
    void deletingTransferRemovesBothSidesAndReclaimsBothNumberTails() {
        WarehouseDocument parent = service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT"));
        service.delete(parent.getDocNumber());
        assertEquals(0, count("warehouse_doc"));
        assertEquals(0, count("warehouse_doc_item"));
        assertEquals(0, count("goods_transaction_dynamic_info"));
        assertEquals(0, jdbc.queryForObject("SELECT SUM(current_value) FROM code_sequence", Integer.class));
        WarehouseDocument recreated = service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT"));
        assertEquals(parent.getDocNumber(), recreated.getDocNumber());
        assertEquals("RKD202610001", inbound(recreated).getDocNumber());
    }

    @Test
    void failedInboundInsertRollsBackOutboundInventoryAndSequences() {
        jdbc.execute("ALTER TABLE warehouse_doc ADD CONSTRAINT fail_destination CHECK (warehouse_id <> 2)");
        assertThrows(RuntimeException.class, () -> service.create(document("TRANSFER_XTJ", "PROJECT_TRANSFER_TO_WAREHOUSE")));
        assertEquals(0, count("warehouse_doc"));
        assertEquals(0, count("warehouse_doc_item"));
        assertEquals(0, count("goods_transaction_dynamic_info"));
        assertEquals(0, count("code_sequence"));
    }

    @Test
    void failedInboundUpdateRollsBackChangesToTheParentAndBothInventories() {
        WarehouseDocument parent = service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT"));
        String inboundNumber = inbound(parent).getDocNumber();
        WarehouseDocumentMapper mapper = mock(WarehouseDocumentMapper.class, delegatesTo(sql.getMapper(WarehouseDocumentMapper.class)));
        doAnswer(invocation -> {
            WarehouseDocument incoming = invocation.getArgument(0);
            if (inboundNumber.equals(incoming.getDocNumber())) throw new IllegalStateException("simulate failed inbound save");
            return sql.getMapper(WarehouseDocumentMapper.class).update(incoming);
        }).when(mapper).update(any());
        ReflectionTestUtils.setField(target, "warehouseDocumentMapper", mapper);
        parent.getItems().get(0).setQuantity(9D);
        assertThrows(IllegalStateException.class, () -> service.update(parent));
        assertEquals(2D, service.getByDocNumber(parent.getDocNumber()).getItems().get(0).getQuantity());
        assertEquals(2D, service.getByDocNumber(inboundNumber).getItems().get(0).getQuantity());
        assertEquals(2, count("goods_transaction_dynamic_info"));
    }

    @Test
    void missingWarehouseAndMismatchedCategoryAreRejectedBeforePosting() {
        when(warehouses.selectAll()).thenReturn(List.of(projectWarehouse));
        assertThrows(IllegalArgumentException.class, () -> service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT")));
        when(warehouses.selectAll()).thenReturn(List.of(projectWarehouse, centralWarehouse));
        assertThrows(IllegalArgumentException.class, () -> service.create(document("OUTBOUND_NOTE", "CENTRALIZED_PURCHASE")));
        assertEquals(0, count("warehouse_doc"));
        assertEquals(0, count("goods_transaction_dynamic_info"));
    }

    @Test
    void reusesExistingWarehouseIdsWhenTheReferencesAreWarehouseNames() {
        projectWarehouse.setWarehouseCode("WH-EXISTING-PROJECT"); projectWarehouse.setWarehouseName("CK-XMCG");
        centralWarehouse.setWarehouseCode("WH-EXISTING-CENTRAL"); centralWarehouse.setWarehouseName("CK-JZCG");
        WarehouseDocument parent = service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT"));
        assertEquals(2L, parent.getWarehouseId());
        assertEquals(1L, inbound(parent).getWarehouseId());
        verify(warehouses, never()).insert(any());
        when(warehouses.selectAll()).thenReturn(List.of(projectWarehouse, centralWarehouse, warehouse(3L, "CK-XMCG")));
        assertThrows(IllegalArgumentException.class, () -> service.create(document("TRANSFER_XTJ", "PROJECT_TRANSFER_TO_WAREHOUSE")));
        assertEquals(2, count("warehouse_doc"));
    }

    @Test
    void editingHistoricalCategoryPreservesItsWarehouseAndDoesNotCreateAnUnrequestedPair() {
        WarehouseDocument historical = document("OUTBOUND_NOTE", "WAREHOUSE_TRANSFER_TO_PROJECT");
        historical.setDocNumber("FHD202609001");
        historical.setWarehouseId(2L);
        historical.setCounterpartyCustomerId("receiver");
        historical.setCounterpartyName("收货公司");
        sql.getMapper(WarehouseDocumentMapper.class).insert(historical);
        WarehouseDocument updated = service.update(historical);
        assertEquals("WAREHOUSE_TRANSFER_TO_PROJECT", updated.getBusinessCategory());
        assertEquals(2L, updated.getWarehouseId());
        assertEquals(1, count("warehouse_doc"));
        assertEquals(1, count("goods_transaction_dynamic_info"));
    }

    @Test
    void documentTypeCannotBeChangedToInvalidateItsNumberAndTransferDirection() {
        WarehouseDocument parent = service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT"));
        parent.setDocType("TRANSFER_XTJ");
        parent.setBusinessCategory("PROJECT_TRANSFER_TO_WAREHOUSE");
        assertThrows(IllegalArgumentException.class, () -> service.update(parent));
        assertEquals("TRANSFER_JTX", service.getByDocNumber(parent.getDocNumber()).getDocType());
        assertEquals(1L, inbound(parent).getWarehouseId());
    }

    @Test
    void transfersLeaveGlobalStockAndPurchaseAverageUnchanged() {
        WarehouseDocument purchase = document("INBOUND_NOTE", "CENTRALIZED_PURCHASE");
        purchase.setCounterpartyCustomerId("supplier");
        service.create(purchase);
        service.create(document("TRANSFER_JTX", "WAREHOUSE_TRANSFER_TO_PROJECT"));
        service.create(document("TRANSFER_XTJ", "PROJECT_TRANSFER_TO_WAREHOUSE"));
        GoodsInventory stock = sql.getMapper(GoodsInventoryMapper.class).selectAll().get(0);
        assertEquals(2D, stock.getBalanceQuantity());
        assertEquals(0, new BigDecimal("100").compareTo(stock.getPurchaseAveragePrice()));
        assertEquals(0, new BigDecimal("200").compareTo(stock.getInventoryAmount()));
    }

    private WarehouseDocument inbound(WarehouseDocument parent) {
        String number = jdbc.queryForObject("SELECT doc_number FROM warehouse_doc WHERE source_ref_type = 'WAREHOUSE_TRANSFER' AND source_ref_id = ?", String.class, parent.getDocNumber());
        return service.getByDocNumber(number);
    }

    private Warehouse warehouse(Long id, String code) {
        Warehouse warehouse = new Warehouse(); warehouse.setId(id); warehouse.setWarehouseCode(code); warehouse.setWarehouseName(code);
        warehouse.setWarehouseAddress(code + "地址"); return warehouse;
    }

    private WarehouseDocument document(String type, String category) {
        WarehouseDocument document = new WarehouseDocument();
        document.setDocType(type); document.setBusinessCategory(category); document.setProjectId("project");
        document.setDocDate(new java.util.Date(Date.valueOf("2026-10-19").getTime()));
        WarehouseDocumentItem item = new WarehouseDocumentItem();
        item.setMaterialId("material"); item.setQuantity(2D); item.setUnitPrice(100D);
        document.setItems(new ArrayList<>(List.of(item)));
        return document;
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T transactional(Object target, Class<T> contract) {
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setInterfaces(contract);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        proxy.addAdvice(interceptor);
        return (T) proxy.getProxy();
    }
}
