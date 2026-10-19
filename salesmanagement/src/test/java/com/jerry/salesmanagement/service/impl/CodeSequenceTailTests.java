package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.*;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

class CodeSequenceTailTests {
    private static final String KEY = "warehouse_doc:RKD2026";
    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;
    private DataSourceTransactionManager transactionManager;
    private CodeSequenceService sequences;
    private WarehouseDocumentService warehouse;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        jdbc.execute("CREATE TABLE code_sequence (sequence_key VARCHAR(128) PRIMARY KEY, current_value INT, description VARCHAR(255), create_time TIMESTAMP, update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE warehouse_doc (doc_number VARCHAR(30) PRIMARY KEY, doc_type VARCHAR(50), source_ref_type VARCHAR(100), source_ref_id VARCHAR(100))");
        jdbc.execute("CREATE TABLE warehouse_doc_item (doc_number VARCHAR(30))");
        jdbc.execute("CREATE TABLE goods_transaction_dynamic_info (source_ref_type VARCHAR(64), source_ref_id VARCHAR(64))");
        jdbc.execute("CREATE TABLE customer_info (uuid VARCHAR(36) PRIMARY KEY, customer_code VARCHAR(9))");
        jdbc.execute("CREATE TABLE finance_voucher (uuid VARCHAR(36) PRIMARY KEY, voucher_no VARCHAR(10))");
        jdbc.execute("CREATE TABLE product_basic_info (uuid VARCHAR(36) PRIMARY KEY, category_code VARCHAR(2), subcategory_code VARCHAR(2), brand_code VARCHAR(2), band_code VARCHAR(3), sequence_no VARCHAR(9))");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[] {
                new ClassPathResource("mapper/CodeSequenceMapper.xml"), new ClassPathResource("mapper/WarehouseDocumentMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentItemMapper.xml"), new ClassPathResource("mapper/InventoryTransactionMapper.xml"),
                new ClassPathResource("mapper/CustomerMapper.xml"), new ClassPathResource("mapper/FinanceVoucherMapper.xml"),
                new ClassPathResource("mapper/MaterialMasterMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());
        CodeSequenceServiceImpl sequenceTarget = new CodeSequenceServiceImpl();
        ReflectionTestUtils.setField(sequenceTarget, "mapper", sql.getMapper(CodeSequenceMapper.class));
        sequences = transactional(sequenceTarget, CodeSequenceService.class);
        WarehouseDocumentServiceImpl warehouseTarget = new WarehouseDocumentServiceImpl();
        ReflectionTestUtils.setField(warehouseTarget, "codeSequenceService", sequences);
        ReflectionTestUtils.setField(warehouseTarget, "warehouseDocumentMapper", sql.getMapper(WarehouseDocumentMapper.class));
        ReflectionTestUtils.setField(warehouseTarget, "warehouseDocumentItemMapper", sql.getMapper(WarehouseDocumentItemMapper.class));
        ReflectionTestUtils.setField(warehouseTarget, "inventoryTransactionMapper", sql.getMapper(InventoryTransactionMapper.class));
        warehouse = transactional(warehouseTarget, WarehouseDocumentService.class);
    }

    @Test
    void deletingMiddleDoesNotReuseItButDeletingTailReclaimsAllTrailingHoles() {
        seedWarehouse();
        warehouse.delete("RKD202610004");
        assertEquals(5, counter(KEY));
        warehouse.delete("RKD202610005");
        assertEquals(3, counter(KEY));
        assertEquals(4, allocateWarehouse());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM warehouse_doc_item WHERE doc_number = 'RKD202610005'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM goods_transaction_dynamic_info WHERE source_ref_id = 'RKD202610005'", Integer.class));
        for (int index = 1; index <= 3; index++) warehouse.delete("RKD20261000" + index);
        assertEquals(0, counter(KEY));
        assertEquals(1, allocateWarehouse());
    }

    @Test
    void oldTrailingHolesAreReclaimedDuringAllocationAndEmptyScopeStartsAtOne() {
        seedWarehouse();
        jdbc.update("DELETE FROM warehouse_doc WHERE doc_number IN ('RKD202610004', 'RKD202610005')");
        assertEquals(4, allocateWarehouse());
        jdbc.update("DELETE FROM warehouse_doc");
        assertEquals(1, allocateWarehouse());
    }

    @Test
    void annualSequenceUsesNumericSuffixAcrossMonthsAndKeepsOtherScopesSeparate() {
        document("RKD202609009");
        document("RKD202610008");
        document("FHD202610020");
        document("RKD202710050");
        warehouse.delete("RKD202609009");
        assertEquals(8, counter(KEY));
        assertEquals(9, allocateWarehouse());
        assertEquals(20, sql.getMapper(WarehouseDocumentMapper.class).selectMaxAnnualSequenceForUpdate("FHD2026"));
        assertEquals(50, sql.getMapper(WarehouseDocumentMapper.class).selectMaxAnnualSequenceForUpdate("RKD2027"));
    }

    @Test
    void failedDeletionRollsBackCounterItemsAndInventoryTransactions() {
        seedWarehouse();
        jdbc.execute("CREATE TABLE warehouse_ref (doc_number VARCHAR(30) REFERENCES warehouse_doc(doc_number))");
        jdbc.update("INSERT INTO warehouse_ref VALUES ('RKD202610005')");
        assertThrows(RuntimeException.class, () -> warehouse.delete("RKD202610005"));
        assertEquals(5, counter(KEY));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM warehouse_doc_item WHERE doc_number = 'RKD202610005'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM goods_transaction_dynamic_info WHERE source_ref_id = 'RKD202610005'", Integer.class));
        jdbc.update("DELETE FROM warehouse_ref");
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
            warehouse.delete("RKD202610005");
            assertEquals(4, counter(KEY));
            throw new IllegalStateException("rollback after reclaim");
        }));
        assertEquals(5, counter(KEY));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM warehouse_doc WHERE doc_number = 'RKD202610005'", Integer.class));
    }

    @Test
    void customerAndVoucherDeletionReclaimOnlyTheirOwnPrefixOrMonth() {
        Customer customer = new Customer();
        customer.setCustomerCode("110110005");
        jdbc.update("INSERT INTO customer_info VALUES ('c1', '110110003'), ('c2', '110110005'), ('c3', '110120020')");
        CustomerMapper customerMapper = mock(CustomerMapper.class, delegatesTo(sql.getMapper(CustomerMapper.class)));
        doReturn(customer).when(customerMapper).selectByUuid("c2");
        CustomerServiceImpl customerTarget = new CustomerServiceImpl();
        ReflectionTestUtils.setField(customerTarget, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(customerTarget, "codeSequenceService", sequences);
        transactional(customerTarget, CustomerService.class).delete("c2");
        assertEquals(3, counter("customer:11011"));

        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setVoucherNo("2026101705");
        jdbc.update("INSERT INTO finance_voucher VALUES ('v1', '2026100103'), ('v2', '2026101705'), ('v3', '2026110109')");
        FinanceVoucherMapper vouchers = mock(FinanceVoucherMapper.class, delegatesTo(sql.getMapper(FinanceVoucherMapper.class)));
        doReturn(voucher).when(vouchers).selectByUuid("v2");
        FinanceVoucherServiceImpl voucherTarget = new FinanceVoucherServiceImpl();
        ReflectionTestUtils.setField(voucherTarget, "mapper", vouchers);
        ReflectionTestUtils.setField(voucherTarget, "codeSequenceService", sequences);
        transactional(voucherTarget, FinanceVoucherService.class).delete("v2");
        assertEquals(3, counter("finance_voucher:202610"));
        assertEquals(4, sequences.nextValue("finance_voucher:202610",
                () -> vouchers.selectMaxVoucherSequenceByMonthPrefixForUpdate("202610"), 99, "test"));
    }

    @Test
    void materialReclaimUsesNumericSequenceAndRespectsBandScopeAndSequenceLength() {
        jdbc.update("INSERT INTO product_basic_info VALUES ('m1','01','02','03','A','003'), ('m2','01','02','03','B','005'), ('m3','01','02','03','A','009'), ('m4','01','02','03','A','0020')");
        MaterialMaster material = new MaterialMaster();
        material.setCategoryCode("01"); material.setSubcategoryCode("02"); material.setBrandCode("03");
        material.setBandCode("A"); material.setSequenceNo("009");
        MaterialMasterMapper materials = mock(MaterialMasterMapper.class, delegatesTo(sql.getMapper(MaterialMasterMapper.class)));
        doReturn(material).when(materials).selectByUuid("m3");
        MaterialMasterServiceImpl materialTarget = new MaterialMasterServiceImpl();
        ReflectionTestUtils.setField(materialTarget, "materialMasterMapper", materials);
        ReflectionTestUtils.setField(materialTarget, "codeSequenceService", sequences);
        SystemConfigService config = mock(SystemConfigService.class);
        ReflectionTestUtils.setField(materialTarget, "systemConfigService", config);
        transactional(materialTarget, MaterialMasterService.class).delete("m3");
        assertEquals(5, counter("material:CATEGORY_SUBCATEGORY_BRAND:3:010203"));
        assertEquals(3, materials.selectMaxMaterialSequenceForUpdate("01", "02", "03", "A", 3));
        assertEquals(5, materials.selectMaxMaterialSequenceForUpdate("01", "02", "03", null, 3));
        when(config.getString("material.code.sequence.scope", "CATEGORY_SUBCATEGORY_BRAND")).thenReturn("CATEGORY_SUBCATEGORY_BRAND_BAND");
        material.setSequenceNo("003");
        doReturn(material).when(materials).selectByUuid("m1");
        transactional(materialTarget, MaterialMasterService.class).delete("m1");
        assertEquals(0, counter("material:CATEGORY_SUBCATEGORY_BRAND_BAND:3:010203A"));
        assertEquals(5, counter("material:CATEGORY_SUBCATEGORY_BRAND:3:010203"));
    }

    @Test
    void exhaustedRangeRejectsAllocationAndDoesNotAdvanceCounter() {
        jdbc.update("INSERT INTO finance_voucher VALUES ('v1', '2026101799')");
        jdbc.update("INSERT INTO code_sequence(sequence_key, current_value) VALUES ('finance_voucher:202610', 99)");
        assertThrows(IllegalArgumentException.class, () -> sequences.nextValue("finance_voucher:202610",
                () -> sql.getMapper(FinanceVoucherMapper.class).selectMaxVoucherSequenceByMonthPrefixForUpdate("202610"), 99, "test"));
        assertEquals(99, counter("finance_voucher:202610"));
    }

    @Test
    void concurrentCreationCommitsBeforeDeleteCanReclaimAndItsNumberIsPreserved() throws Exception {
        seedWarehouse();
        CountDownLatch allocated = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        CountDownLatch deleting = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> creation = workers.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                int next = allocateWarehouse();
                document("RKD20261000" + next);
                allocated.countDown();
                await(commit);
                return null;
            }));
            assertTrue(allocated.await(5, TimeUnit.SECONDS));
            Future<?> deletion = workers.submit(() -> { deleting.countDown(); warehouse.delete("RKD202610005"); });
            assertTrue(deleting.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> deletion.get(100, TimeUnit.MILLISECONDS));
            commit.countDown();
            creation.get(5, TimeUnit.SECONDS);
            deletion.get(5, TimeUnit.SECONDS);
            assertEquals(6, counter(KEY));
            assertEquals(7, allocateWarehouse());
        } finally {
            commit.countDown(); workers.shutdownNow();
        }
    }

    @Test
    void concurrentAllocationWaitsForDeletionAndThenUsesReclaimedNumber() throws Exception {
        seedWarehouse();
        CountDownLatch deleted = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> deletion = workers.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                warehouse.delete("RKD202610005");
                deleted.countDown();
                await(commit);
                return null;
            }));
            assertTrue(deleted.await(5, TimeUnit.SECONDS));
            Future<Integer> creation = workers.submit(this::allocateWarehouse);
            assertThrows(TimeoutException.class, () -> creation.get(100, TimeUnit.MILLISECONDS));
            commit.countDown();
            deletion.get(5, TimeUnit.SECONDS);
            assertEquals(5, creation.get(5, TimeUnit.SECONDS));
        } finally {
            commit.countDown(); workers.shutdownNow();
        }
    }

    private void seedWarehouse() {
        jdbc.update("INSERT INTO code_sequence(sequence_key, current_value) VALUES (?, 5)", KEY);
        for (int index = 1; index <= 5; index++) document("RKD20261000" + index);
    }

    private void document(String number) {
        jdbc.update("INSERT INTO warehouse_doc(doc_number) VALUES (?)", number);
        jdbc.update("INSERT INTO warehouse_doc_item VALUES (?)", number);
        jdbc.update("INSERT INTO goods_transaction_dynamic_info VALUES ('WAREHOUSE_DOC', ?)", number);
    }

    private int counter(String key) {
        return jdbc.queryForObject("SELECT current_value FROM code_sequence WHERE sequence_key = ?", Integer.class, key);
    }

    private int allocateWarehouse() {
        return sequences.nextValue(KEY, () -> sql.getMapper(WarehouseDocumentMapper.class).selectMaxAnnualSequenceForUpdate("RKD2026"), 999, "test");
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for test transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
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
