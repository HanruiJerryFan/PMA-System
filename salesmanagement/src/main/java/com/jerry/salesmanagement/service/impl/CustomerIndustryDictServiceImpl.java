package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerIndustryDictMapper;
import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import com.jerry.salesmanagement.service.CustomerIndustryDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CustomerIndustryDictServiceImpl implements CustomerIndustryDictService {

    @Autowired
    private CustomerIndustryDictMapper customerIndustryDictMapper;

    @Override
    public CustomerIndustryDict getById(Long id) {
        return customerIndustryDictMapper.selectById(id);
    }

    @Override
    public CustomerIndustryDict getByName(String industryName) {
        return customerIndustryDictMapper.selectByName(industryName);
    }

    @Override
    public List<CustomerIndustryDict> getAll() {
        return customerIndustryDictMapper.selectAll();
    }

    @Override
    public CustomerIndustryDict create(CustomerIndustryDict industryDict) {
        validate(industryDict);
        customerIndustryDictMapper.insert(industryDict);
        return industryDict;
    }

    @Override
    public CustomerIndustryDict update(CustomerIndustryDict industryDict) {
        validate(industryDict);
        customerIndustryDictMapper.update(industryDict);
        return industryDict;
    }

    @Override
    public void delete(Long id) {
        customerIndustryDictMapper.deleteById(id);
    }

    private void validate(CustomerIndustryDict industryDict) {
        if (!StringUtils.hasText(industryDict.getIndustryName())) {
            throw new IllegalArgumentException("Industry name is required");
        }
        if (industryDict.getIsActive() == null) {
            throw new IllegalArgumentException("Industry active flag is required");
        }
    }
}
