package com.jerry.salesmanagement.config;

import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.SysUserService;
import com.jerry.salesmanagement.service.impl.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_OR_LOGOUT_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/logout-beacon",
            "/api/auth/register",
            "/api/auth/config"
    );

    private static final Set<String> FORCE_PASSWORD_CHANGE_ALLOWED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/logout-beacon",
            "/api/auth/heartbeat",
            "/api/auth/config",
            "/api/auth/me",
            "/api/auth/change-password"
    );

    private final SysUserService sysUserService;
    private final CustomUserDetailsService customUserDetailsService;

    public PasswordChangeRequiredFilter(
            SysUserService sysUserService,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.sysUserService = sysUserService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || PUBLIC_OR_LOGOUT_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            filterChain.doFilter(request, response);
            return;
        }

        SysUser currentUser = sysUserService.getByUsername(authentication.getName());
        if (currentUser == null || !"ACTIVE".equalsIgnoreCase(currentUser.getStatus())) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Account is inactive");
            return;
        }

        refreshAuthoritiesIfChanged(request, authentication, currentUser);

        if (Boolean.TRUE.equals(currentUser.getForcePasswordChange())
                && !FORCE_PASSWORD_CHANGE_ALLOWED_PATHS.contains(path)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Password change required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void refreshAuthoritiesIfChanged(
            HttpServletRequest request,
            Authentication authentication,
            SysUser currentUser
    ) {
        if (customUserDetailsService == null) {
            return;
        }
        List<GrantedAuthority> liveAuthorities = customUserDetailsService.loadAuthorities(currentUser);
        Set<String> liveNames = liveAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        Set<String> currentNames = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (liveNames.equals(currentNames)) {
            return;
        }

        UsernamePasswordAuthenticationToken refreshed = UsernamePasswordAuthenticationToken.authenticated(
                authentication.getPrincipal(),
                authentication.getCredentials(),
                liveAuthorities
        );
        refreshed.setDetails(authentication.getDetails());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(refreshed);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);
        }
    }
}
