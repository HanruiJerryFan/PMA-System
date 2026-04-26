package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ContractType;
import com.jerry.salesmanagement.service.ContractTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contract-types")
@CrossOrigin
public class ContractTypeController {

    @Autowired
    private ContractTypeService service;

    @GetMapping
    public List<ContractType> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ContractType getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
