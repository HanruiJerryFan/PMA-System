package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProductBand;

import java.util.List;

public interface ProductBandService {
    List<ProductBand> getAll();
    ProductBand getById(Long id);
    ProductBand create(ProductBand item);
    ProductBand update(ProductBand item);
    void delete(Long id);
}
