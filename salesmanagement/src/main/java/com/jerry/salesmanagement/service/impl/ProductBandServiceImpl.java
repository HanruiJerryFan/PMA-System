package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ProductBandMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.pojo.ProductBand;
import com.jerry.salesmanagement.service.ProductBandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ProductBandServiceImpl implements ProductBandService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{1,3}$");

    @Autowired
    private ProductBandMapper mapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public List<ProductBand> getAll() {
        return mapper.selectAll();
    }

    @Override
    public ProductBand getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public ProductBand create(ProductBand item) {
        validate(item);
        mapper.insert(item);
        return item;
    }

    @Override
    @Transactional
    public ProductBand update(ProductBand item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("Band id is required");
        }
        ProductBand existing = mapper.selectById(item.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Band does not exist");
        }
        validate(item);
        if (!Objects.equals(existing.getCode(), item.getCode())
                && referenceCheckMapper != null
                && referenceCheckMapper.countMaterialsByBandCode(existing.getCode()) > 0) {
            throw new IllegalArgumentException("Band code is referenced by materials and cannot be changed");
        }
        mapper.update(item);
        ProductBand updated = mapper.selectById(item.getId());
        return updated != null ? updated : item;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductBand existing = mapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "物料频段",
                    new ReferenceCheckMapper.ReferenceUsage("物料主数据", referenceCheckMapper.countMaterialsByBandCode(existing.getCode()))
            );
        }
        mapper.deleteById(id);
    }

    private void validate(ProductBand item) {
        if (!StringUtils.hasText(item.getCode())) {
            throw new IllegalArgumentException("Band code must be at most 3 characters");
        }
        String code = item.getCode().trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Band code must be 1 to 3 uppercase alphanumeric characters");
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
        item.setCode(code);
        item.setDescription(item.getDescription().trim());
        for (ProductBand other : mapper.selectAll()) {
            if (other != null
                    && !Objects.equals(other.getId(), item.getId())
                    && other.getCode() != null
                    && other.getCode().trim().equalsIgnoreCase(code)) {
                throw new IllegalArgumentException("Band code already exists");
            }
        }
    }
}
