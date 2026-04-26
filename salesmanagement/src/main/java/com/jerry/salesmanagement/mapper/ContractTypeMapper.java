package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ContractType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ContractTypeMapper {
    ContractType selectById(Long id);
    List<ContractType> selectAll();
}
