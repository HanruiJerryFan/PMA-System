package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductCategoryMapper;
import com.jerry.salesmanagement.pojo.ProductCategory;
import com.jerry.salesmanagement.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Override
    public ProductCategory getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<ProductCategory> getAll() {
        return categoryMapper.selectAll();
    }

    @Override
    public ProductCategory create(ProductCategory category) {
        validate(category);
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public ProductCategory update(ProductCategory category) {
        validate(category);
        categoryMapper.update(category);
        return category;
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }

    private void validate(ProductCategory category) {
        if (!StringUtils.hasText(category.getCode()) || category.getCode().trim().length() != 2) {
            throw new IllegalArgumentException("Category code must be 2 characters");
        }
        if (!StringUtils.hasText(category.getName())) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (category.getSortOrder() == null) {
            throw new IllegalArgumentException("Category sort order is required");
        }
        if (category.getIsActive() == null) {
            throw new IllegalArgumentException("Category active flag is required");
        }
        category.setCode(category.getCode().trim().toUpperCase());
    }
}
