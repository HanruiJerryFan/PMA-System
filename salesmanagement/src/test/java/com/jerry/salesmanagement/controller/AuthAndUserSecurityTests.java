package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.LoginRecord;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.LoginRequest;
import com.jerry.salesmanagement.pojo.dto.RegisterRequest;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.LoginRecordService;
import com.jerry.salesmanagement.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthAndUserSecurityTests {

    @Test
    void selfRegistrationIsDisabledByDefaultAndReturnsForbidden() {
        SysUserService sysUserService = mock(SysUserService.class);
        AuthController controller = createAuthController(sysUserService);

        ResponseEntity<Map<String, Boolean>> configResponse = controller.getAuthConfig();
        assertEquals(HttpStatus.OK, configResponse.getStatusCode());
        assertEquals(Map.of("selfRegistrationEnabled", false), configResponse.getBody());

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Pass@1234");
        request.setRealName("New User");

        ResponseEntity<String> registerResponse = controller.register(request);
        assertEquals(HttpStatus.FORBIDDEN, registerResponse.getStatusCode());
        verifyNoInteractions(sysUserService);
    }

    @Test
    void selfRegistrationWorksWhenExplicitlyEnabled() {
        SysUserService sysUserService = mock(SysUserService.class);
        SysUser created = new SysUser();
        created.setId(12L);
        created.setUsername("newuser");
        when(sysUserService.create(any(SysUser.class))).thenReturn(created);

        AuthController controller = createAuthController(sysUserService);
        ReflectionTestUtils.setField(controller, "selfRegistrationEnabled", true);

        ResponseEntity<Map<String, Boolean>> configResponse = controller.getAuthConfig();
        assertEquals(Map.of("selfRegistrationEnabled", true), configResponse.getBody());

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Pass@1234");
        request.setRealName("New User");

        ResponseEntity<String> registerResponse = controller.register(request);
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        verify(sysUserService).create(any(SysUser.class));
    }

    @Test
    void resetPasswordAlwaysUsesAuthenticatedCurrentUserAsOperator() {
        SysUserService sysUserService = mock(SysUserService.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireCurrentUserId()).thenReturn(7L);
        when(sysUserService.adminResetPassword(42L, 7L)).thenReturn("Tmp@12345678");

        SysUserController controller = new SysUserController(sysUserService, auditTrailService, currentUserService);
        ApiResponse<Map<String, String>> response = controller.resetPassword(42L, 999L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals("Tmp@12345678", response.getData().get("temporaryPassword"));
        verify(currentUserService).requireCurrentUserId();
        verify(sysUserService).adminResetPassword(42L, 7L);
        verify(auditTrailService).record("users", "RESET_PASSWORD", "users", "42", "Admin reset user password");
    }

    @Test
    void loginRotatesSessionIgnoresSpoofedForwardedHeadersTruncatesUaAndLogoutBeaconOnlyTouchesSession() {
        AuthenticationManager authManager = mock(AuthenticationManager.class);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice", "secret", AuthorityUtils.createAuthorityList("project.access"));
        when(authManager.authenticate(any(Authentication.class))).thenReturn(auth);

        SysUserService sysUserService = mock(SysUserService.class);
        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("alice");
        user.setStatus("ACTIVE");
        when(sysUserService.getByUsername("alice")).thenReturn(user);

        LoginRecordService loginRecordService = mock(LoginRecordService.class);
        when(loginRecordService.create(any(LoginRecord.class))).thenAnswer(inv -> {
            LoginRecord created = inv.getArgument(0);
            created.setId(77L);
            return created;
        });

        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authenticationManager", authManager);
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "loginRecordService", loginRecordService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession oldSession = new MockHttpSession();
        oldSession.setAttribute("LOGIN_RECORD_ID", 66L);
        String originalSessionId = oldSession.getId();
        request.setSession(oldSession);
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.addHeader("User-Agent", "Mozilla/".repeat(120));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("alice");
        loginRequest.setPassword("secret");

        try {
            ResponseEntity<String> loginResponse = controller.login(loginRequest, request);
            assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
            assertNotEquals(originalSessionId, request.getSession(false).getId());
            verify(loginRecordService).completeSession(eq(66L), any(Date.class), eq("RELOGIN"));

            ArgumentCaptor<LoginRecord> recordCaptor = ArgumentCaptor.forClass(LoginRecord.class);
            verify(loginRecordService).create(recordCaptor.capture());
            assertEquals("10.0.0.8", recordCaptor.getValue().getLoginIp());
            assertEquals(512, recordCaptor.getValue().getUserAgent().length());

            ResponseEntity<String> beaconResponse = controller.logoutBeacon(request);
            assertEquals(HttpStatus.OK, beaconResponse.getStatusCode());
            verify(loginRecordService).touchSession(eq(77L), any(Date.class));
            verify(loginRecordService, never()).completeSession(eq(77L), any(Date.class), any());
            assertNotNull(request.getSession(false));
            assertFalse(((MockHttpSession) request.getSession(false)).isInvalid());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private AuthController createAuthController(SysUserService sysUserService) {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authenticationManager", mock(AuthenticationManager.class));
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "loginRecordService", mock(LoginRecordService.class));
        return controller;
    }
}
