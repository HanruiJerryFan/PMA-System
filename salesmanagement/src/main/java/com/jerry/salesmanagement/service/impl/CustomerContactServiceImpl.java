package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerContactMapper;
import com.jerry.salesmanagement.pojo.CustomerContact;
import com.jerry.salesmanagement.service.CustomerContactService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerContactServiceImpl implements CustomerContactService {

    @Autowired
    private CustomerContactMapper customerContactMapper;

    @Autowired
    private CustomerService customerService;

    @Override
    public List<CustomerContact> getAll() {
        return customerContactMapper.selectAll();
    }

    @Override
    public CustomerContact getByUuid(String uuid) {
        return customerContactMapper.selectByUuid(uuid);
    }

    @Override
    @Transactional
    public CustomerContact create(CustomerContact customerContact) {
        validate(customerContact);
        if (!StringUtils.hasText(customerContact.getUuid())) {
            customerContact.setUuid(UUID.randomUUID().toString());
        }
        customerContactMapper.insert(customerContact);
        customerService.touchActivity(customerContact.getClientId());
        return customerContact;
    }

    @Override
    @Transactional
    public CustomerContact update(CustomerContact customerContact) {
        validate(customerContact);
        customerContactMapper.updateByUuid(customerContact);
        customerService.touchActivity(customerContact.getClientId());
        return customerContact;
    }

    @Override
    public void delete(String uuid) {
        customerContactMapper.deleteByUuid(uuid);
    }

    private void validate(CustomerContact customerContact) {
        if (!StringUtils.hasText(customerContact.getClientId())) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (!StringUtils.hasText(customerContact.getContactName())) {
            throw new IllegalArgumentException("Contact name is required");
        }
        if (StringUtils.hasText(customerContact.getEmail()) && !customerContact.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email format is invalid");
        }
    }
}
