package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.CustomerIndustryDict;

import java.util.List;

public interface CustomerIndustryDictService {
    CustomerIndustryDict getById(Long id);
    CustomerIndustryDict getByName(String industryName);
    List<CustomerIndustryDict> getAll();
    CustomerIndustryDict create(CustomerIndustryDict industryDict);
    CustomerIndustryDict update(CustomerIndustryDict industryDict);
    void delete(Long id);
}
