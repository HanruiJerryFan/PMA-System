package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CustomerIndustryDictMapper {
    CustomerIndustryDict selectById(Long id);
    CustomerIndustryDict selectByName(String industryName);
    List<CustomerIndustryDict> selectAll();
    List<CustomerIndustryDict> selectActive();
    int insert(CustomerIndustryDict industryDict);
    int update(CustomerIndustryDict industryDict);
    int deleteById(Long id);
}
