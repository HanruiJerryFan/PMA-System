package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProductSubcategory;

import java.util.List;

public interface ProductSubcategoryService {
    List<ProductSubcategory> getAll();
    ProductSubcategory getById(Long id);
    ProductSubcategory create(ProductSubcategory item);
    ProductSubcategory update(ProductSubcategory item);
    void delete(Long id);
}
