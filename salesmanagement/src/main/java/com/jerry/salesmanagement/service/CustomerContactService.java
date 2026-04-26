package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.CustomerContact;

import java.util.List;

public interface CustomerContactService {
    List<CustomerContact> getAll();
    CustomerContact getByUuid(String uuid);
    CustomerContact create(CustomerContact contact);
    CustomerContact update(CustomerContact contact);
    void delete(String uuid);
}
