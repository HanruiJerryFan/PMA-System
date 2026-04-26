package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import com.jerry.salesmanagement.service.ContractBasicInfoService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContractBasicInfoServiceImpl implements ContractBasicInfoService {

    @Autowired
    private ContractBasicInfoMapper contractbasicInfoMapper;

    @Autowired
    private CustomerService customerService;

    @Override
    public List<ContractBasicInfo> getAll() {
        return contractbasicInfoMapper.selectAll();
    }

    @Override
    public ContractBasicInfo getByUuid(String uuid) {
        return contractbasicInfoMapper.selectByUuid(uuid);
    }

    @Override
    @Transactional
    public ContractBasicInfo create(ContractBasicInfo contractBasicInfo) {
        if (contractBasicInfo.getUuid() == null || contractBasicInfo.getUuid().isEmpty()) {
            contractBasicInfo.setUuid(UUID.randomUUID().toString());
        }
        validateContract(contractBasicInfo);
        contractbasicInfoMapper.insert(contractBasicInfo);
        customerService.touchActivity(contractBasicInfo.getClientId());
        return contractBasicInfo;
    }

    @Override
    @Transactional
    public ContractBasicInfo update(ContractBasicInfo contractBasicInfo) {
        validateContract(contractBasicInfo);
        contractbasicInfoMapper.updateByUuid(contractBasicInfo);
        customerService.touchActivity(contractBasicInfo.getClientId());
        return contractBasicInfo;
    }

    @Override
    public void delete(String uuid) {
        contractbasicInfoMapper.deleteByUuid(uuid);
    }

    private void validateContract(ContractBasicInfo contractBasicInfo) {
        if (contractBasicInfo.getProjectBasicInfoId() == null || contractBasicInfo.getProjectBasicInfoId().isBlank()) {
            throw new IllegalArgumentException("Project is required");
        }
        if (contractBasicInfo.getContractTypeId() == null) {
            throw new IllegalArgumentException("Contract type is required");
        }
        if (contractBasicInfo.getContractNumber() == null || contractBasicInfo.getContractNumber().isBlank()) {
            throw new IllegalArgumentException("Contract number is required");
        }
        if (contractBasicInfo.getClientId() == null || contractBasicInfo.getClientId().isBlank()) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (contractBasicInfo.getSubItemNo() == null || contractBasicInfo.getSubItemNo().isBlank()) {
            contractBasicInfo.setSubItemNo("01");
        }
    }
}
