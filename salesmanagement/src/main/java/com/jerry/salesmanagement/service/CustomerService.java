package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Customer;

import java.util.List;

public interface CustomerService {
    List<Customer> getAll();

    Customer getByUuid(String uuid);

    Customer create(Customer customer);

    Customer update(Customer customer);

    void touchActivity(String uuid);

    void delete(String uuid);
}
