package com.jerry.salesmanagement.config;

import com.jerry.salesmanagement.service.impl.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] CUSTOMER_PATHS = {
            "/api/customers/**",
            "/api/customer-contacts/**",
            "/api/customer-industry-dicts/**",
            "/api/customer-activity-rules/**",
            "/api/customer-types/**",
            "/api/region/**",
            "/api/provinces",
            "/api/cities",
            "/api/cities/**"
    };

    private static final String[] PROJECT_PATHS = {
            "/api/projects/**",
            "/api/project-types/**",
            "/api/project-stages/**",
            "/api/project-status-history/**",
            "/api/project-lists/**",
            "/api/project-list-items/**"
    };

    private static final String[] CONTRACT_PATHS = {
            "/api/contracts/**",
            "/api/contract-clauses/**",
            "/api/contract-types/**",
            "/api/clausetype/**"
    };

    private static final String[] PRODUCT_PATHS = {
            "/api/materials/**",
            "/api/material-category-dicts/**",
            "/api/material-brand-dicts/**",
            "/api/material-subcategory-dicts/**",
            "/api/material-band-dicts/**",
            "/api/product-prices/**"
    };

    private static final String[] INVENTORY_PATHS = {
            "/api/inventory-transactions/**",
            "/api/inventory-stock/**",
            "/api/warehouses/**",
            "/api/warehouse-documents/**"
    };

    private static final String[] FINANCE_PATHS = {
            "/api/finance-vouchers/**",
            "/api/tax-rate-dicts/**"
    };

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            PasswordChangeRequiredFilter passwordChangeRequiredFilter,
            PasswordEncoder passwordEncoder
    ) {
        this.customUserDetailsService = customUserDetailsService;
        this.passwordChangeRequiredFilter = passwordChangeRequiredFilter;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/pdf-exports/**")
                        .hasAnyAuthority(
                                "customer.access",
                                "customer.manage",
                                "project.access",
                                "project.manage",
                                "contract.access",
                                "contract.manage",
                                "product.access",
                                "product.manage",
                                "inventory.access",
                                "inventory.manage",
                                "finance.access",
                                "finance.manage"
                        )

                        .requestMatchers(HttpMethod.GET, "/api/system-configs/**").hasAuthority("permission.items.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/system-configs/**").hasAuthority("permission.items.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/system-configs/**").hasAuthority("permission.items.manage")

                        .requestMatchers(HttpMethod.GET, "/api/users/options").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/customers/options")
                        .hasAnyAuthority(
                                "customer.access",
                                "customer.manage",
                                "project.access",
                                "project.manage",
                                "project.list.entry",
                                "project.list.audit",
                                "contract.access",
                                "contract.manage",
                                "product.manage",
                                "inventory.access",
                                "inventory.manage",
                                "inventory.warehouse-doc.entry",
                                "inventory.warehouse-doc.audit",
                                "finance.access",
                                "finance.manage",
                                "finance.voucher.entry",
                                "finance.voucher.audit",
                                "attachment.manage"
                        )
                        .requestMatchers(HttpMethod.GET, "/api/customer-types/options")
                        .hasAnyAuthority("customer.access", "product.manage", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/customer-contacts/options")
                        .hasAnyAuthority("customer.access", "contract.access", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/projects/options")
                        .hasAnyAuthority("project.access", "project.manage", "project.list.entry", "project.list.audit", "contract.access", "contract.manage", "finance.access", "finance.manage", "finance.voucher.entry", "finance.voucher.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/project-lists/options")
                        .hasAnyAuthority("project.access", "project.manage", "project.list.entry", "project.list.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/contracts/options")
                        .hasAnyAuthority("contract.access", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/finance-vouchers/options")
                        .hasAnyAuthority("finance.access", "finance.manage", "finance.voucher.entry", "finance.voucher.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/warehouse-documents/options")
                        .hasAnyAuthority("inventory.access", "inventory.manage", "inventory.warehouse-doc.entry", "inventory.warehouse-doc.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/region/options")
                        .hasAnyAuthority("customer.access", "project.access", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/materials/options")
                        .hasAnyAuthority("product.access", "project.access", "project.list.entry", "project.list.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/material-category-dicts/options")
                        .hasAnyAuthority("product.access", "project.access", "project.list.entry", "project.list.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/material-subcategory-dicts/options")
                        .hasAnyAuthority("product.access", "project.access", "project.list.entry", "project.list.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/material-brand-dicts/options")
                        .hasAnyAuthority("product.access", "project.access", "project.list.entry", "project.list.audit", "attachment.manage")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAuthority("permission.users.manage")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAuthority("permission.users.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("permission.users.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("permission.users.manage")

                        .requestMatchers(HttpMethod.GET, "/api/roles/**").hasAuthority("permission.roles.manage")
                        .requestMatchers(HttpMethod.POST, "/api/roles/**").hasAuthority("permission.roles.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/roles/**").hasAuthority("permission.roles.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/roles/**").hasAuthority("permission.roles.manage")

                        .requestMatchers(HttpMethod.GET, "/api/permissions/**").hasAuthority("permission.items.manage")
                        .requestMatchers(HttpMethod.POST, "/api/permissions/**").hasAuthority("permission.items.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/permissions/**").hasAuthority("permission.items.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/permissions/**").hasAuthority("permission.items.manage")

                        .requestMatchers(HttpMethod.GET, "/api/sysuserrole/**").hasAuthority("permission.user-roles.manage")
                        .requestMatchers(HttpMethod.POST, "/api/sysuserrole/**").hasAuthority("permission.user-roles.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/sysuserrole/**").hasAuthority("permission.user-roles.manage")

                        .requestMatchers(HttpMethod.GET, "/api/sysrolepermission/**").hasAuthority("permission.role-permissions.manage")
                        .requestMatchers(HttpMethod.POST, "/api/sysrolepermission/**").hasAuthority("permission.role-permissions.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/sysrolepermission/**").hasAuthority("permission.role-permissions.manage")

                        .requestMatchers(HttpMethod.GET, "/api/project-lists/**", "/api/project-list-items/**")
                        .hasAnyAuthority("project.access", "project.manage", "project.list.entry", "project.list.audit")
                        .requestMatchers(HttpMethod.POST, "/api/project-lists/*/audit")
                        .hasAnyAuthority("project.list.audit", "project.manage")
                        .requestMatchers(HttpMethod.POST, "/api/project-lists/**")
                        .hasAnyAuthority("project.list.entry", "project.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/project-lists/**")
                        .hasAnyAuthority("project.list.entry", "project.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/project-lists/**")
                        .hasAnyAuthority("project.list.entry", "project.manage")
                        .requestMatchers(HttpMethod.POST, "/api/project-list-items/**")
                        .hasAnyAuthority("project.list.entry", "project.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/project-list-items/**")
                        .hasAnyAuthority("project.list.entry", "project.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/project-list-items/**")
                        .hasAnyAuthority("project.list.entry", "project.manage")

                        .requestMatchers(HttpMethod.GET, "/api/warehouse-documents/**")
                        .hasAnyAuthority("inventory.access", "inventory.manage", "inventory.warehouse-doc.entry", "inventory.warehouse-doc.audit")
                        .requestMatchers(HttpMethod.GET, "/api/warehouses/**")
                        .hasAnyAuthority("inventory.access", "inventory.manage", "inventory.warehouse-doc.entry", "inventory.warehouse-doc.audit")
                        .requestMatchers(HttpMethod.POST, "/api/warehouse-documents/*/audit")
                        .hasAnyAuthority("inventory.warehouse-doc.audit", "inventory.manage")
                        .requestMatchers(HttpMethod.POST, "/api/warehouse-documents/**")
                        .hasAnyAuthority("inventory.warehouse-doc.entry", "inventory.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/warehouse-documents/**")
                        .hasAnyAuthority("inventory.warehouse-doc.entry", "inventory.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/warehouse-documents/**")
                        .hasAnyAuthority("inventory.warehouse-doc.entry", "inventory.manage")

                        .requestMatchers(HttpMethod.GET, "/api/finance-vouchers/**")
                        .hasAnyAuthority("finance.access", "finance.manage", "finance.voucher.entry", "finance.voucher.audit")
                        .requestMatchers(HttpMethod.GET, "/api/tax-rate-dicts/**")
                        .hasAnyAuthority("finance.access", "finance.manage", "finance.voucher.entry", "finance.voucher.audit")
                        .requestMatchers(HttpMethod.POST, "/api/finance-vouchers/*/audit")
                        .hasAnyAuthority("finance.voucher.audit", "finance.manage")
                        .requestMatchers(HttpMethod.POST, "/api/finance-vouchers/**")
                        .hasAnyAuthority("finance.voucher.entry", "finance.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/finance-vouchers/**")
                        .hasAnyAuthority("finance.voucher.entry", "finance.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/finance-vouchers/**")
                        .hasAnyAuthority("finance.voucher.entry", "finance.manage")

                        .requestMatchers(HttpMethod.GET, CUSTOMER_PATHS).hasAnyAuthority("customer.access", "customer.manage")
                        .requestMatchers(HttpMethod.POST, CUSTOMER_PATHS).hasAuthority("customer.manage")
                        .requestMatchers(HttpMethod.PUT, CUSTOMER_PATHS).hasAuthority("customer.manage")
                        .requestMatchers(HttpMethod.DELETE, CUSTOMER_PATHS).hasAuthority("customer.manage")

                        .requestMatchers(HttpMethod.GET, PROJECT_PATHS).hasAnyAuthority("project.access", "project.manage")
                        .requestMatchers(HttpMethod.POST, PROJECT_PATHS).hasAuthority("project.manage")
                        .requestMatchers(HttpMethod.PUT, PROJECT_PATHS).hasAuthority("project.manage")
                        .requestMatchers(HttpMethod.DELETE, PROJECT_PATHS).hasAuthority("project.manage")

                        .requestMatchers(HttpMethod.GET, CONTRACT_PATHS).hasAnyAuthority("contract.access", "contract.manage")
                        .requestMatchers(HttpMethod.POST, CONTRACT_PATHS).hasAuthority("contract.manage")
                        .requestMatchers(HttpMethod.PUT, CONTRACT_PATHS).hasAuthority("contract.manage")
                        .requestMatchers(HttpMethod.DELETE, CONTRACT_PATHS).hasAuthority("contract.manage")

                        .requestMatchers(HttpMethod.GET, PRODUCT_PATHS).hasAnyAuthority("product.access", "product.manage")
                        .requestMatchers(HttpMethod.POST, PRODUCT_PATHS).hasAuthority("product.manage")
                        .requestMatchers(HttpMethod.PUT, PRODUCT_PATHS).hasAuthority("product.manage")
                        .requestMatchers(HttpMethod.DELETE, PRODUCT_PATHS).hasAuthority("product.manage")

                        .requestMatchers(HttpMethod.GET, INVENTORY_PATHS).hasAnyAuthority("inventory.access", "inventory.manage")
                        .requestMatchers(HttpMethod.POST, INVENTORY_PATHS).hasAuthority("inventory.manage")
                        .requestMatchers(HttpMethod.PUT, INVENTORY_PATHS).hasAuthority("inventory.manage")
                        .requestMatchers(HttpMethod.DELETE, INVENTORY_PATHS).hasAuthority("inventory.manage")

                        .requestMatchers(HttpMethod.GET, FINANCE_PATHS).hasAnyAuthority("finance.access", "finance.manage")
                        .requestMatchers(HttpMethod.POST, FINANCE_PATHS).hasAuthority("finance.manage")
                        .requestMatchers(HttpMethod.PUT, FINANCE_PATHS).hasAuthority("finance.manage")
                        .requestMatchers(HttpMethod.DELETE, FINANCE_PATHS).hasAuthority("finance.manage")

                        .requestMatchers(HttpMethod.GET, "/api/attachments/**").hasAnyAuthority("attachment.access", "attachment.manage")
                        .requestMatchers(HttpMethod.POST, "/api/attachments/**").hasAuthority("attachment.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/attachments/**").hasAuthority("attachment.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/attachments/**").hasAuthority("attachment.manage")

                        .requestMatchers(HttpMethod.GET, "/api/login-logs/**", "/api/audit-trails/**")
                        .hasAnyAuthority("logs.view", "logs.manage")
                        .requestMatchers(HttpMethod.POST, "/api/login-logs/**", "/api/audit-trails/**")
                        .hasAuthority("logs.manage")
                        .requestMatchers(HttpMethod.PUT, "/api/login-logs/**", "/api/audit-trails/**")
                        .hasAuthority("logs.manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/login-logs/**", "/api/audit-trails/**")
                        .hasAuthority("logs.manage")

                        .anyRequest().authenticated()
                )
                .formLogin().disable()
                .httpBasic();

        http.addFilterAfter(passwordChangeRequiredFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http
                .getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder)
                .and()
                .build();
    }

}
