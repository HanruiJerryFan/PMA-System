package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductBandMapper;
import com.jerry.salesmanagement.pojo.ProductBand;
import com.jerry.salesmanagement.service.ProductBandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductBandServiceImpl implements ProductBandService {

    @Autowired
    private ProductBandMapper mapper;

    @Override
    public List<ProductBand> getAll() {
        return mapper.selectAll();
    }

    @Override
    public ProductBand getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public ProductBand create(ProductBand item) {
        validate(item);
        mapper.insert(item);
        return item;
    }

    @Override
    public ProductBand update(ProductBand item) {
        validate(item);
        mapper.update(item);
        return mapper.selectById(item.getId());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void validate(ProductBand item) {
        if (!StringUtils.hasText(item.getCode()) || item.getCode().trim().length() > 3) {
            throw new IllegalArgumentException("Band code must be at most 3 characters");
        }
        if (!StringUtils.hasText(item.getDescription())) {
            throw new IllegalArgumentException("Band description is required");
        }
        if (item.getSortOrder() == null) {
            throw new IllegalArgumentException("Band sort order is required");
        }
        if (item.getIsActive() == null) {
            throw new IllegalArgumentException("Band active flag is required");
        }
        item.setCode(item.getCode().trim().toUpperCase());
    }
}
