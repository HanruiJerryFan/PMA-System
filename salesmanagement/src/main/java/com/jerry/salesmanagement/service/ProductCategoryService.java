package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProductCategory;

import java.util.List;

public interface ProductCategoryService {
    ProductCategory getById(Long id);
    List<ProductCategory> getAll();
    ProductCategory create(ProductCategory category);
    ProductCategory update(ProductCategory category);
    void delete(Long id);
}
