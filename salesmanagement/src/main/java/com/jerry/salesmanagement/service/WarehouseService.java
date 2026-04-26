package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Warehouse;

import java.util.List;

public interface WarehouseService {
    Warehouse getById(Long id);

    List<Warehouse> getAll();

    Warehouse create(Warehouse warehouse);

    Warehouse update(Warehouse warehouse);

    void delete(Long id);
}
