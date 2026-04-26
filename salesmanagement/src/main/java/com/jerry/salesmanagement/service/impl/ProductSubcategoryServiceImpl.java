package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductSubcategoryMapper;
import com.jerry.salesmanagement.pojo.ProductSubcategory;
import com.jerry.salesmanagement.service.ProductSubcategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductSubcategoryServiceImpl implements ProductSubcategoryService {

    @Autowired
    private ProductSubcategoryMapper mapper;

    @Override
    public List<ProductSubcategory> getAll() {
        return mapper.selectAll();
    }

    @Override
    public ProductSubcategory getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public ProductSubcategory create(ProductSubcategory item) {
        validate(item);
        mapper.insert(item);
        return item;
    }

    @Override
    public ProductSubcategory update(ProductSubcategory item) {
        validate(item);
        mapper.update(item);
        return mapper.selectById(item.getId());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void validate(ProductSubcategory item) {
        if (!StringUtils.hasText(item.getCode()) || item.getCode().trim().length() != 2) {
            throw new IllegalArgumentException("Subcategory code must be 2 characters");
        }
        if (!StringUtils.hasText(item.getName())) {
            throw new IllegalArgumentException("Subcategory name is required");
        }
        if (item.getSortOrder() == null) {
            throw new IllegalArgumentException("Subcategory sort order is required");
        }
        if (item.getIsActive() == null) {
            throw new IllegalArgumentException("Subcategory active flag is required");
        }
        item.setCode(item.getCode().trim().toUpperCase());
    }
}
