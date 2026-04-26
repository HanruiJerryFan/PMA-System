package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ContractTypeMapper;
import com.jerry.salesmanagement.pojo.ContractType;
import com.jerry.salesmanagement.service.ContractTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContractTypeServiceImpl implements ContractTypeService {

    @Autowired
    private ContractTypeMapper mapper;

    @Override
    public ContractType getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ContractType> getAll() {
        return mapper.selectAll();
    }
}
