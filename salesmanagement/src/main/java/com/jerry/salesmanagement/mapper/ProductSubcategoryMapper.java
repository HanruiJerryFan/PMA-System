package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ProductSubcategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductSubcategoryMapper {
    List<ProductSubcategory> selectAll();
    ProductSubcategory selectById(Long id);
    int insert(ProductSubcategory item);
    int update(ProductSubcategory item);
    int deleteById(Long id);
}
