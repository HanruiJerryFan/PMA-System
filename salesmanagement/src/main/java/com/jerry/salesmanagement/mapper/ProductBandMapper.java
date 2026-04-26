package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProductBand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductBandMapper {
    List<ProductBand> selectAll();
    ProductBand selectById(Long id);
    int insert(ProductBand item);
    int update(ProductBand item);
    int deleteById(Long id);
}
