package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductSubcategoryMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.ProductSubcategory;
import com.jerry.salesmanagement.service.ProductSubcategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ProductSubcategoryServiceImpl implements ProductSubcategoryService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2}$");

    @Autowired
    private ProductSubcategoryMapper mapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public List<ProductSubcategory> getAll() {
        return mapper.selectAll();
    }

    @Override
    public ProductSubcategory getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public ProductSubcategory create(ProductSubcategory item) {
        validate(item);
        mapper.insert(item);
        return item;
    }

    @Override
    @Transactional
    public ProductSubcategory update(ProductSubcategory item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("Subcategory id is required");
        }
        ProductSubcategory existing = mapper.selectById(item.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Subcategory does not exist");
        }
        validate(item);
        if (!Objects.equals(existing.getCode(), item.getCode())
                && referenceCheckMapper != null
                && referenceCheckMapper.countMaterialsBySubcategoryId(existing.getId()) > 0) {
            throw new IllegalArgumentException("Subcategory code is referenced by materials and cannot be changed");
        }
        mapper.update(item);
        ProductSubcategory updated = mapper.selectById(item.getId());
        return updated != null ? updated : item;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductSubcategory existing = mapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "物料分项",
                    new ReferenceCheckMapper.ReferenceUsage("物料主数据", referenceCheckMapper.countMaterialsBySubcategoryId(id))
            );
        }
        mapper.deleteById(id);
    }

    private void validate(ProductSubcategory item) {
        if (!StringUtils.hasText(item.getCode())) {
            throw new IllegalArgumentException("Subcategory code must be 2 characters");
        }
        String code = item.getCode().trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Subcategory code must be 2 uppercase alphanumeric characters");
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
        item.setCode(code);
        item.setName(item.getName().trim());
        for (ProductSubcategory other : mapper.selectAll()) {
            if (other != null
                    && !Objects.equals(other.getId(), item.getId())
                    && other.getCode() != null
                    && other.getCode().trim().equalsIgnoreCase(code)) {
                throw new IllegalArgumentException("Subcategory code already exists");
            }
        }
    }
}
