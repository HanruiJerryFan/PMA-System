package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.CustomerContact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerContactMapper {
    List<CustomerContact> selectAll();

    CustomerContact selectByUuid(String uuid);

    int insert(CustomerContact contact);

    int updateByUuid(CustomerContact contact);

    int deleteByUuid(@Param("uuid") String uuid);
}
