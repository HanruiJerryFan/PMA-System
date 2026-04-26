package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import com.jerry.salesmanagement.service.CustomerIndustryDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-industry-dicts")
@CrossOrigin
public class CustomerIndustryDictController {

    @Autowired
    private CustomerIndustryDictService customerIndustryDictService;

    @GetMapping
    public List<CustomerIndustryDict> getAll() {
        return customerIndustryDictService.getAll();
    }

    @GetMapping("/{id}")
    public CustomerIndustryDict getById(@PathVariable Long id) {
        return customerIndustryDictService.getById(id);
    }

    @PostMapping
    public CustomerIndustryDict create(@RequestBody CustomerIndustryDict industryDict) {
        return customerIndustryDictService.create(industryDict);
    }

    @PutMapping("/{id}")
    public CustomerIndustryDict update(@PathVariable Long id, @RequestBody CustomerIndustryDict industryDict) {
        industryDict.setId(id);
        return customerIndustryDictService.update(industryDict);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        customerIndustryDictService.delete(id);
    }
}
