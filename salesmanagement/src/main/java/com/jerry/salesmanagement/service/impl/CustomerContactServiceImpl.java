package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerContactMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.CustomerContact;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerContactService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CustomerContactServiceImpl implements CustomerContactService {

    @Autowired
    private CustomerContactMapper customerContactMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Autowired(required = false)
    private CustomerMapper customerMapper;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

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
        if (currentUserService != null) {
            customerContact.setCreateUser(currentUserService.requireCurrentUserId());
        }
        customerContactMapper.insert(customerContact);
        customerService.touchActivity(customerContact.getClientId());
        return customerContact;
    }

    @Override
    @Transactional
    public CustomerContact update(CustomerContact customerContact) {
        CustomerContact existing = customerContactMapper.selectByUuid(customerContact.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Customer contact does not exist");
        }
        validate(customerContact);
        if (!Objects.equals(existing.getClientId(), customerContact.getClientId())) {
            throw new IllegalArgumentException("Customer contact cannot be moved to another customer");
        }
        if (currentUserService != null) {
            customerContact.setUpdateUser(currentUserService.requireCurrentUserId());
        }
        customerContactMapper.updateByUuid(customerContact);
        customerService.touchActivity(customerContact.getClientId());
        CustomerContact updated = customerContactMapper.selectByUuid(customerContact.getUuid());
        return updated != null ? updated : customerContact;
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        CustomerContact existing = customerContactMapper.selectByUuid(uuid);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "客户联系人",
                    new ReferenceCheckMapper.ReferenceUsage("合同", referenceCheckMapper.countContractsByContactUuid(uuid))
            );
        }
        customerContactMapper.deleteByUuid(uuid);
    }

    private void validate(CustomerContact customerContact) {
        if (!StringUtils.hasText(customerContact.getClientId())) {
            throw new IllegalArgumentException("Customer is required");
        }
        customerContact.setClientId(customerContact.getClientId().trim());
        if (customerMapper != null && customerMapper.selectByUuid(customerContact.getClientId()) == null) {
            throw new IllegalArgumentException("Customer does not exist");
        }
        if (!StringUtils.hasText(customerContact.getContactName())) {
            throw new IllegalArgumentException("Contact name is required");
        }
        customerContact.setContactName(customerContact.getContactName().trim());
        customerContact.setContactPosition(trimToNull(customerContact.getContactPosition()));
        customerContact.setContactPhone(trimToNull(customerContact.getContactPhone()));
        customerContact.setWechat(trimToNull(customerContact.getWechat()));
        customerContact.setEmail(trimToNull(customerContact.getEmail()));
        customerContact.setQq(trimToNull(customerContact.getQq()));
        customerContact.setRemark(trimToNull(customerContact.getRemark()));
        if (customerContact.getEmail() != null && !customerContact.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email format is invalid");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
