package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.CustomerIndustryDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-industry-dicts")
public class CustomerIndustryDictController {

    @Autowired
    private CustomerIndustryDictService customerIndustryDictService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

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
        CustomerIndustryDict created = customerIndustryDictService.create(industryDict);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("customer-industry-dicts", "CREATE", "customer-industry-dicts", String.valueOf(created.getId()), "Created customer industry");
        }
        return created;
    }

    @PutMapping("/{id}")
    public CustomerIndustryDict update(@PathVariable Long id, @RequestBody CustomerIndustryDict industryDict) {
        industryDict.setId(id);
        CustomerIndustryDict updated = customerIndustryDictService.update(industryDict);
        if (auditTrailService != null) {
            auditTrailService.record("customer-industry-dicts", "UPDATE", "customer-industry-dicts", String.valueOf(id), "Updated customer industry");
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        customerIndustryDictService.delete(id);
        if (auditTrailService != null) {
            auditTrailService.record("customer-industry-dicts", "DELETE", "customer-industry-dicts", String.valueOf(id), "Deleted customer industry");
        }
    }
}
