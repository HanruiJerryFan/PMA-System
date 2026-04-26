package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.CustomerIndustryDictMapper;
import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.mapper.RegionMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.pojo.Region;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Pattern CUSTOMER_CODE_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern CUSTOMER_TYPE_CODE_PATTERN = Pattern.compile("^[0-9]$");
    private static final Set<String> ACTIVITY_STATUS_VALUES = Set.of("ACTIVE", "NORMAL", "INACTIVE", "DORMANT");

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private CustomerTypeMapper customerTypeMapper;

    @Autowired
    private CustomerIndustryDictMapper customerIndustryDictMapper;

    @Autowired
    private CodeSequenceService codeSequenceService;

    @Override
    public List<Customer> getAll() {
        return customerMapper.selectAll();
    }

    @Override
    public Customer getByUuid(String uuid) {
        return customerMapper.selectByUuid(uuid);
    }

    @Override
    @Transactional
    public Customer create(Customer customer) {
        customer.setCustomerCode(generateCustomerCode(customer.getRegionId(), customer.getCustomerTypeId()));
        customer.setActivityStatus("ACTIVE");
        customer.setLastActiveAt(new Date());
        validateCustomer(customer);
        if (!StringUtils.hasText(customer.getUuid())) {
            customer.setUuid(UUID.randomUUID().toString());
        }
        customerMapper.insert(customer);
        return customer;
    }

    @Override
    @Transactional
    public Customer update(Customer customer) {
        Customer existing = customerMapper.selectByUuid(customer.getUuid());
        if (existing == null) {
            throw new IllegalArgumentException("Customer does not exist");
        }

        if (StringUtils.hasText(existing.getCustomerCode())) {
            customer.setCustomerCode(existing.getCustomerCode());
        } else {
            Long regionId = customer.getRegionId() != null ? customer.getRegionId() : existing.getRegionId();
            Long customerTypeId = customer.getCustomerTypeId() != null
                    ? customer.getCustomerTypeId()
                    : existing.getCustomerTypeId();
            customer.setCustomerCode(generateCustomerCode(regionId, customerTypeId));
        }

        customer.setActivityStatus(existing.getActivityStatus());
        customer.setLastActiveAt(existing.getLastActiveAt());
        validateCustomer(customer);
        customerMapper.updateByUuid(customer);
        return customer;
    }

    @Override
    public void touchActivity(String uuid) {
        if (!StringUtils.hasText(uuid)) {
            throw new IllegalArgumentException("Customer is required");
        }
        int updated = customerMapper.touchActivityByUuid(uuid.trim(), "ACTIVE");
        if (updated == 0) {
            throw new IllegalArgumentException("Customer does not exist");
        }
    }

    @Override
    public void delete(String uuid) {
        customerMapper.deleteByUuid(uuid);
    }

    private void validateCustomer(Customer customer) {
        if (!StringUtils.hasText(customer.getCustomerName())) {
            throw new IllegalArgumentException("Customer name is required");
        }
        Customer existingByName = customerMapper.selectByCustomerName(customer.getCustomerName().trim());
        if (existingByName != null && !sameCustomer(existingByName, customer)) {
            throw new IllegalArgumentException("Customer name already exists");
        }
        if (customer.getRegionId() == null) {
            throw new IllegalArgumentException("Region is required");
        }
        if (customer.getCustomerTypeId() == null) {
            throw new IllegalArgumentException("Customer type is required");
        }
        resolveCustomerTypeCode(customer.getCustomerTypeId());
        if (!StringUtils.hasText(customer.getCustomerCode())) {
            throw new IllegalArgumentException("Customer code is required");
        }
        if (!CUSTOMER_CODE_PATTERN.matcher(customer.getCustomerCode().trim()).matches()) {
            throw new IllegalArgumentException("Customer code must be a 9-digit system ID");
        }
        if (StringUtils.hasText(customer.getTaxIdentificationNumber())
                && customer.getTaxIdentificationNumber().trim().length() != 18) {
            throw new IllegalArgumentException("Tax identification number must be 18 characters");
        }
        if (StringUtils.hasText(customer.getTaxIdentificationNumber())) {
            Customer existingByTaxId = customerMapper.selectByTaxIdentificationNumber(customer.getTaxIdentificationNumber().trim());
            if (existingByTaxId != null && !sameCustomer(existingByTaxId, customer)) {
                throw new IllegalArgumentException("Tax identification number already exists");
            }
        }
        if (StringUtils.hasText(customer.getIndustry())
                && !industryExists(customer.getIndustry().trim())) {
            throw new IllegalArgumentException("Industry must use a configured dictionary value");
        }
        if (!StringUtils.hasText(customer.getActivityStatus())) {
            throw new IllegalArgumentException("Activity status is required");
        }
        if (!ACTIVITY_STATUS_VALUES.contains(customer.getActivityStatus().trim())) {
            throw new IllegalArgumentException("Activity status must be ACTIVE, NORMAL, INACTIVE, or DORMANT");
        }
    }

    private String generateCustomerCode(Long regionId, Long customerTypeId) {
        if (regionId == null) {
            throw new IllegalArgumentException("Region is required");
        }

        Region region = regionMapper.selectById(Math.toIntExact(regionId));
        if (region == null || region.getId() == null) {
            throw new IllegalArgumentException("Region does not exist");
        }
        if (region.getAreaCode() == null) {
            throw new IllegalArgumentException("Region area code is required");
        }
        String regionCode = String.valueOf(region.getAreaCode());
        if (regionCode.length() < 4) {
            throw new IllegalArgumentException("Region area code must have at least 4 digits");
        }

        String customerTypeCode = resolveCustomerTypeCode(customerTypeId);
        String prefix = regionCode.substring(0, 4) + customerTypeCode;
        String maxCustomerCode = customerMapper.selectMaxCustomerCodeByPrefix(prefix);
        int currentSequence = 0;
        if (StringUtils.hasText(maxCustomerCode)) {
            currentSequence = Integer.parseInt(maxCustomerCode.substring(5));
        }
        int nextSequence = codeSequenceService.nextValue(
                "customer:" + prefix,
                currentSequence,
                9999,
                "Customer code sequence for prefix " + prefix
        );
        return prefix + String.format("%04d", nextSequence);
    }

    private String resolveCustomerTypeCode(Long customerTypeId) {
        if (customerTypeId == null) {
            throw new IllegalArgumentException("Customer type is required");
        }

        CustomerType customerType = customerTypeMapper.selectById(customerTypeId);
        if (customerType == null) {
            throw new IllegalArgumentException("Customer type does not exist");
        }
        if (!StringUtils.hasText(customerType.getTypeCode())) {
            throw new IllegalArgumentException("Customer type code is required");
        }
        String customerTypeCode = customerType.getTypeCode().trim();
        if (!CUSTOMER_TYPE_CODE_PATTERN.matcher(customerTypeCode).matches()) {
            throw new IllegalArgumentException("Customer type code must be a single digit");
        }
        return customerTypeCode;
    }

    private boolean industryExists(String industryName) {
        CustomerIndustryDict industryDict = customerIndustryDictMapper.selectByName(industryName);
        return industryDict != null && Boolean.TRUE.equals(industryDict.getIsActive());
    }

    private boolean sameCustomer(Customer existing, Customer incoming) {
        return existing != null
                && incoming != null
                && StringUtils.hasText(existing.getUuid())
                && existing.getUuid().equals(incoming.getUuid());
    }
}
