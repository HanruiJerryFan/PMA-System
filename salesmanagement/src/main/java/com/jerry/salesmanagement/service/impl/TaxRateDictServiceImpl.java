package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.mapper.TaxRateDictMapper;
import com.jerry.salesmanagement.pojo.TaxRateDict;
import com.jerry.salesmanagement.service.TaxRateDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TaxRateDictServiceImpl implements TaxRateDictService {

    @Autowired
    private TaxRateDictMapper mapper;

    @Autowired(required = false)
    private ReferenceCheckMapper referenceCheckMapper;

    @Override
    public TaxRateDict getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<TaxRateDict> getAll() {
        return mapper.selectAll();
    }

    @Override
    @Transactional
    public TaxRateDict create(TaxRateDict taxRateDict) {
        validate(taxRateDict);
        mapper.insert(taxRateDict);
        return taxRateDict;
    }

    @Override
    @Transactional
    public TaxRateDict update(TaxRateDict taxRateDict) {
        if (taxRateDict.getId() == null) {
            throw new IllegalArgumentException("Tax rate id is required");
        }
        TaxRateDict existingById = mapper.selectById(taxRateDict.getId());
        if (existingById == null) {
            throw new IllegalArgumentException("Tax rate does not exist");
        }
        validate(taxRateDict);
        if (existingById.getRate() != null
                && existingById.getRate().setScale(2, RoundingMode.HALF_UP).compareTo(taxRateDict.getRate()) != 0
                && referenceCheckMapper != null
                && referenceCheckMapper.countFinanceVouchersByTaxRate(existingById.getRate()) > 0) {
            throw new IllegalArgumentException("Tax rate is referenced by finance vouchers and its numeric rate cannot be changed");
        }
        mapper.update(taxRateDict);
        return mapper.selectById(taxRateDict.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TaxRateDict existing = mapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (referenceCheckMapper != null) {
            ReferenceCheckMapper.ensureNotReferenced(
                    "税率",
                    new ReferenceCheckMapper.ReferenceUsage("财务凭证", referenceCheckMapper.countFinanceVouchersByTaxRate(existing.getRate()))
            );
        }
        mapper.deleteById(id);
    }

    private void validate(TaxRateDict taxRateDict) {
        if (taxRateDict.getRate() == null) {
            throw new IllegalArgumentException("Tax rate is required");
        }
        taxRateDict.setRate(taxRateDict.getRate().setScale(2, RoundingMode.HALF_UP));
        if (taxRateDict.getRate().compareTo(BigDecimal.ZERO) < 0 || taxRateDict.getRate().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 1");
        }
        if (!StringUtils.hasText(taxRateDict.getLabel())) {
            throw new IllegalArgumentException("Tax rate label is required");
        }
        taxRateDict.setLabel(taxRateDict.getLabel().trim());
        if (taxRateDict.getSortOrder() == null) {
            throw new IllegalArgumentException("Sort order is required");
        }
        if (taxRateDict.getIsActive() == null) {
            throw new IllegalArgumentException("Active flag is required");
        }
        TaxRateDict existing = mapper.selectByRate(taxRateDict.getRate());
        if (existing != null && !existing.getId().equals(taxRateDict.getId())) {
            throw new IllegalArgumentException("Tax rate already exists");
        }
    }
}
