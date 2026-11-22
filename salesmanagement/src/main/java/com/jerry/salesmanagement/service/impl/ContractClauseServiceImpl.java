package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ClauseTypeMapper;
import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.ContractClauseMapper;
import com.jerry.salesmanagement.pojo.ContractClause;
import com.jerry.salesmanagement.service.ContractClauseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContractClauseServiceImpl implements ContractClauseService {

    @Autowired
    private ContractClauseMapper mapper;

    @Autowired(required = false)
    private ContractBasicInfoMapper contractBasicInfoMapper;

    @Autowired(required = false)
    private ClauseTypeMapper clauseTypeMapper;

    @Override
    public ContractClause getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<ContractClause> getByContractUuid(String contractUuid) {
        return mapper.selectByContractUuid(contractUuid);
    }

    @Override
    @Transactional
    public ContractClause create(ContractClause clause) {
        validate(clause);
        if (clause.getUuid() == null || clause.getUuid().isEmpty()) {
            clause.setUuid(UUID.randomUUID().toString());
        }
        mapper.insert(clause);
        return clause;
    }

    @Override
    @Transactional
    public ContractClause update(ContractClause clause) {
        ContractClause existing = mapper.selectByUuid(clause.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Contract clause does not exist");
        }
        validate(clause);
        if (!Objects.equals(existing.getContractBasicInfoId(), clause.getContractBasicInfoId())) {
            throw new IllegalArgumentException("Contract clause cannot be moved to another contract");
        }
        mapper.updateByUuid(clause);
        ContractClause updated = mapper.selectByUuid(clause.getUuid());
        return updated != null ? updated : clause;
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        mapper.deleteByUuid(uuid);
    }

    private void validate(ContractClause clause) {
        if (!StringUtils.hasText(clause.getContractBasicInfoId())) {
            throw new IllegalArgumentException("Contract is required");
        }
        clause.setContractBasicInfoId(clause.getContractBasicInfoId().trim());
        if (contractBasicInfoMapper != null
                && contractBasicInfoMapper.selectByUuid(clause.getContractBasicInfoId()) == null) {
            throw new IllegalArgumentException("Contract does not exist");
        }
        if (clause.getClauseTypeId() == null) {
            throw new IllegalArgumentException("Clause type is required");
        }
        if (clauseTypeMapper != null && clauseTypeMapper.selectById(clause.getClauseTypeId()) == null) {
            throw new IllegalArgumentException("Clause type does not exist");
        }
        clause.setClauseDescription(StringUtils.hasText(clause.getClauseDescription())
                ? clause.getClauseDescription().trim() : null);
        if (clause.getClauseAmount() != null && clause.getClauseAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Clause amount cannot be negative");
        }
    }
}
