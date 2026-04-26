package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.SystemConfig;

import java.util.List;

public interface SystemConfigService {
    SystemConfig getByKey(String configKey);
    List<SystemConfig> getAll();
    SystemConfig createOrUpdate(SystemConfig systemConfig);
    void delete(String configKey);
    String getString(String configKey, String defaultValue);
    int getInt(String configKey, int defaultValue);
}
