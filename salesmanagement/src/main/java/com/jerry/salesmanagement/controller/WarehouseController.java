package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public List<Warehouse> getAll() {
        return warehouseService.getAll();
    }

    @GetMapping("/{id}")
    public Warehouse getById(@PathVariable Long id) {
        return warehouseService.getById(id);
    }

    @PostMapping
    public Warehouse create(@RequestBody Warehouse warehouse) {
        Warehouse created = warehouseService.create(warehouse);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("warehouses", "CREATE", "warehouses", String.valueOf(created.getId()), "Created warehouse");
        }
        return created;
    }

    @PutMapping("/{id}")
    public Warehouse update(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        warehouse.setId(id);
        Warehouse updated = warehouseService.update(warehouse);
        if (auditTrailService != null) {
            auditTrailService.record("warehouses", "UPDATE", "warehouses", String.valueOf(id), "Updated warehouse");
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        warehouseService.delete(id);
        if (auditTrailService != null) {
            auditTrailService.record("warehouses", "DELETE", "warehouses", String.valueOf(id), "Deleted warehouse");
        }
    }
}
