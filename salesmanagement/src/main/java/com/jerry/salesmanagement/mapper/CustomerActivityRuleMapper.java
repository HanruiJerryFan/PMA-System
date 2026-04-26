package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.CustomerActivityRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CustomerActivityRuleMapper {
    CustomerActivityRule selectById(Long id);
    CustomerActivityRule selectByStatusCode(String statusCode);
    List<CustomerActivityRule> selectAll();
    int insert(CustomerActivityRule rule);
    int update(CustomerActivityRule rule);
    int deleteById(Long id);
}
