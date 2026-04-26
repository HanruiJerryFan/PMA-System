package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ContractClause;

import java.util.List;

public interface ContractClauseService {
    ContractClause getByUuid(String uuid);
    List<ContractClause> getByContractUuid(String contractUuid);
    ContractClause create(ContractClause clause);
    ContractClause update(ContractClause clause);
    void delete(String uuid);
}
