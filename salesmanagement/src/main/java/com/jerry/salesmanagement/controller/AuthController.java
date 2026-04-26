package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.LoginRecord;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.ChangePasswordRequest;
import com.jerry.salesmanagement.pojo.dto.LoginRequest;
import com.jerry.salesmanagement.pojo.dto.RegisterRequest;
import com.jerry.salesmanagement.service.LoginRecordService;
import com.jerry.salesmanagement.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private static final String LOGIN_RECORD_ID = "LOGIN_RECORD_ID";
    private static final String SYSTEM_USERNAME = "System";

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private LoginRecordService loginRecordService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        SysUser currentUser = sysUserService.getByUsername(loginRequest.getUsername());
        if (currentUser != null && currentUser.getStatus() != null
                && !"ACTIVE".equalsIgnoreCase(currentUser.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not active");
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
        try {
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
            recordLoginStart(authentication, request, session);
            return ResponseEntity.ok("Login success!");
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not active");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad credentials");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            recordLoginEnd(session, "MANUAL");
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logout success!");
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<String> heartbeat(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No Session");
        }

        SecurityContext securityContext = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        if (securityContext == null
                || securityContext.getAuthentication() == null
                || !securityContext.getAuthentication().isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No Authentication");
        }

        touchLoginRecord(session);
        return ResponseEntity.ok("Heartbeat success!");
    }

    @PostMapping("/logout-beacon")
    public ResponseEntity<String> logoutBeacon(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.ok("No Session");
        }
        recordLoginEnd(session, "BROWSER_CLOSED");
        return ResponseEntity.ok("Logout beacon success!");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {
        try {
            SysUser user = new SysUser();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(registerRequest.getPassword());
            user.setRealName(registerRequest.getRealName());

            boolean success = sysUserService.create(user) != null;
            if (success) {
                return ResponseEntity.ok("Register success!");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Register failed!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest requestContext) {
        HttpSession session = requestContext.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No Session");
        }

        SecurityContext securityContext = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        if (securityContext == null || securityContext.getAuthentication() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No Authentication");
        }

        try {
            Authentication auth = securityContext.getAuthentication();
            SysUser currentUser = sysUserService.getByUsername(auth.getName());
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            sysUserService.changePassword(
                    currentUser.getId(),
                    request.getOldPassword(),
                    request.getNewPassword(),
                    currentUser.getId()
            );
            recordLoginEnd(session, "FORCED");
            session.invalidate();
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok("Password changed successfully. Please log in again.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No Session");
        }

        SecurityContext securityContext = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        if (securityContext == null
                || securityContext.getAuthentication() == null
                || !securityContext.getAuthentication().isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No Authentication");
        }

        Authentication auth = securityContext.getAuthentication();
        SysUser currentUser = sysUserService.getByUsername(auth.getName());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", currentUser != null ? currentUser.getId() : null);
        payload.put("username", auth.getName());
        payload.put("realName", currentUser != null ? currentUser.getRealName() : null);
        payload.put("forcePasswordChange", currentUser != null && Boolean.TRUE.equals(currentUser.getForcePasswordChange()));
        payload.put(
                "authorities",
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())
        );
        return ResponseEntity.ok(payload);
    }

    private void recordLoginStart(Authentication authentication, HttpServletRequest request, HttpSession session) {
        SysUser currentUser = sysUserService.getByUsername(authentication.getName());
        if (currentUser == null || isSystemUser(currentUser)) {
            return;
        }

        LoginRecord record = new LoginRecord();
        Date loginStartTime = new Date();
        record.setUserId(currentUser.getId());
        record.setLoginStartTime(loginStartTime);
        record.setLoginIp(extractClientIp(request));
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setLastSeenAt(loginStartTime);
        record.setCreatedAt(loginStartTime);
        LoginRecord created = loginRecordService.create(record);
        if (created != null && created.getId() != null) {
            session.setAttribute(LOGIN_RECORD_ID, created.getId());
        }
    }

    private void touchLoginRecord(HttpSession session) {
        Object loginRecordIdValue = session.getAttribute(LOGIN_RECORD_ID);
        if (!(loginRecordIdValue instanceof Long loginRecordId)) {
            return;
        }
        loginRecordService.touchSession(loginRecordId, new Date());
    }

    private void recordLoginEnd(HttpSession session, String logoutReason) {
        Object loginRecordIdValue = session.getAttribute(LOGIN_RECORD_ID);
        if (!(loginRecordIdValue instanceof Long loginRecordId)) {
            return;
        }
        loginRecordService.completeSession(loginRecordId, new Date(), logoutReason);
    }

    private String extractClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isSystemUser(SysUser user) {
        return user != null && SYSTEM_USERNAME.equalsIgnoreCase(user.getUsername());
    }
}
