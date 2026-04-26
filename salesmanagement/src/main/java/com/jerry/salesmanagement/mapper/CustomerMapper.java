package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper {
    List<Customer> selectAll();
    Customer selectByUuid(String uuid);
    Customer selectByCustomerName(String customerName);
    Customer selectByTaxIdentificationNumber(String taxIdentificationNumber);
    String selectMaxCustomerCodeByPrefix(String prefix);
    int insert(Customer customer);
    int updateByUuid(Customer customer);
    int touchActivityByUuid(@Param("uuid") String uuid, @Param("activityStatus") String activityStatus);
    int updateActivityStatusByUuid(@Param("uuid") String uuid, @Param("activityStatus") String activityStatus);
    int deleteByUuid(String uuid);
}
