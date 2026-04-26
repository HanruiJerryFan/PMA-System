package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ContractBasicInfo;

import java.util.List;

public interface ContractBasicInfoService {
    List<ContractBasicInfo> getAll();
    ContractBasicInfo getByUuid(String uuid);
    
    ContractBasicInfo create(ContractBasicInfo contractBasicInfo);
    ContractBasicInfo update(ContractBasicInfo contractBasicInfo);
    void delete(String uuid);
}
