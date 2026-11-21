package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper {
    List<Customer> selectAll();
    List<Customer> selectPage(
            @Param("customerCode") String customerCode,
            @Param("customerName") String customerName,
            @Param("taxIdentificationNumber") String taxIdentificationNumber,
            @Param("industry") String industry,
            @Param("activityStatus") String activityStatus,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    long countPage(
            @Param("customerCode") String customerCode,
            @Param("customerName") String customerName,
            @Param("taxIdentificationNumber") String taxIdentificationNumber,
            @Param("industry") String industry,
            @Param("activityStatus") String activityStatus
    );
    Customer selectByUuid(String uuid);
    Customer selectByCustomerName(String customerName);
    Customer selectByTaxIdentificationNumber(String taxIdentificationNumber);
    String selectMaxCustomerCodeByPrefix(String prefix);
    String selectMaxCustomerCodeByPrefixForUpdate(String prefix);
    int insert(Customer customer);
    int updateByUuid(Customer customer);
    int touchActivityByUuid(@Param("uuid") String uuid, @Param("activityStatus") String activityStatus);
    int updateActivityStatusByUuid(@Param("uuid") String uuid, @Param("activityStatus") String activityStatus);
    int deleteByUuid(String uuid);
}
