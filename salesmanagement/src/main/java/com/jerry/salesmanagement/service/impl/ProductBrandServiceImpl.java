package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.service.ProductBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ProductBrandServiceImpl implements ProductBrandService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2}$");

    @Autowired
    private ProductBrandMapper brandMapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public ProductBrand getById(Long id) {
        return brandMapper.selectById(id);
    }

    @Override
    public List<ProductBrand> getAll() {
        return brandMapper.selectAll();
    }

    @Override
    @Transactional
    public ProductBrand create(ProductBrand brand) {
        validate(brand);
        brandMapper.insert(brand);
        return brand;
    }

    @Override
    @Transactional
    public ProductBrand update(ProductBrand brand) {
        if (brand.getId() == null) {
            throw new IllegalArgumentException("Brand id is required");
        }
        ProductBrand existing = brandMapper.selectById(brand.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Brand does not exist");
        }
        validate(brand);
        if (!Objects.equals(existing.getCode(), brand.getCode())
                && referenceCheckMapper != null
                && referenceCheckMapper.countMaterialsByBrandId(existing.getId()) > 0) {
            throw new IllegalArgumentException("Brand code is referenced by materials and cannot be changed");
        }
        brandMapper.update(brand);
        ProductBrand updated = brandMapper.selectById(brand.getId());
        return updated != null ? updated : brand;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductBrand existing = brandMapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "物料品牌",
                    new ReferenceCheckMapper.ReferenceUsage("物料主数据", referenceCheckMapper.countMaterialsByBrandId(id))
            );
        }
        brandMapper.deleteById(id);
    }

    private void validate(ProductBrand brand) {
        if (!StringUtils.hasText(brand.getCode())) {
            throw new IllegalArgumentException("Brand code must be 2 characters");
        }
        String code = brand.getCode().trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Brand code must be 2 uppercase alphanumeric characters");
        }
        if (!StringUtils.hasText(brand.getName())) {
            throw new IllegalArgumentException("Brand name is required");
        }
        if (brand.getSortOrder() == null) {
            throw new IllegalArgumentException("Brand sort order is required");
        }
        if (brand.getIsActive() == null) {
            throw new IllegalArgumentException("Brand active flag is required");
        }
        brand.setCode(code);
        brand.setName(brand.getName().trim());
        for (ProductBrand other : brandMapper.selectAll()) {
            if (other != null
                    && !Objects.equals(other.getId(), brand.getId())
                    && other.getCode() != null
                    && other.getCode().trim().equalsIgnoreCase(code)) {
                throw new IllegalArgumentException("Brand code already exists");
            }
        }
    }
}
