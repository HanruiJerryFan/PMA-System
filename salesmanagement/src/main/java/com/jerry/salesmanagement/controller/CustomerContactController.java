package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.CustomerContact;
import com.jerry.salesmanagement.service.CustomerContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-contacts")
@CrossOrigin
public class CustomerContactController {

    @Autowired
    private CustomerContactService customerContactService;

    @GetMapping
    public List<CustomerContact> getAll() {
        return customerContactService.getAll();
    }

    @GetMapping("/options")
    public List<CustomerContact> getOptions() {
        return customerContactService.getAll();
    }

    @GetMapping("/{uuid}")
    public CustomerContact getCustomerContactByUuid(@PathVariable String uuid) {
        return customerContactService.getByUuid(uuid);
    }

    @PostMapping
    public CustomerContact createCustomerContact(@RequestBody CustomerContact Contact) {
        return customerContactService.create(Contact);
    }

    @PutMapping("/{uuid}")
    public CustomerContact updateCustomerContact(@PathVariable String uuid, @RequestBody CustomerContact Contact) {
        Contact.setUuid(uuid);
        return customerContactService.update(Contact);
    }

    @DeleteMapping("/{uuid}")
    public void deleteCustomerContact(@PathVariable String uuid) {
        customerContactService.delete(uuid);
    }
}
