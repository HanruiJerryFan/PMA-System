package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProductBrand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductBrandMapper {
    ProductBrand selectById(Long id);
    List<ProductBrand> selectAll();

    int insert(ProductBrand brand);
    int update(ProductBrand brand);
    int deleteById(Long id);
}
