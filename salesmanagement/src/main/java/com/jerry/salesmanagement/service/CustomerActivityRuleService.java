package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.CustomerActivityRule;

import java.util.List;

public interface CustomerActivityRuleService {
    CustomerActivityRule getById(Long id);
    List<CustomerActivityRule> getAll();
    CustomerActivityRule create(CustomerActivityRule rule);
    CustomerActivityRule update(CustomerActivityRule rule);
    void delete(Long id);
    String resolveStatusByIdleDays(Integer idleDays);
    int refreshAllCustomerActivityStatuses();
}
