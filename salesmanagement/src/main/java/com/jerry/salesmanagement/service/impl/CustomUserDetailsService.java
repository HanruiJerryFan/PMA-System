package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.service.SysPermissionService;
import com.jerry.salesmanagement.service.SysRoleService;
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

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;

    public CustomUserDetailsService(
            SysUserService sysUserService,
            SysRoleService sysRoleService,
            SysPermissionService sysPermissionService
    ) {
        this.sysUserService = sysUserService;
        this.sysRoleService = sysRoleService;
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

        List<GrantedAuthority> authorities = loadAuthorities(sysUser);

        return new User(sysUser.getUsername(), sysUser.getPassword(), authorities);
    }

    public List<GrantedAuthority> loadAuthorities(SysUser sysUser) {
        if (sysUser == null || sysUser.getId() == null) {
            return List.of();
        }
        Set<String> roleSet = new LinkedHashSet<>();
        for (String roleName : sysRoleService.getRoleNamesByUserId(sysUser.getId())) {
            if (roleName != null && !roleName.isBlank()) {
                roleSet.add("ROLE_" + roleName.trim());
            }
        }

        Set<String> permissionSet = new LinkedHashSet<>();
        for (String permissionCode : sysPermissionService.getPermissionCodesByUserId(sysUser.getId())) {
            if (permissionCode != null && !permissionCode.isBlank()) {
                permissionSet.add(permissionCode.trim());
            }
        }

        List<GrantedAuthority> authorities = new ArrayList<>(roleSet.size() + permissionSet.size());
        for (String role : roleSet) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        for (String permission : permissionSet) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return authorities;
    }
}
