package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductHistoryPriceMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductHistoryPrice;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.ProductHistoryPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductHistoryPriceServiceImpl implements ProductHistoryPriceService {

    private static final java.util.Set<String> SUPPLIER_CUSTOMER_TYPES = java.util.Set.of("合作伙伴", "供应商");

    @Autowired
    private ProductHistoryPriceMapper mapper;

    @Autowired
    private MaterialMasterMapper materialMasterMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerTypeMapper customerTypeMapper;

    @Autowired
    private CustomerService customerService;

    @Override
    public ProductHistoryPrice getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ProductHistoryPrice> getAll() {
        return mapper.selectAll();
    }

    @Override
    public List<ProductHistoryPrice> getByProductUuid(String productUuid) {
        return mapper.selectByProductUuid(productUuid);
    }

    @Override
    @Transactional
    public ProductHistoryPrice create(ProductHistoryPrice historyPrice) {
        validate(historyPrice);
        mapper.insert(historyPrice);
        customerService.touchActivity(historyPrice.getSupplierCustomerId());
        return historyPrice;
    }

    @Override
    @Transactional
    public ProductHistoryPrice update(ProductHistoryPrice historyPrice) {
        validate(historyPrice);
        mapper.update(historyPrice);
        customerService.touchActivity(historyPrice.getSupplierCustomerId());
        return mapper.selectById(historyPrice.getId());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void validate(ProductHistoryPrice historyPrice) {
        if (!StringUtils.hasText(historyPrice.getProductBasicInfoUuid())) {
            throw new IllegalArgumentException("Product is required");
        }
        MaterialMaster materialMaster = materialMasterMapper.selectByUuid(historyPrice.getProductBasicInfoUuid());
        if (materialMaster == null) {
            throw new IllegalArgumentException("Product does not exist");
        }
        if (historyPrice.getReferencePrice() == null || historyPrice.getReferencePrice() < 0) {
            throw new IllegalArgumentException("Reference price must be greater than or equal to 0");
        }
        if (!StringUtils.hasText(historyPrice.getSupplierCustomerId())) {
            throw new IllegalArgumentException("Supplier is required");
        }
        Customer customer = customerMapper.selectByUuid(historyPrice.getSupplierCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("Supplier does not exist");
        }
        CustomerType customerType = customerTypeMapper.selectById(customer.getCustomerTypeId());
        if (customerType == null || !SUPPLIER_CUSTOMER_TYPES.contains(customerType.getTypeName())) {
            throw new IllegalArgumentException("Supplier must be a partner or supplier customer");
        }
    }
}
