package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.AuditTrailMapper;
import com.jerry.salesmanagement.mapper.LoginRecordMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.SysPermissionMapper;
import com.jerry.salesmanagement.mapper.SysRoleMapper;
import com.jerry.salesmanagement.mapper.SysUserMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentItemMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.pojo.AuditTrail;
import com.jerry.salesmanagement.pojo.LoginRecord;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BatchQueryOptimizationTests {

    private JdbcTemplate jdbc;
    private SqlSessionTemplate sql;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);

        jdbc.execute("CREATE TABLE sys_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), password VARCHAR(255), real_name VARCHAR(50), status VARCHAR(16), force_password_change BOOLEAN, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE sys_role (id BIGINT AUTO_INCREMENT PRIMARY KEY, role_name VARCHAR(50), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE sys_permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, permission_code VARCHAR(128), permission_name VARCHAR(100), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE warehouse_doc (doc_number VARCHAR(50) PRIMARY KEY, doc_type VARCHAR(50), business_category VARCHAR(64), project_id VARCHAR(36), warehouse_id BIGINT, counterparty_customer_id VARCHAR(36), counterparty_name VARCHAR(200), counterparty_address VARCHAR(255), counterparty_contact VARCHAR(255), contract_number VARCHAR(64), doc_date TIMESTAMP, total_amount DECIMAL(18,2), source_ref_type VARCHAR(100), source_ref_id VARCHAR(100), entry_user BIGINT, auditor_user BIGINT, remark TEXT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE warehouse_doc_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), doc_number VARCHAR(50), line_no INT, material_id VARCHAR(36), material_code VARCHAR(12), material_name VARCHAR(200), model VARCHAR(200), display_name VARCHAR(200), display_model VARCHAR(200), brand VARCHAR(128), unit VARCHAR(32), quantity DECIMAL(18,4), unit_price DECIMAL(18,4), amount DECIMAL(18,2), remark TEXT, create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE project_basic_info (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), project_number VARCHAR(10), project_name VARCHAR(200), customer_id VARCHAR(36), customer_uuid VARCHAR(36), manager_id BIGINT, participant_1_user_id BIGINT, participant_2_user_id BIGINT, participant_3_user_id BIGINT, region_id BIGINT, project_type_id BIGINT, warranty_until DATE, plan_start_time TIMESTAMP, plan_end_time TIMESTAMP, sales_contract_attachment_uuid VARCHAR(36), create_time TIMESTAMP, create_user BIGINT, update_time TIMESTAMP, update_user BIGINT)");
        jdbc.execute("CREATE TABLE stage (id BIGINT PRIMARY KEY, sort_order INT, description VARCHAR(128))");
        jdbc.execute("CREATE TABLE project_status_history (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(64), project_id VARCHAR(64), stage_id BIGINT, created_at TIMESTAMP, created_by BIGINT)");
        jdbc.execute("CREATE TABLE audit_trail (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, module VARCHAR(64), action VARCHAR(64), target_type VARCHAR(64), target_id VARCHAR(128), detail TEXT, occurred_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE login_record (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, login_start_time TIMESTAMP, login_end_time TIMESTAMP, duration_seconds BIGINT, login_ip VARCHAR(64), user_agent VARCHAR(512), last_seen_at TIMESTAMP, logout_reason VARCHAR(32), created_at TIMESTAMP)");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/SysUserMapper.xml"),
                new ClassPathResource("mapper/SysRoleMapperMapper.xml"),
                new ClassPathResource("mapper/SysPermissionMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentMapper.xml"),
                new ClassPathResource("mapper/WarehouseDocumentItemMapper.xml"),
                new ClassPathResource("mapper/ProjectMapper.xml"),
                new ClassPathResource("mapper/ProjectStatusHistoryMapper.xml"),
                new ClassPathResource("mapper/AuditTrailMapper.xml"),
                new ClassPathResource("mapper/LoginRecordMapper.xml")
        });
        sql = new SqlSessionTemplate(factory.getObject());
    }

    @Test
    void warehouseDocumentGetAllBatchesItemLoadingInASingleQuery() {
        jdbc.update("INSERT INTO warehouse_doc(doc_number, doc_type, business_category, counterparty_name, doc_date) VALUES ('RKD202610001', 'INBOUND_NOTE', 'PROJECT_PURCHASE', 'Supp1', CURRENT_TIMESTAMP), ('RKD202610002', 'INBOUND_NOTE', 'PROJECT_PURCHASE', 'Supp2', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO warehouse_doc_item(uuid, doc_number, line_no, material_id, material_name, display_name, display_model, quantity) VALUES ('i1', 'RKD202610001', 2, 'm1', 'MatB', 'MatB', 'MB', 2), ('i2', 'RKD202610001', 1, 'm1', 'MatA', 'MatA', 'MA', 1), ('i3', 'RKD202610002', 1, 'm2', 'MatC', 'MatC', 'MC', 3)");

        WarehouseDocumentItemMapper itemMapperSpy = mock(WarehouseDocumentItemMapper.class, delegatesTo(sql.getMapper(WarehouseDocumentItemMapper.class)));
        WarehouseDocumentServiceImpl service = new WarehouseDocumentServiceImpl();
        ReflectionTestUtils.setField(service, "warehouseDocumentMapper", sql.getMapper(WarehouseDocumentMapper.class));
        ReflectionTestUtils.setField(service, "warehouseDocumentItemMapper", itemMapperSpy);

        List<WarehouseDocument> docs = service.getAll();
        assertEquals(2, docs.size());
        verify(itemMapperSpy, times(1)).selectByDocNumbers(anyList());
        verify(itemMapperSpy, never()).selectByDocNumber(anyString());

        WarehouseDocument doc1 = docs.stream().filter(d -> "RKD202610001".equals(d.getDocNumber())).findFirst().orElseThrow();
        assertEquals(2, doc1.getItems().size());
        assertEquals(1, doc1.getItems().get(0).getLineNo());
        assertEquals(2, doc1.getItems().get(1).getLineNo());
    }

    @Test
    void projectGetAllBatchesStageSortOrdersAndReadsLockConfigOnce() {
        jdbc.update("INSERT INTO stage(id, sort_order, description) VALUES (1, 2, 'Early'), (2, 6, 'Locked')");
        jdbc.update("INSERT INTO project_basic_info(uuid, project_number, project_name, customer_id, customer_uuid, manager_id, region_id, project_type_id) VALUES ('p-early', 'AB-0001', 'Early', 'c1', 'c1', 1, 1, 1), ('p-locked', 'AB-0002', 'Locked', 'c1', 'c1', 1, 1, 1), ('p-none', 'AB-0003', 'NoStage', 'c1', 'c1', 1, 1, 1)");
        jdbc.update("INSERT INTO project_status_history(uuid, project_id, stage_id) VALUES ('h1', 'p-early', 1), ('h2', 'p-locked', 1), ('h3', 'p-locked', 2)");

        ProjectStatusHistoryMapper historySpy = mock(ProjectStatusHistoryMapper.class, delegatesTo(sql.getMapper(ProjectStatusHistoryMapper.class)));
        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.getInt("project.delete.lock_stage_sort_order", 5)).thenReturn(5);

        ProjectServiceImpl service = new ProjectServiceImpl();
        ReflectionTestUtils.setField(service, "projectMapper", sql.getMapper(ProjectMapper.class));
        ReflectionTestUtils.setField(service, "projectStatusHistoryMapper", historySpy);
        ReflectionTestUtils.setField(service, "systemConfigService", configService);

        List<Project> projects = service.getAll();
        assertEquals(3, projects.size());
        verify(historySpy, times(1)).selectCurrentStageSortOrdersByProjectIds(anyList());
        verify(historySpy, never()).selectCurrentStageSortOrderByProjectId(anyString());
        verify(configService, times(1)).getInt("project.delete.lock_stage_sort_order", 5);

        Project early = projects.stream().filter(p -> "p-early".equals(p.getUuid())).findFirst().orElseThrow();
        Project locked = projects.stream().filter(p -> "p-locked".equals(p.getUuid())).findFirst().orElseThrow();
        Project none = projects.stream().filter(p -> "p-none".equals(p.getUuid())).findFirst().orElseThrow();
        assertEquals(2, early.getCurrentStageSortOrder());
        assertTrue(early.getCanDelete());
        assertEquals(6, locked.getCurrentStageSortOrder());
        assertFalse(locked.getCanDelete());
        assertNull(none.getCurrentStageSortOrder());
        assertTrue(none.getCanDelete());
    }

    @Test
    void auditTrailAndLoginRecordGetAllFilterSystemUserInSqlWithoutPerRowLookups() {
        jdbc.update("INSERT INTO sys_user(id, username, status) VALUES (1, 'System', 'ACTIVE'), (2, 'alice', 'ACTIVE')");
        jdbc.update("INSERT INTO audit_trail(user_id, module, action, target_type, target_id, occurred_at) VALUES (1, 'PROJECTS', 'CREATE', 'projects', 'p1', CURRENT_TIMESTAMP), (2, 'PROJECTS', 'UPDATE', 'projects', 'p2', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO login_record(user_id, login_start_time, created_at) VALUES (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), (2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        SysUserService sysUserService = mock(SysUserService.class);

        AuditTrailServiceImpl auditService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(auditService, "auditTrailMapper", sql.getMapper(AuditTrailMapper.class));
        ReflectionTestUtils.setField(auditService, "sysUserService", sysUserService);

        List<AuditTrail> audits = auditService.getAll(null, "projects", null);
        assertEquals(1, audits.size());
        assertEquals("alice", audits.get(0).getUsername());
        assertEquals("projects", audits.get(0).getModule());

        LoginRecordServiceImpl loginService = new LoginRecordServiceImpl();
        ReflectionTestUtils.setField(loginService, "loginRecordMapper", sql.getMapper(LoginRecordMapper.class));
        ReflectionTestUtils.setField(loginService, "sysUserService", sysUserService);

        List<LoginRecord> logins = loginService.getAll();
        assertEquals(1, logins.size());
        assertEquals(2L, logins.get(0).getUserId());
        verify(sysUserService, never()).getById(anyLong());
    }

    @Test
    void customUserDetailsServiceLoadsDeduplicatedAuthoritiesViaJoinQueries() {
        jdbc.update("INSERT INTO sys_user(id, username, password, status) VALUES (10, 'alice', '{bcrypt}pwd', 'ACTIVE'), (11, 'disabledUser', '{bcrypt}pwd', 'DISABLED')");
        jdbc.update("INSERT INTO sys_role(id, role_name) VALUES (1, 'ADMIN'), (2, 'EDITOR')");
        jdbc.update("INSERT INTO sys_permission(id, permission_code, permission_name) VALUES (1, 'customer.access', 'Customer Access'), (2, 'project.access', 'Project Access'), (3, ' ', 'Blank')");
        jdbc.update("INSERT INTO sys_user_role(user_id, role_id) VALUES (10, 1), (10, 2)");
        jdbc.update("INSERT INTO sys_role_permission(role_id, permission_id) VALUES (1, 1), (1, 2), (2, 2), (2, 3)");

        SysUserServiceImpl userService = new SysUserServiceImpl();
        ReflectionTestUtils.setField(userService, "sysUserMapper", sql.getMapper(SysUserMapper.class));

        SysRoleServiceImpl roleService = new SysRoleServiceImpl();
        ReflectionTestUtils.setField(roleService, "roleMapper", sql.getMapper(SysRoleMapper.class));

        SysPermissionServiceImpl permService = new SysPermissionServiceImpl();
        ReflectionTestUtils.setField(permService, "permissionMapper", sql.getMapper(SysPermissionMapper.class));

        CustomUserDetailsService detailsService = new CustomUserDetailsService(userService, roleService, permService);
        UserDetails details = detailsService.loadUserByUsername("alice");
        Set<String> auths = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("ROLE_ADMIN", "ROLE_EDITOR", "customer.access", "project.access"), auths);
        assertThrows(DisabledException.class, () -> detailsService.loadUserByUsername("disabledUser"));
    }
}
