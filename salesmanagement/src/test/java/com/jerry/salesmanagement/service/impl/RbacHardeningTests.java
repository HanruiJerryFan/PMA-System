package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.config.PasswordChangeRequiredFilter;
import com.jerry.salesmanagement.mapper.SysPermissionMapper;
import com.jerry.salesmanagement.mapper.SysRoleMapper;
import com.jerry.salesmanagement.mapper.SysRolePermissionMapper;
import com.jerry.salesmanagement.mapper.SysUserMapper;
import com.jerry.salesmanagement.mapper.SysUserRoleMapper;
import com.jerry.salesmanagement.mapper.SystemConfigMapper;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.SysPermission;
import com.jerry.salesmanagement.pojo.SysRole;
import com.jerry.salesmanagement.pojo.SysRolePermission;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.SysUserRole;
import com.jerry.salesmanagement.pojo.SystemConfig;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.SysPermissionService;
import com.jerry.salesmanagement.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacHardeningTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterRejectsDisabledOrDeletedUserAndInvalidatesSession() throws Exception {
        SysUserService sysUserService = mock(SysUserService.class);
        CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
        PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter(sysUserService, detailsService);

        SysUser disabledUser = new SysUser();
        disabledUser.setId(5L);
        disabledUser.setUsername("bob");
        disabledUser.setStatus("DISABLED");
        when(sysUserService.getByUsername("bob")).thenReturn(disabledUser);

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "bob",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("finance.manage"))
                )
        );
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/finance-vouchers");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(chain.getRequest());

        // Deleted user (null from DB) is also invalidated immediately
        when(sysUserService.getByUsername("deletedUser")).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "deletedUser",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("project.access"))
                )
        );
        MockHttpSession deletedSession = new MockHttpSession();
        MockHttpServletRequest deletedReq = new MockHttpServletRequest("GET", "/api/projects");
        deletedReq.setSession(deletedSession);
        MockHttpServletResponse deletedRes = new MockHttpServletResponse();
        filter.doFilter(deletedReq, deletedRes, new MockFilterChain());

        assertEquals(401, deletedRes.getStatus());
        assertTrue(deletedSession.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void filterRefreshesChangedAuthoritiesIntoSecurityContextAndSession() throws Exception {
        SysUserService sysUserService = mock(SysUserService.class);
        CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
        PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter(sysUserService, detailsService);

        SysUser activeUser = new SysUser();
        activeUser.setId(8L);
        activeUser.setUsername("alice");
        activeUser.setStatus("ACTIVE");
        activeUser.setForcePasswordChange(false);
        when(sysUserService.getByUsername("alice")).thenReturn(activeUser);
        when(detailsService.loadAuthorities(activeUser)).thenReturn(List.of(
                new SimpleGrantedAuthority("ROLE_审核员"),
                new SimpleGrantedAuthority("finance.voucher.audit")
        ));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "alice",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("finance.manage"))
                )
        );
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/approvals/pending");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        Authentication updatedAuth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> updatedNames = updatedAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_审核员", "finance.voucher.audit"), updatedNames);
        SecurityContext sessionContext = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        assertNotNull(sessionContext);
        assertEquals(updatedAuth, sessionContext.getAuthentication());
    }

    @Test
    void filterForcePasswordChangeAllowsHeartbeatAndMeWhileBlockingBusinessApis() throws Exception {
        SysUserService sysUserService = mock(SysUserService.class);
        CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
        PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter(sysUserService, detailsService);

        SysUser user = new SysUser();
        user.setId(9L);
        user.setUsername("carol");
        user.setStatus("ACTIVE");
        user.setForcePasswordChange(true);
        when(sysUserService.getByUsername("carol")).thenReturn(user);
        when(detailsService.loadAuthorities(user)).thenReturn(List.of(new SimpleGrantedAuthority("project.access")));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "carol", "n/a", List.of(new SimpleGrantedAuthority("project.access")))
        );

        MockHttpServletResponse heartbeatRes = new MockHttpServletResponse();
        MockFilterChain heartbeatChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/auth/heartbeat"), heartbeatRes, heartbeatChain);
        assertNotNull(heartbeatChain.getRequest());

        MockHttpServletResponse meRes = new MockHttpServletResponse();
        MockFilterChain meChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/auth/me"), meRes, meChain);
        assertNotNull(meChain.getRequest());

        MockHttpServletResponse apiRes = new MockHttpServletResponse();
        MockFilterChain apiChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/projects"), apiRes, apiChain);
        assertEquals(403, apiRes.getStatus());
        assertEquals("Password change required", apiRes.getContentAsString());
        assertNull(apiChain.getRequest());
    }

    @Test
    void currentUserServiceDoesNotBypassByHardcodedRoleName() {
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        SysUser adminRoleUser = new SysUser();
        adminRoleUser.setId(2L);
        adminRoleUser.setUsername("normal_admin");
        when(sysUserMapper.selectByUsername("normal_admin")).thenReturn(adminRoleUser);

        CurrentUserServiceImpl service = new CurrentUserServiceImpl();
        ReflectionTestUtils.setField(service, "sysUserMapper", sysUserMapper);

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "normal_admin",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_管理员"))
                )
        );
        assertFalse(service.isSystemUser());
        assertFalse(service.isSystemOrAdmin());

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "normal_admin",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("document.entry-audit.manage"))
                )
        );
        assertTrue(service.isSystemOrAdmin());

        SysUser systemUser = new SysUser();
        systemUser.setId(1L);
        systemUser.setUsername("System");
        when(sysUserMapper.selectByUsername("System")).thenReturn(systemUser);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("System", "n/a", List.of())
        );
        assertTrue(service.isSystemUser());
        assertTrue(service.isSystemOrAdmin());
    }

    @Test
    void entryAuditEnforcesSeparationOfDutiesAndLocksAuditedRecords() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        EntryAuditServiceImpl service = new EntryAuditServiceImpl();
        ReflectionTestUtils.setField(service, "currentUserService", currentUserService);

        when(currentUserService.requireCurrentUserId()).thenReturn(10L);
        when(currentUserService.isSystemOrAdmin()).thenReturn(false);

        FinanceVoucher ownPending = new FinanceVoucher();
        ownPending.setEntryUser(10L);
        ownPending.setAuditorUser(null);
        assertThrows(IllegalArgumentException.class, () -> service.applyAudit(ownPending));

        FinanceVoucher auditedRecord = new FinanceVoucher();
        auditedRecord.setEntryUser(10L);
        auditedRecord.setAuditorUser(20L);
        FinanceVoucher incoming = new FinanceVoucher();
        assertThrows(IllegalArgumentException.class, () -> service.applyUpdate(incoming, auditedRecord));
        assertThrows(IllegalArgumentException.class, () -> service.assertCanDelete(auditedRecord));
        assertThrows(IllegalArgumentException.class, () -> service.applyAudit(auditedRecord));

        when(currentUserService.isSystemOrAdmin()).thenReturn(true);
        service.applyAudit(ownPending);
        assertEquals(10L, ownPending.getAuditorUser());
        service.applyUpdate(incoming, auditedRecord);
        service.assertCanDelete(auditedRecord);
        assertThrows(IllegalArgumentException.class, () -> service.applyAudit(auditedRecord));
    }

    @Test
    void rbacServicesProtectSystemEntitiesAndBlockPrivilegeEscalation() {
        SysUser systemUser = new SysUser();
        systemUser.setId(1L);
        systemUser.setUsername("System");
        systemUser.setStatus("ACTIVE");

        SysUser subAdmin = new SysUser();
        subAdmin.setId(2L);
        subAdmin.setUsername("subadmin");
        subAdmin.setStatus("ACTIVE");

        SysUser financeUser = new SysUser();
        financeUser.setId(3L);
        financeUser.setUsername("finuser");
        financeUser.setStatus("ACTIVE");

        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectById(1L)).thenReturn(systemUser);
        when(userMapper.selectById(2L)).thenReturn(subAdmin);
        when(userMapper.selectById(3L)).thenReturn(financeUser);
        when(userMapper.selectByUsername("System")).thenReturn(systemUser);

        CurrentUserService actorService = mock(CurrentUserService.class);
        when(actorService.getCurrentUser()).thenReturn(subAdmin);
        when(actorService.getCurrentUserId()).thenReturn(2L);
        when(actorService.isSystemUser()).thenReturn(false);
        when(actorService.hasAnyAuthority("permission.users.manage")).thenReturn(true);
        when(actorService.hasAnyAuthority("finance.manage")).thenReturn(false);

        SysPermissionService permissionService = mock(SysPermissionService.class);
        when(permissionService.getPermissionCodesByUserId(3L)).thenReturn(List.of("finance.manage"));

        SysUserServiceImpl userService = new SysUserServiceImpl();
        ReflectionTestUtils.setField(userService, "sysUserMapper", userMapper);
        ReflectionTestUtils.setField(userService, "passwordEncoder", mock(PasswordEncoder.class));
        ReflectionTestUtils.setField(userService, "currentUserService", actorService);
        ReflectionTestUtils.setField(userService, "sysPermissionService", permissionService);

        assertThrows(IllegalArgumentException.class, () -> userService.delete(1L));
        assertThrows(IllegalArgumentException.class, () -> userService.delete(2L));
        assertThrows(IllegalArgumentException.class, () -> userService.adminResetPassword(1L, 2L));
        assertThrows(IllegalArgumentException.class, () -> userService.adminResetPassword(3L, 2L));

        SysUser disableSelf = new SysUser();
        disableSelf.setId(2L);
        disableSelf.setStatus("DISABLED");
        assertThrows(IllegalArgumentException.class, () -> userService.update(disableSelf));

        SysUser editSystem = new SysUser();
        editSystem.setId(1L);
        editSystem.setRealName("Hijack");
        assertThrows(IllegalArgumentException.class, () -> userService.update(editSystem));

        // SysRoleServiceImpl blocks updating or deleting System role
        SysRole systemRole = new SysRole();
        systemRole.setId(1L);
        systemRole.setRoleName("System");
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(roleMapper.selectById(1L)).thenReturn(systemRole);

        SysRoleServiceImpl roleService = new SysRoleServiceImpl();
        ReflectionTestUtils.setField(roleService, "roleMapper", roleMapper);
        SysRole renameSystem = new SysRole();
        renameSystem.setId(1L);
        renameSystem.setRoleName("Other");
        assertThrows(IllegalArgumentException.class, () -> roleService.update(renameSystem));
        assertThrows(IllegalArgumentException.class, () -> roleService.delete(1L));

        // SysUserRoleServiceImpl blocks self role change, System role/user manipulation, and escalation
        SysRole privilegedRole = new SysRole();
        privilegedRole.setId(9L);
        privilegedRole.setRoleName("财务总监");
        when(roleMapper.selectById(9L)).thenReturn(privilegedRole);

        SysPermission finPerm = new SysPermission();
        finPerm.setId(21L);
        finPerm.setPermissionCode("finance.manage");
        SysPermissionMapper permMapper = mock(SysPermissionMapper.class);
        when(permMapper.selectAll()).thenReturn(List.of(finPerm));
        when(permMapper.selectById(21L)).thenReturn(finPerm);

        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(9L);
        rp.setPermissionId(21L);
        SysRolePermissionMapper rpMapper = mock(SysRolePermissionMapper.class);
        when(rpMapper.selectByRoleId(9L)).thenReturn(List.of(rp));

        SysUserRoleServiceImpl userRoleService = new SysUserRoleServiceImpl();
        ReflectionTestUtils.setField(userRoleService, "userRoleMapper", mock(SysUserRoleMapper.class));
        ReflectionTestUtils.setField(userRoleService, "currentUserService", actorService);
        ReflectionTestUtils.setField(userRoleService, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(userRoleService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userRoleService, "rolePermissionMapper", rpMapper);
        ReflectionTestUtils.setField(userRoleService, "permissionMapper", permMapper);

        SysUserRole selfAssign = new SysUserRole();
        selfAssign.setUserId(2L);
        selfAssign.setRoleId(9L);
        assertThrows(IllegalArgumentException.class, () -> userRoleService.addUserRole(selfAssign));

        SysUserRole assignSystemRole = new SysUserRole();
        assignSystemRole.setUserId(3L);
        assignSystemRole.setRoleId(1L);
        assertThrows(IllegalArgumentException.class, () -> userRoleService.addUserRole(assignSystemRole));

        SysUserRole escalateRole = new SysUserRole();
        escalateRole.setUserId(3L);
        escalateRole.setRoleId(9L);
        assertThrows(IllegalArgumentException.class, () -> userRoleService.addUserRole(escalateRole));

        // SysRolePermissionServiceImpl blocks modifying System role or granting unheld permission
        SysRolePermissionServiceImpl rolePermissionService = new SysRolePermissionServiceImpl();
        ReflectionTestUtils.setField(rolePermissionService, "rolePermissionMapper", rpMapper);
        ReflectionTestUtils.setField(rolePermissionService, "currentUserService", actorService);
        ReflectionTestUtils.setField(rolePermissionService, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(rolePermissionService, "permissionMapper", permMapper);

        SysRolePermission editSystemRolePerm = new SysRolePermission();
        editSystemRolePerm.setRoleId(1L);
        editSystemRolePerm.setPermissionId(21L);
        assertThrows(IllegalArgumentException.class, () -> rolePermissionService.addRolePermission(editSystemRolePerm));

        SysRolePermission grantUnheldPerm = new SysRolePermission();
        grantUnheldPerm.setRoleId(9L);
        grantUnheldPerm.setPermissionId(21L);
        assertThrows(IllegalArgumentException.class, () -> rolePermissionService.addRolePermission(grantUnheldPerm));
        assertThrows(IllegalArgumentException.class, () -> rolePermissionService.deleteRolePermission(9L, 21L));
    }

    @Test
    void systemConfigRejectsPathTraversalForSensitivePaths() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        SystemConfigServiceImpl service = new SystemConfigServiceImpl();
        ReflectionTestUtils.setField(service, "systemConfigMapper", mapper);

        SystemConfig badStorage = new SystemConfig();
        badStorage.setConfigKey("attachment.storage.root");
        badStorage.setConfigValue("../../etc");
        assertThrows(IllegalArgumentException.class, () -> service.createOrUpdate(badStorage));

        SystemConfig badFont = new SystemConfig();
        badFont.setConfigKey("pdf.font.path");
        badFont.setConfigValue("C:\\fonts\\..\\secret.ttf");
        assertThrows(IllegalArgumentException.class, () -> service.createOrUpdate(badFont));

        SystemConfig safeStorage = new SystemConfig();
        safeStorage.setConfigKey("attachment.storage.root");
        safeStorage.setConfigValue("/opt/salesmanagement/uploads");
        when(mapper.selectByKey("attachment.storage.root")).thenReturn(safeStorage);
        service.createOrUpdate(safeStorage);
        verify(mapper).updateByKey(any(SystemConfig.class));
    }
}
