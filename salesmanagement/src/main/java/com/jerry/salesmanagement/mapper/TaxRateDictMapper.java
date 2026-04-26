package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.TaxRateDict;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TaxRateDictMapper {
    TaxRateDict selectById(Long id);
    TaxRateDict selectByRate(BigDecimal rate);
    List<TaxRateDict> selectAll();
    int insert(TaxRateDict taxRateDict);
    int update(TaxRateDict taxRateDict);
    int deleteById(Long id);
}
