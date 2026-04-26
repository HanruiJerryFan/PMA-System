package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ContractType;

import java.util.List;

public interface ContractTypeService {
    ContractType getById(Long id);
    List<ContractType> getAll();
}
