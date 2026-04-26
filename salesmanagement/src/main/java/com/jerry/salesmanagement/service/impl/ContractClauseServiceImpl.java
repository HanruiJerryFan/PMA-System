package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ContractClauseMapper;
import com.jerry.salesmanagement.pojo.ContractClause;
import com.jerry.salesmanagement.service.ContractClauseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ContractClauseServiceImpl implements ContractClauseService {

    @Autowired
    private ContractClauseMapper mapper;

    @Override
    public ContractClause getByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Override
    public List<ContractClause> getByContractUuid(String contractUuid) {
        return mapper.selectByContractUuid(contractUuid);
    }

    @Override
    public ContractClause create(ContractClause clause) {
        if (clause.getUuid() == null || clause.getUuid().isEmpty()) {
            clause.setUuid(UUID.randomUUID().toString());
        }
        mapper.insert(clause);
        return clause;
    }

    @Override
    public ContractClause update(ContractClause clause) {
        mapper.updateByUuid(clause);
        return clause;
    }

    @Override
    public void delete(String uuid) {
        mapper.deleteByUuid(uuid);
    }
}
