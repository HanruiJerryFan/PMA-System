package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.service.ProductBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductBrandServiceImpl implements ProductBrandService {

    @Autowired
    private ProductBrandMapper brandMapper;

    @Override
    public ProductBrand getById(Long id) {
        return brandMapper.selectById(id);
    }

    @Override
    public List<ProductBrand> getAll() {
        return brandMapper.selectAll();
    }

    @Override
    public ProductBrand create(ProductBrand brand) {
        validate(brand);
        brandMapper.insert(brand);
        return brand;
    }

    @Override
    public ProductBrand update(ProductBrand brand) {
        validate(brand);
        brandMapper.update(brand);
        return brand;
    }

    @Override
    public void delete(Long id) {
        brandMapper.deleteById(id);
    }

    private void validate(ProductBrand brand) {
        if (!StringUtils.hasText(brand.getCode()) || brand.getCode().trim().length() != 2) {
            throw new IllegalArgumentException("Brand code must be 2 characters");
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
        brand.setCode(brand.getCode().trim().toUpperCase());
    }
}
