package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ContractClause;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ContractClauseMapper {
    ContractClause selectByUuid(String uuid);
    List<ContractClause> selectByContractUuid(String contractUuid);
    int insert(ContractClause clause);
    int updateByUuid(ContractClause clause);
    int deleteByUuid(String uuid);
}
