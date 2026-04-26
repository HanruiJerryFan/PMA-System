package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.service.CustomerTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerTypeServiceImpl implements CustomerTypeService {

    @Autowired
    private CustomerTypeMapper mapper;

    @Override
    public CustomerType getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<CustomerType> getAll() {
        return mapper.selectAll();
    }
}
