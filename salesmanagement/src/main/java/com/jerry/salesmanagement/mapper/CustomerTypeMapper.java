package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.CustomerType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CustomerTypeMapper {
    CustomerType selectById(Long id);
    List<CustomerType> selectAll();
}
