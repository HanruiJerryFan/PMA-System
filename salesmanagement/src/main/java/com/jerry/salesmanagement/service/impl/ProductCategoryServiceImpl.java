package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductCategoryMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.ProductCategory;
import com.jerry.salesmanagement.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2}$");

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public ProductCategory getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<ProductCategory> getAll() {
        return categoryMapper.selectAll();
    }

    @Override
    @Transactional
    public ProductCategory create(ProductCategory category) {
        validate(category);
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public ProductCategory update(ProductCategory category) {
        if (category.getId() == null) {
            throw new IllegalArgumentException("Category id is required");
        }
        ProductCategory existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Category does not exist");
        }
        validate(category);
        if (!Objects.equals(existing.getCode(), category.getCode())
                && referenceCheckMapper != null
                && referenceCheckMapper.countMaterialsByCategoryId(existing.getId()) > 0) {
            throw new IllegalArgumentException("Category code is referenced by materials and cannot be changed");
        }
        categoryMapper.update(category);
        ProductCategory updated = categoryMapper.selectById(category.getId());
        return updated != null ? updated : category;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "物料大类",
                    new ReferenceCheckMapper.ReferenceUsage("物料主数据", referenceCheckMapper.countMaterialsByCategoryId(id))
            );
        }
        categoryMapper.deleteById(id);
    }

    private void validate(ProductCategory category) {
        if (!StringUtils.hasText(category.getCode())) {
            throw new IllegalArgumentException("Category code must be 2 characters");
        }
        String code = category.getCode().trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Category code must be 2 uppercase alphanumeric characters");
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
        category.setCode(code);
        category.setName(category.getName().trim());
        for (ProductCategory other : categoryMapper.selectAll()) {
            if (other != null
                    && !Objects.equals(other.getId(), category.getId())
                    && other.getCode() != null
                    && other.getCode().trim().equalsIgnoreCase(code)) {
                throw new IllegalArgumentException("Category code already exists");
            }
        }
    }
}
