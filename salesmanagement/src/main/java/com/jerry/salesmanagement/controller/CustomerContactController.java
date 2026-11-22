package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.CustomerContact;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.CustomerContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-contacts")
public class CustomerContactController {

    @Autowired
    private CustomerContactService customerContactService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

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
        CustomerContact created = customerContactService.create(Contact);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("customer-contacts", "CREATE", "customer-contacts", created.getUuid(), "Created customer contact");
        }
        return created;
    }

    @PutMapping("/{uuid}")
    public CustomerContact updateCustomerContact(@PathVariable String uuid, @RequestBody CustomerContact Contact) {
        Contact.setUuid(uuid);
        CustomerContact updated = customerContactService.update(Contact);
        if (auditTrailService != null) {
            auditTrailService.record("customer-contacts", "UPDATE", "customer-contacts", uuid, "Updated customer contact");
        }
        return updated;
    }

    @DeleteMapping("/{uuid}")
    public void deleteCustomerContact(@PathVariable String uuid) {
        customerContactService.delete(uuid);
        if (auditTrailService != null) {
            auditTrailService.record("customer-contacts", "DELETE", "customer-contacts", uuid, "Deleted customer contact");
        }
    }
}
