package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.RegionMapper;
import com.jerry.salesmanagement.pojo.Region;
import com.jerry.salesmanagement.service.RegionService;
import org.apache.poi.ss.usermodel.Cell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class RegionServiceImpl implements RegionService {

    @Autowired
    private RegionMapper regionMapper;

    @Override
    public List<Region> getAll() {
        return regionMapper.selectAll();
    }

    @Override
    public List<Region> getProvinces() {
        return regionMapper.selectByLevel(1);
    }

    @Override
    public Region getById(Integer id) {
        return regionMapper.selectById(id);
    }

    @Override
    public Region getByCodePrefix(String codePrefix) {
        if (!StringUtils.hasText(codePrefix) || !codePrefix.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Region code prefix must be 4 digits");
        }
        return regionMapper.selectByAreaCodePrefix(codePrefix);
    }

    @Override
    public List<Region> getByParentCode(Long parentCode) {
        return regionMapper.selectByParentCode(parentCode);
    }
}
