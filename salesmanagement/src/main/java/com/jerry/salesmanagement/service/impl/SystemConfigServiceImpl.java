package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.SystemConfigMapper;
import com.jerry.salesmanagement.pojo.SystemConfig;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public SystemConfig getByKey(String configKey) {
        return systemConfigMapper.selectByKey(configKey);
    }

    @Override
    public List<SystemConfig> getAll() {
        return systemConfigMapper.selectAll();
    }

    @Override
    public SystemConfig createOrUpdate(SystemConfig systemConfig) {
        validate(systemConfig);
        SystemConfig existing = systemConfigMapper.selectByKey(systemConfig.getConfigKey());
        if (existing == null) {
            systemConfigMapper.insert(systemConfig);
        } else {
            systemConfigMapper.updateByKey(systemConfig);
        }
        return systemConfigMapper.selectByKey(systemConfig.getConfigKey());
    }

    @Override
    public void delete(String configKey) {
        systemConfigMapper.deleteByKey(configKey);
    }

    @Override
    public String getString(String configKey, String defaultValue) {
        SystemConfig config = systemConfigMapper.selectByKey(configKey);
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue().trim();
    }

    @Override
    public int getInt(String configKey, int defaultValue) {
        String value = getString(configKey, null);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void validate(SystemConfig systemConfig) {
        if (systemConfig == null || !StringUtils.hasText(systemConfig.getConfigKey())) {
            throw new IllegalArgumentException("Config key is required");
        }
        if (!StringUtils.hasText(systemConfig.getConfigValue())) {
            throw new IllegalArgumentException("Config value is required");
        }
    }
}
