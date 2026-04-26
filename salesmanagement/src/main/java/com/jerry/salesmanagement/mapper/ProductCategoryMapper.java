package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductCategoryMapper {
    ProductCategory selectById(Long id);
    List<ProductCategory> selectAll();

    int insert(ProductCategory category);
    int update(ProductCategory category);
    int deleteById(Long id);
}
