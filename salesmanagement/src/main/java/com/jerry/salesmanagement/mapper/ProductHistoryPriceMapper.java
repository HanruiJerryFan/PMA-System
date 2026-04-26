package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProductHistoryPrice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductHistoryPriceMapper {
    ProductHistoryPrice selectById(Long id);
    List<ProductHistoryPrice> selectAll();
    List<ProductHistoryPrice> selectByProductUuid(String productUuid);
    int insert(ProductHistoryPrice historyPrice);
    int update(ProductHistoryPrice historyPrice);
    int deleteById(Long id);
}
