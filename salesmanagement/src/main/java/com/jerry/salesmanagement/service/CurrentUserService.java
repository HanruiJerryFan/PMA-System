package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.SysUser;

public interface CurrentUserService {
    SysUser getCurrentUser();

    Long getCurrentUserId();

    Long requireCurrentUserId();

    boolean hasAnyAuthority(String... authorities);

    boolean isSystemOrAdmin();
}
