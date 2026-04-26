package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProductHistoryPrice;

import java.util.List;

public interface ProductHistoryPriceService {
    ProductHistoryPrice getById(Long id);
    List<ProductHistoryPrice> getAll();
    List<ProductHistoryPrice> getByProductUuid(String productUuid);
    ProductHistoryPrice create(ProductHistoryPrice historyPrice);
    ProductHistoryPrice update(ProductHistoryPrice historyPrice);
    void delete(Long id);
}
