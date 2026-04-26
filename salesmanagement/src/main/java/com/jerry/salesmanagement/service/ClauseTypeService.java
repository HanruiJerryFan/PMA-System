package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ClauseType;

import java.util.List;

public interface ClauseTypeService {
    ClauseType getById(Long id);
    List<ClauseType> getAll();
}
