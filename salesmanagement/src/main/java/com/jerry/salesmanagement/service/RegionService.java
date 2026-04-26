package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Region;

import java.util.List;

public interface RegionService {
    List<Region> getAll();
    List<Region> getProvinces();
    Region getById(Integer id);
    Region getByCodePrefix(String codePrefix);
    List<Region> getByParentCode(Long parentCode);
}
