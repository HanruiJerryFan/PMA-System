package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.GoodsInventory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GoodsInventoryMapperTests {
    private JdbcTemplate jdbc;
    private SqlSessionFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE goods_transaction_dynamic_info (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, material_id VARCHAR(36), material_code VARCHAR(12),
                  material_name VARCHAR(200), model VARCHAR(200), brand VARCHAR(128), category VARCHAR(64),
                  txn_type VARCHAR(20), quantity DECIMAL(15,2), unit_price DECIMAL(15,4)
                )
                """);
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        String resource = "mapper/GoodsInventoryMapper.xml";
        try (InputStream input = new ClassPathResource(resource).getInputStream()) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void weightsPurchasePricesByQuantityAndGroupsByMaterialCode() {
        transaction("id-1", "M001", "PROJECT_PURCHASE", "IN", "10", "100");
        transaction("id-2", "M001", "CENTRALIZED_PURCHASE", "IN", "30", "200");
        transaction("id-2", "M001", "PROJECT_SALES", "OUT", "5", "9999");
        transaction("id-3", "M002", "PROJECT_PURCHASE", "IN", "2", "50");
        try (SqlSession session = factory.openSession(true)) {
            GoodsInventoryMapper mapper = session.getMapper(GoodsInventoryMapper.class);
            assertEquals(2, mapper.selectAll().size());
            GoodsInventory stock = mapper.selectByUuid("id-2");
            assertEquals(40D, stock.getInboundQuantity());
            assertEquals(5D, stock.getOutboundQuantity());
            assertEquals(35D, stock.getBalanceQuantity());
            assertDecimal("175", stock.getPurchaseAveragePrice());
            assertDecimal("6125.00", stock.getInventoryAmount());
        }
    }

    @Test
    void usesAllInboundQuantityAsDenominatorButOnlyPurchaseCostsAsNumerator() {
        transaction("id-1", "M001", "PROJECT_PURCHASE", "IN", "10", "100");
        transaction("id-1", "M001", "PROJECT_TRANSFER_TO_WAREHOUSE", "IN", "10", "9999");
        transaction("id-1", "M001", "WAREHOUSE_TRANSFER_TO_PROJECT", "OUT", "4", "9999");
        try (SqlSession session = factory.openSession(true)) {
            GoodsInventory stock = session.getMapper(GoodsInventoryMapper.class).selectByUuid("id-1");
            assertDecimal("50", stock.getPurchaseAveragePrice());
            assertDecimal("800.00", stock.getInventoryAmount());
        }
    }

    @Test
    void zeroInboundAndMissingPricesProduceZeroAmounts() {
        transaction("id-1", "M001", "PROJECT_SALES", "OUT", "3", "100");
        transaction("id-2", "M002", "PROJECT_PURCHASE", "IN", "10", null);
        transaction("id-3", "M003", "PROJECT_TRANSFER_TO_WAREHOUSE", "IN", "10", "100");
        try (SqlSession session = factory.openSession(true)) {
            for (GoodsInventory stock : session.getMapper(GoodsInventoryMapper.class).selectAll()) {
                assertDecimal("0", stock.getPurchaseAveragePrice());
                assertDecimal("0.00", stock.getInventoryAmount());
            }
        }
    }

    @Test
    void roundsInventoryAmountAfterMultiplyingTheUnroundedAverage() {
        transaction("id-1", "M001", "PROJECT_PURCHASE", "IN", "1", "1.0001");
        transaction("id-1", "M001", "PROJECT_PURCHASE", "IN", "2", "2.0001");
        transaction("id-1", "M001", "PROJECT_SALES", "OUT", "0.5", "100");
        try (SqlSession session = factory.openSession(true)) {
            GoodsInventory stock = session.getMapper(GoodsInventoryMapper.class).selectByUuid("id-1");
            assertEquals(2.5D, stock.getBalanceQuantity());
            assertDecimal("4.17", stock.getInventoryAmount());
        }
    }

    @Test
    void priceEditsAndTransactionDeletionsAreReflectedInTheNextSummary() {
        transaction("id-1", "M001", "PROJECT_PURCHASE", "IN", "10", "100");
        transaction("id-1", "M001", "CENTRALIZED_PURCHASE", "IN", "30", "200");
        try (SqlSession session = factory.openSession(true)) {
            GoodsInventoryMapper mapper = session.getMapper(GoodsInventoryMapper.class);
            assertDecimal("7000.00", mapper.selectByUuid("id-1").getInventoryAmount());
            jdbc.update("UPDATE goods_transaction_dynamic_info SET unit_price = 300 WHERE category = 'CENTRALIZED_PURCHASE'");
            session.clearCache();
            assertDecimal("250", mapper.selectByUuid("id-1").getPurchaseAveragePrice());
            jdbc.update("DELETE FROM goods_transaction_dynamic_info WHERE category = 'CENTRALIZED_PURCHASE'");
            session.clearCache();
            assertDecimal("1000.00", mapper.selectByUuid("id-1").getInventoryAmount());
            jdbc.update("DELETE FROM goods_transaction_dynamic_info");
            session.clearCache();
            assertEquals(0, mapper.selectAll().size());
            assertNull(mapper.selectByUuid("id-1"));
        }
    }

    private void transaction(String materialId, String code, String category, String type, String quantity, String price) {
        jdbc.update("""
                INSERT INTO goods_transaction_dynamic_info
                  (material_id, material_code, material_name, category, txn_type, quantity, unit_price)
                VALUES (?, ?, 'Test material', ?, ?, ?, ?)
                """, materialId, code, category, type, new BigDecimal(quantity), price == null ? null : new BigDecimal(price));
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
