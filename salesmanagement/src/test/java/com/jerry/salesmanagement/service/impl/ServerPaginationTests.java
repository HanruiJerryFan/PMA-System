package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.AuditTrailMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.LoginRecordMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.pojo.AuditTrail;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.LoginRecord;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import com.jerry.salesmanagement.service.SysUserService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ServerPaginationTests {

    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);

        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, username VARCHAR(50))");
        jdbc.execute("CREATE TABLE login_record (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, login_start_time TIMESTAMP, login_end_time TIMESTAMP, duration_seconds BIGINT, login_ip VARCHAR(64), user_agent VARCHAR(512), last_seen_at TIMESTAMP, logout_reason VARCHAR(32), created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE audit_trail (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, module VARCHAR(64), action VARCHAR(64), target_type VARCHAR(64), target_id VARCHAR(128), detail TEXT, occurred_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE goods_transaction_dynamic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), material_id VARCHAR(36), material_code VARCHAR(12), material_name VARCHAR(200), model VARCHAR(200), brand VARCHAR(128), category VARCHAR(64), project_id VARCHAR(36), warehouse_id BIGINT, txn_type VARCHAR(20), txn_time TIMESTAMP, quantity DECIMAL(15,2), unit_price DECIMAL(15,4), amount DECIMAL(15,2), source_ref_type VARCHAR(64), source_ref_id VARCHAR(64), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE customer_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), customer_code VARCHAR(9), customer_name VARCHAR(100), region_id BIGINT, register_address VARCHAR(255), office_address VARCHAR(255), tax_identification_number VARCHAR(18), customer_type_id BIGINT, bank_name VARCHAR(100), bank_account VARCHAR(50), industry VARCHAR(100), remark TEXT, activity_status VARCHAR(20), last_active_at TIMESTAMP, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE product_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), brand_code VARCHAR(2), category_code VARCHAR(2), subcategory_code VARCHAR(2), sequence_no VARCHAR(3), band_code VARCHAR(3), material_code VARCHAR(12), category_id BIGINT, subcategory_id BIGINT, product_name VARCHAR(200), product_model VARCHAR(200), brand_id BIGINT, manufacturer VARCHAR(200), unit VARCHAR(50), frequency VARCHAR(100), specification TEXT, other_note TEXT, is_active BOOLEAN, created_at TIMESTAMP, created_by BIGINT, updated_at TIMESTAMP, updated_by BIGINT)");
        jdbc.execute("CREATE TABLE project_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_number VARCHAR(10), project_name VARCHAR(200), customer_id VARCHAR(36), customer_uuid VARCHAR(36), manager_id BIGINT, participant_1_user_id BIGINT, participant_2_user_id BIGINT, participant_3_user_id BIGINT, region_id BIGINT, project_type_id BIGINT, warranty_until DATE, plan_start_time TIMESTAMP, plan_end_time TIMESTAMP, sales_contract_attachment_uuid VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE stage (id BIGINT PRIMARY KEY, sort_order INT, description VARCHAR(128))");
        jdbc.execute("CREATE TABLE project_status_history (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(64), project_id VARCHAR(64), stage_id BIGINT, created_at TIMESTAMP, created_by BIGINT)");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/LoginRecordMapper.xml"),
                new ClassPathResource("mapper/AuditTrailMapper.xml"),
                new ClassPathResource("mapper/InventoryTransactionMapper.xml"),
                new ClassPathResource("mapper/CustomerMapper.xml"),
                new ClassPathResource("mapper/MaterialMasterMapper.xml"),
                new ClassPathResource("mapper/ProjectMapper.xml"),
                new ClassPathResource("mapper/ProjectStatusHistoryMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());
    }

    @Test
    void pageResultClampsBoundsAndSkipsFetcherWhenOffsetBeyondTotal() {
        assertEquals(1, PageResult.normalizePage(0));
        assertEquals(1, PageResult.normalizePage(-5));
        assertEquals(10, PageResult.normalizePageSize(0));
        assertEquals(200, PageResult.normalizePageSize(9999));

        boolean[] fetched = {false};
        PageResult<String> emptyTail = PageResult.paginate(5, 10, () -> 3L, (offset, limit) -> {
            fetched[0] = true;
            return java.util.List.of("unexpected");
        });
        assertFalse(fetched[0]);
        assertEquals(3L, emptyTail.getTotal());
        assertTrue(emptyTail.getRecords().isEmpty());
        assertEquals(5, emptyTail.getPage());
        assertEquals(10, emptyTail.getPageSize());
    }

    @Test
    void loginRecordAndAuditTrailPaginationFilterCorrectlyAndExcludeSystemUser() {
        jdbc.update("INSERT INTO sys_user(id, username) VALUES (1, 'System'), (2, 'alice'), (3, 'bob')");
        for (int i = 1; i <= 5; i++) {
            jdbc.update("INSERT INTO login_record(user_id, login_start_time, created_at) VALUES (?, DATEADD('MINUTE', ?, TIMESTAMP '2026-10-01 08:00:00'), CURRENT_TIMESTAMP)",
                    i == 1 ? 1L : (i <= 3 ? 2L : 3L), i);
            jdbc.update("INSERT INTO audit_trail(user_id, module, action, target_id, occurred_at) VALUES (?, ?, ?, ?, DATEADD('MINUTE', ?, TIMESTAMP '2026-10-01 08:00:00'))",
                    i == 1 ? 1L : (i <= 3 ? 2L : 3L), i % 2 == 0 ? "CUSTOMERS" : "PROJECTS", i <= 3 ? "CREATE" : "UPDATE", "id-" + i, i);
        }

        LoginRecordServiceImpl loginService = new LoginRecordServiceImpl();
        ReflectionTestUtils.setField(loginService, "loginRecordMapper", sql.getMapper(LoginRecordMapper.class));
        ReflectionTestUtils.setField(loginService, "sysUserService", mock(SysUserService.class));

        PageResult<LoginRecord> loginPage1 = loginService.getPage(null, 1, 2);
        assertEquals(4L, loginPage1.getTotal());
        assertEquals(2, loginPage1.getRecords().size());
        assertTrue(loginPage1.getRecords().stream().noneMatch(r -> Long.valueOf(1L).equals(r.getUserId())));

        PageResult<LoginRecord> loginFiltered = loginService.getPage(2L, 1, 10);
        assertEquals(2L, loginFiltered.getTotal());
        assertEquals(2, loginFiltered.getRecords().size());

        AuditTrailServiceImpl auditService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(auditService, "auditTrailMapper", sql.getMapper(AuditTrailMapper.class));
        ReflectionTestUtils.setField(auditService, "sysUserService", mock(SysUserService.class));

        PageResult<AuditTrail> auditPage = auditService.getPage(null, "customers", null, 1, 10);
        assertEquals(2L, auditPage.getTotal());
        assertTrue(auditPage.getRecords().stream().allMatch(a -> "customers".equals(a.getModule())));
    }

    @Test
    void businessTablesPaginateAndFilterCorrectlyAndEnrichProjectsBatchWise() {
        jdbc.update("INSERT INTO goods_transaction_dynamic_info(uuid, material_id, material_code, material_name, category, txn_type, project_id, warehouse_id, quantity, source_ref_id, txn_time, create_time) VALUES ('t1','m1','0101','Alpha Switch','PROJECT_PURCHASE','IN','p1',1,5,'RKD1',TIMESTAMP '2026-10-01 10:00:00',CURRENT_TIMESTAMP), ('t2','m2','0102','Beta Router','PROJECT_SALES','OUT','p1',1,2,'FHD1',TIMESTAMP '2026-10-02 10:00:00',CURRENT_TIMESTAMP), ('t3','m3','0103','Alpha Cable','PROJECT_PURCHASE','IN','p2',2,10,'RKD2',TIMESTAMP '2026-10-03 10:00:00',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO customer_info(uuid, customer_code, customer_name, industry, activity_status) VALUES ('c1','110110001','Alpha Telecom','通信','ACTIVE'), ('c2','110110002','Beta Energy','电力','NORMAL'), ('c3','110110003','Gamma Telecom','通信','DORMANT')");
        jdbc.update("INSERT INTO product_basic_info(uuid, material_code, product_name, product_model, brand_id, category_id, is_active) VALUES ('m1','010101001AAA','Fiber Switch','FS-24',10,1,true), ('m2','010101002AAA','Core Router','CR-80',20,1,true), ('m3','010101003AAA','Fiber Cable','FC-10',10,1,false)");
        jdbc.update("INSERT INTO stage(id, sort_order, description) VALUES (1, 2, 'Init'), (2, 7, 'Closed')");
        jdbc.update("INSERT INTO project_basic_info(uuid, project_number, project_name, customer_id, customer_uuid, manager_id, region_id, project_type_id) VALUES ('p1','AB-0001','Alpha Net','c1','c1',100,1,1), ('p2','AB-0002','Beta Grid','c2','c2',200,1,1), ('p3','CD-0003','Alpha Cloud','c1','c1',100,1,1)");
        jdbc.update("INSERT INTO project_status_history(uuid, project_id, stage_id) VALUES ('h1','p1',1), ('h2','p3',2)");

        InventoryTransactionServiceImpl txnService = new InventoryTransactionServiceImpl();
        ReflectionTestUtils.setField(txnService, "inventoryTransactionMapper", sql.getMapper(InventoryTransactionMapper.class));
        PageResult<InventoryTransaction> txnPage = txnService.getPage(null, "alpha", "PROJECT_PURCHASE", null, null, null, null, 1, 1);
        assertEquals(2L, txnPage.getTotal());
        assertEquals(1, txnPage.getRecords().size());
        assertEquals("t3", txnPage.getRecords().get(0).getUuid());

        CustomerServiceImpl customerService = new CustomerServiceImpl();
        ReflectionTestUtils.setField(customerService, "customerMapper", sql.getMapper(CustomerMapper.class));
        PageResult<Customer> customerPage = customerService.getPage(null, null, null, "通信", "ACTIVE", 1, 10);
        assertEquals(1L, customerPage.getTotal());
        assertEquals("c1", customerPage.getRecords().get(0).getUuid());

        MaterialMasterServiceImpl materialService = new MaterialMasterServiceImpl();
        ReflectionTestUtils.setField(materialService, "materialMasterMapper", sql.getMapper(MaterialMasterMapper.class));
        PageResult<MaterialMaster> materialPage = materialService.getPage(null, "fiber", null, 10L, true, 1, 10);
        assertEquals(1L, materialPage.getTotal());
        assertEquals("m1", materialPage.getRecords().get(0).getUuid());

        ProjectStatusHistoryMapper historySpy = mock(ProjectStatusHistoryMapper.class, delegatesTo(sql.getMapper(ProjectStatusHistoryMapper.class)));
        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.getInt("project.delete.lock_stage_sort_order", 5)).thenReturn(5);

        ProjectServiceImpl projectService = new ProjectServiceImpl();
        ReflectionTestUtils.setField(projectService, "projectMapper", sql.getMapper(ProjectMapper.class));
        ReflectionTestUtils.setField(projectService, "projectStatusHistoryMapper", historySpy);
        ReflectionTestUtils.setField(projectService, "systemConfigService", configService);

        PageResult<Project> projectPage = projectService.getPage(null, "alpha", null, 100L, 1, 1);
        assertEquals(2L, projectPage.getTotal());
        assertEquals(1, projectPage.getRecords().size());
        verify(historySpy, times(1)).selectCurrentStageSortOrdersByProjectIds(anyList());
        assertEquals("p3", projectPage.getRecords().get(0).getUuid());
        assertFalse(projectPage.getRecords().get(0).getCanDelete());
    }
}
