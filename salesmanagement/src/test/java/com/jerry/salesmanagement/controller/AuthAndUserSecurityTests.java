package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.RegisterRequest;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.LoginRecordService;
import com.jerry.salesmanagement.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    private AuthController createAuthController(SysUserService sysUserService) {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authenticationManager", mock(AuthenticationManager.class));
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "loginRecordService", mock(LoginRecordService.class));
        return controller;
    }
}
