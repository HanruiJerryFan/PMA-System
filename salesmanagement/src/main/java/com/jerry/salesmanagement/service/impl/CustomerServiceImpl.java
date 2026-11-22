package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.CustomerIndustryDictMapper;
import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.mapper.RegionMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.pojo.Region;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
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

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Autowired(required = false)
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private AttachmentService attachmentService;

    @Override
    public List<Customer> getAll() {
        return customerMapper.selectAll();
    }

    @Override
    public PageResult<Customer> getPage(
            String customerCode,
            String customerName,
            String taxIdentificationNumber,
            String industry,
            String activityStatus,
            int page,
            int pageSize
    ) {
        return PageResult.paginate(
                page,
                pageSize,
                () -> customerMapper.countPage(
                        customerCode, customerName, taxIdentificationNumber, industry, activityStatus),
                (offset, limit) -> customerMapper.selectPage(
                        customerCode, customerName, taxIdentificationNumber, industry, activityStatus, offset, limit)
        );
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
        if (currentUserService != null) {
            customer.setCreateUser(currentUserService.requireCurrentUserId());
        }
        validateCustomer(customer, null);
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
            if (!Objects.equals(existing.getRegionId(), customer.getRegionId())
                    || !Objects.equals(existing.getCustomerTypeId(), customer.getCustomerTypeId())) {
                throw new IllegalArgumentException("客户编码已生成，创建后不可修改所属区域或客户类型");
            }
            customer.setCustomerCode(existing.getCustomerCode());
        } else {
            Long regionId = customer.getRegionId() != null ? customer.getRegionId() : existing.getRegionId();
            Long customerTypeId = customer.getCustomerTypeId() != null
                    ? customer.getCustomerTypeId()
                    : existing.getCustomerTypeId();
            customer.setRegionId(regionId);
            customer.setCustomerTypeId(customerTypeId);
            customer.setCustomerCode(generateCustomerCode(regionId, customerTypeId));
        }

        customer.setActivityStatus(existing.getActivityStatus());
        customer.setLastActiveAt(existing.getLastActiveAt());
        if (currentUserService != null) {
            customer.setUpdateUser(currentUserService.requireCurrentUserId());
        }
        validateCustomer(customer, existing);
        customerMapper.updateByUuid(customer);
        Customer updated = customerMapper.selectByUuid(customer.getUuid());
        return updated != null ? updated : customer;
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
    @Transactional
    public void delete(String uuid) {
        Customer existing = customerMapper.selectByUuid(uuid);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "客户",
                    new ReferenceCheckMapper.ReferenceUsage("客户联系人", referenceCheckMapper.countCustomerContactsByCustomerUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("项目", referenceCheckMapper.countProjectsByCustomerUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("合同", referenceCheckMapper.countContractsByCustomerUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("财务凭证", referenceCheckMapper.countFinanceVouchersByCustomerUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("出入库单", referenceCheckMapper.countWarehouseDocsByCustomerUuid(uuid)),
                    new ReferenceCheckMapper.ReferenceUsage("物料历史价格", referenceCheckMapper.countProductHistoryPricesByCustomerUuid(uuid))
            );
        }
        String code = existing.getCustomerCode();
        if (!StringUtils.hasText(code) || !CUSTOMER_CODE_PATTERN.matcher(code).matches()) {
            customerMapper.deleteByUuid(uuid);
            if (attachmentService != null) {
                attachmentService.deleteByBusiness("customers", uuid);
            }
            return;
        }
        String prefix = code.substring(0, 5);
        codeSequenceService.deleteAndReclaimTail("customer:" + prefix,
                () -> customerMapper.deleteByUuid(uuid),
                () -> {
                    String maxCode = customerMapper.selectMaxCustomerCodeByPrefixForUpdate(prefix);
                    return maxCode == null ? 0 : Integer.parseInt(maxCode.substring(5));
                });
        if (attachmentService != null) {
            attachmentService.deleteByBusiness("customers", uuid);
        }
    }

    private void validateCustomer(Customer customer, Customer existing) {
        if (!StringUtils.hasText(customer.getCustomerName())) {
            throw new IllegalArgumentException("Customer name is required");
        }
        customer.setCustomerName(customer.getCustomerName().trim());
        customer.setRegisterAddress(trimToNull(customer.getRegisterAddress()));
        customer.setOfficeAddress(trimToNull(customer.getOfficeAddress()));
        customer.setTaxIdentificationNumber(trimToNull(customer.getTaxIdentificationNumber()));
        customer.setBankName(trimToNull(customer.getBankName()));
        customer.setBankAccount(trimToNull(customer.getBankAccount()));
        customer.setIndustry(trimToNull(customer.getIndustry()));
        customer.setRemark(trimToNull(customer.getRemark()));

        Customer existingByName = customerMapper.selectByCustomerName(customer.getCustomerName());
        if (existingByName != null && !sameCustomer(existingByName, customer)) {
            throw new IllegalArgumentException("Customer name already exists");
        }
        if (customer.getRegionId() == null) {
            throw new IllegalArgumentException("Region is required");
        }
        if (regionMapper != null) {
            Region region = regionMapper.selectById(Math.toIntExact(customer.getRegionId()));
            if (region == null || region.getId() == null) {
                throw new IllegalArgumentException("Region does not exist");
            }
        }
        if (customer.getCustomerTypeId() == null) {
            throw new IllegalArgumentException("Customer type is required");
        }
        resolveCustomerTypeCode(customer.getCustomerTypeId());
        if (!StringUtils.hasText(customer.getCustomerCode())) {
            throw new IllegalArgumentException("Customer code is required");
        }
        customer.setCustomerCode(customer.getCustomerCode().trim());
        if (!CUSTOMER_CODE_PATTERN.matcher(customer.getCustomerCode()).matches()) {
            throw new IllegalArgumentException("Customer code must be a 9-digit system ID");
        }
        if (customer.getTaxIdentificationNumber() != null
                && customer.getTaxIdentificationNumber().length() != 18) {
            throw new IllegalArgumentException("Tax identification number must be 18 characters");
        }
        if (customer.getTaxIdentificationNumber() != null) {
            Customer existingByTaxId = customerMapper.selectByTaxIdentificationNumber(customer.getTaxIdentificationNumber());
            if (existingByTaxId != null && !sameCustomer(existingByTaxId, customer)) {
                throw new IllegalArgumentException("Tax identification number already exists");
            }
        }
        if (customer.getIndustry() != null) {
            boolean unchangedIndustry = existing != null && Objects.equals(customer.getIndustry(), trimToNull(existing.getIndustry()));
            if (!unchangedIndustry && !industryExists(customer.getIndustry())) {
                throw new IllegalArgumentException("Industry must use a configured dictionary value");
            }
        }
        if (!StringUtils.hasText(customer.getActivityStatus())) {
            throw new IllegalArgumentException("Activity status is required");
        }
        if (!ACTIVITY_STATUS_VALUES.contains(customer.getActivityStatus().trim())) {
            throw new IllegalArgumentException("Activity status must be ACTIVE, NORMAL, INACTIVE, or DORMANT");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
        int nextSequence = codeSequenceService.nextValue(
                "customer:" + prefix,
                () -> {
                    String maxCode = customerMapper.selectMaxCustomerCodeByPrefixForUpdate(prefix);
                    return maxCode == null ? 0 : Integer.parseInt(maxCode.substring(5));
                },
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
