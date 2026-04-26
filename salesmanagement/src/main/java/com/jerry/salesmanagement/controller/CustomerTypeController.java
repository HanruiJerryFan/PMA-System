package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.service.CustomerTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-types")
@CrossOrigin
public class CustomerTypeController {

    @Autowired
    private CustomerTypeService service;

    @GetMapping
    public List<CustomerType> getAll() {
        return service.getAll();
    }

    @GetMapping("/options")
    public List<CustomerType> getOptions() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CustomerType getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
