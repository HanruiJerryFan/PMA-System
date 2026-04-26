package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ClauseTypeMapper;
import com.jerry.salesmanagement.pojo.ClauseType;
import com.jerry.salesmanagement.service.ClauseTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClauseTypeServiceImpl implements ClauseTypeService {

    @Autowired
    private ClauseTypeMapper mapper;

    @Override
    public ClauseType getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ClauseType> getAll() {
        return mapper.selectAll();
    }
}
