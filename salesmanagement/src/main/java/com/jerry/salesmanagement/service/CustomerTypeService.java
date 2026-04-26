package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.CustomerType;

import java.util.List;

public interface CustomerTypeService {
    CustomerType getById(Long id);
    List<CustomerType> getAll();
}
