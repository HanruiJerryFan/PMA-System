package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.TaxRateDict;

import java.util.List;

public interface TaxRateDictService {
    TaxRateDict getById(Long id);
    List<TaxRateDict> getAll();
    TaxRateDict create(TaxRateDict taxRateDict);
    TaxRateDict update(TaxRateDict taxRateDict);
    void delete(Long id);
}
