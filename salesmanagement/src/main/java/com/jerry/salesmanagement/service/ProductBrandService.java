package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProductBrand;

import java.util.List;

public interface ProductBrandService {
    ProductBrand getById(Long id);
    List<ProductBrand> getAll();
    ProductBrand create(ProductBrand brand);
    ProductBrand update(ProductBrand brand);
    void delete(Long id);
}
