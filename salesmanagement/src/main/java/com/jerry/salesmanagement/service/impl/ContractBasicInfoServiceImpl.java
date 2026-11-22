package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.ContractTypeMapper;
import com.jerry.salesmanagement.mapper.CustomerContactMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import com.jerry.salesmanagement.pojo.CustomerContact;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.ContractBasicInfoService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContractBasicInfoServiceImpl implements ContractBasicInfoService {

    @Autowired
    private ContractBasicInfoMapper contractbasicInfoMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Autowired(required = false)
    private ProjectMapper projectMapper;

    @Autowired(required = false)
    private ContractTypeMapper contractTypeMapper;

    @Autowired(required = false)
    private CustomerMapper customerMapper;

    @Autowired(required = false)
    private CustomerContactMapper customerContactMapper;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private AttachmentService attachmentService;

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
        if (currentUserService != null) {
            contractBasicInfo.setCreateUser(currentUserService.requireCurrentUserId());
        }
        contractbasicInfoMapper.insert(contractBasicInfo);
        customerService.touchActivity(contractBasicInfo.getClientId());
        return contractBasicInfo;
    }

    @Override
    @Transactional
    public ContractBasicInfo update(ContractBasicInfo contractBasicInfo) {
        ContractBasicInfo existing = contractbasicInfoMapper.selectByUuid(contractBasicInfo.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Contract does not exist");
        }
        validateContract(contractBasicInfo);
        if (currentUserService != null) {
            contractBasicInfo.setUpdateUser(currentUserService.requireCurrentUserId());
        }
        contractbasicInfoMapper.updateByUuid(contractBasicInfo);
        customerService.touchActivity(contractBasicInfo.getClientId());
        ContractBasicInfo updated = contractbasicInfoMapper.selectByUuid(contractBasicInfo.getUuid());
        return updated != null ? updated : contractBasicInfo;
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        ContractBasicInfo existing = StringUtils.hasText(uuid) ? contractbasicInfoMapper.selectByUuid(uuid) : null;
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "合同",
                    new ReferenceCheckMapper.ReferenceUsage("合同条款", referenceCheckMapper.countContractClausesByContractUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("出入库单", referenceCheckMapper.countWarehouseDocsByContractNumber(existing.getContractNumber())),
                    new ReferenceCheckMapper.ReferenceUsage("库存流水", referenceCheckMapper.countInventoryTransactionsByContractUuid(uuid))
            );
        }
        contractbasicInfoMapper.deleteByUuid(uuid);
        if (attachmentService != null) {
            attachmentService.deleteByBusiness("contracts", uuid);
        }
    }

    private void validateContract(ContractBasicInfo contractBasicInfo) {
        if (!StringUtils.hasText(contractBasicInfo.getProjectBasicInfoId())) {
            throw new IllegalArgumentException("Project is required");
        }
        contractBasicInfo.setProjectBasicInfoId(contractBasicInfo.getProjectBasicInfoId().trim());
        if (projectMapper != null && projectMapper.selectByUuid(contractBasicInfo.getProjectBasicInfoId()) == null) {
            throw new IllegalArgumentException("Project does not exist");
        }
        if (contractBasicInfo.getContractTypeId() == null) {
            throw new IllegalArgumentException("Contract type is required");
        }
        if (contractTypeMapper != null && contractTypeMapper.selectById(contractBasicInfo.getContractTypeId()) == null) {
            throw new IllegalArgumentException("Contract type does not exist");
        }
        if (!StringUtils.hasText(contractBasicInfo.getContractNumber())) {
            throw new IllegalArgumentException("Contract number is required");
        }
        contractBasicInfo.setContractNumber(contractBasicInfo.getContractNumber().trim());
        for (ContractBasicInfo other : contractbasicInfoMapper.selectAll()) {
            if (other != null
                    && !Objects.equals(other.getUuid(), contractBasicInfo.getUuid())
                    && other.getContractNumber() != null
                    && other.getContractNumber().trim().equalsIgnoreCase(contractBasicInfo.getContractNumber())) {
                throw new IllegalArgumentException("Contract number already exists");
            }
        }
        if (!StringUtils.hasText(contractBasicInfo.getClientId())) {
            throw new IllegalArgumentException("Customer is required");
        }
        contractBasicInfo.setClientId(contractBasicInfo.getClientId().trim());
        if (customerMapper != null && customerMapper.selectByUuid(contractBasicInfo.getClientId()) == null) {
            throw new IllegalArgumentException("Customer does not exist");
        }
        contractBasicInfo.setContactId(StringUtils.hasText(contractBasicInfo.getContactId())
                ? contractBasicInfo.getContactId().trim() : null);
        if (contractBasicInfo.getContactId() != null && customerContactMapper != null) {
            CustomerContact contact = customerContactMapper.selectByUuid(contractBasicInfo.getContactId());
            if (contact == null) {
                throw new IllegalArgumentException("Customer contact does not exist");
            }
            if (!Objects.equals(contact.getClientId(), contractBasicInfo.getClientId())) {
                throw new IllegalArgumentException("Customer contact does not belong to selected customer");
            }
        }
        if (!StringUtils.hasText(contractBasicInfo.getSubItemNo())) {
            contractBasicInfo.setSubItemNo("01");
        } else {
            contractBasicInfo.setSubItemNo(contractBasicInfo.getSubItemNo().trim());
        }
        if (contractBasicInfo.getSubItemNo().length() > 2) {
            throw new IllegalArgumentException("Sub-item number must be at most 2 characters");
        }
        contractBasicInfo.setSubItemContent(StringUtils.hasText(contractBasicInfo.getSubItemContent())
                ? contractBasicInfo.getSubItemContent().trim() : null);
        if (contractBasicInfo.getContractAmount() != null
                && contractBasicInfo.getContractAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Contract amount cannot be negative");
        }
    }
}
