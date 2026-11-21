package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.dto.PageResult;

import java.util.List;

public interface CustomerService {
    List<Customer> getAll();

    PageResult<Customer> getPage(
            String customerCode,
            String customerName,
            String taxIdentificationNumber,
            String industry,
            String activityStatus,
            int page,
            int pageSize
    );

    Customer getByUuid(String uuid);

    Customer create(Customer customer);

    Customer update(Customer customer);

    void touchActivity(String uuid);

    void delete(String uuid);
}
