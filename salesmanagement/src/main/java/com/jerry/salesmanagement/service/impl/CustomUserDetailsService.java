package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.SysPermission;
import com.jerry.salesmanagement.pojo.SysRolePermission;
import com.jerry.salesmanagement.pojo.SysUserRole;
import com.jerry.salesmanagement.service.SysPermissionService;
import com.jerry.salesmanagement.service.SysRolePermissionService;
import com.jerry.salesmanagement.service.SysRoleService;
import com.jerry.salesmanagement.service.SysUserRoleService;
import com.jerry.salesmanagement.service.SysUserService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserService sysUserService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleService sysRoleService;
    private final SysRolePermissionService sysRolePermissionService;
    private final SysPermissionService sysPermissionService;

    public CustomUserDetailsService(
            SysUserService sysUserService,
            SysUserRoleService sysUserRoleService,
            SysRoleService sysRoleService,
            SysRolePermissionService sysRolePermissionService,
            SysPermissionService sysPermissionService
    ) {
        this.sysUserService = sysUserService;
        this.sysUserRoleService = sysUserRoleService;
        this.sysRoleService = sysRoleService;
        this.sysRolePermissionService = sysRolePermissionService;
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserService.getByUsername(username);
        if (sysUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        if (!"ACTIVE".equalsIgnoreCase(sysUser.getStatus())) {
            throw new DisabledException("User status is " + sysUser.getStatus());
        }

        List<SysUserRole> userRoles = sysUserRoleService.getByUserId(sysUser.getId());
        List<String> roleList = userRoles.stream()
                .map(userRole -> "ROLE_" + sysRoleService.getById(userRole.getRoleId()).getRoleName())
                .collect(Collectors.toList());
        Set<String> permissionSet = new LinkedHashSet<>();
        for (SysUserRole userRole : userRoles) {
            List<SysRolePermission> rolePermissions = sysRolePermissionService.getByRoleId(userRole.getRoleId());
            for (SysRolePermission rolePermission : rolePermissions) {
                SysPermission permission = sysPermissionService.getById(rolePermission.getPermissionId());
                if (permission != null && permission.getPermissionCode() != null && !permission.getPermissionCode().isBlank()) {
                    permissionSet.add(permission.getPermissionCode().trim());
                }
            }
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roleList) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        for (String permission : permissionSet) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        return new User(sysUser.getUsername(), sysUser.getPassword(), authorities);
    }
}
