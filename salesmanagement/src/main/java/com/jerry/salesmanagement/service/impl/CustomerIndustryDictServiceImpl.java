package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerIndustryDictMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import com.jerry.salesmanagement.service.CustomerIndustryDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class CustomerIndustryDictServiceImpl implements CustomerIndustryDictService {

    @Autowired
    private CustomerIndustryDictMapper customerIndustryDictMapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

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
    @Transactional
    public CustomerIndustryDict create(CustomerIndustryDict industryDict) {
        validate(industryDict);
        customerIndustryDictMapper.insert(industryDict);
        return industryDict;
    }

    @Override
    @Transactional
    public CustomerIndustryDict update(CustomerIndustryDict industryDict) {
        if (industryDict.getId() == null) {
            throw new IllegalArgumentException("Industry id is required");
        }
        CustomerIndustryDict existing = customerIndustryDictMapper.selectById(industryDict.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Industry does not exist");
        }
        validate(industryDict);
        if (!Objects.equals(existing.getIndustryName(), industryDict.getIndustryName())
                && referenceCheckMapper != null
                && referenceCheckMapper.countCustomersByIndustry(existing.getIndustryName()) > 0) {
            throw new IllegalArgumentException("Industry is referenced by customers and cannot be renamed");
        }
        customerIndustryDictMapper.update(industryDict);
        return customerIndustryDictMapper.selectById(industryDict.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CustomerIndustryDict existing = customerIndustryDictMapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "客户行业",
                    new ReferenceCheckMapper.ReferenceUsage("客户", referenceCheckMapper.countCustomersByIndustry(existing.getIndustryName()))
            );
        }
        customerIndustryDictMapper.deleteById(id);
    }

    private void validate(CustomerIndustryDict industryDict) {
        if (!StringUtils.hasText(industryDict.getIndustryName())) {
            throw new IllegalArgumentException("Industry name is required");
        }
        industryDict.setIndustryName(industryDict.getIndustryName().trim());
        if (industryDict.getIsActive() == null) {
            throw new IllegalArgumentException("Industry active flag is required");
        }
        CustomerIndustryDict existingByName = customerIndustryDictMapper.selectByName(industryDict.getIndustryName());
        if (existingByName != null && !Objects.equals(existingByName.getId(), industryDict.getId())) {
            throw new IllegalArgumentException("Industry name already exists");
        }
    }
}
