package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<Customer>> getAll() {
        try {
            return ApiResponse.success(customerService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query customers: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<Customer>> getPage(
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String taxIdentificationNumber,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String activityStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            return ApiResponse.success(customerService.getPage(
                    customerCode, customerName, taxIdentificationNumber, industry, activityStatus, page, pageSize
            ));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query customers page: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResponse<List<Customer>> getOptions() {
        try {
            return ApiResponse.success(customerService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query customer options: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<Customer> getByUuid(@PathVariable String uuid) {
        try {
            Customer customer = customerService.getByUuid(uuid);
            return customer != null ? ApiResponse.success(customer) : ApiResponse.notFound("Customer not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query customer: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Customer> create(@RequestBody Customer customer) {
        try {
            Customer created = customerService.create(customer);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("customers", "CREATE", "customers", created.getUuid(), "Created customer");
            }
            return ApiResponse.success("Customer created", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create customer: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<Customer> update(@PathVariable String uuid, @RequestBody Customer customer) {
        try {
            customer.setUuid(uuid);
            Customer updated = customerService.update(customer);
            if (auditTrailService != null) {
                auditTrailService.record("customers", "UPDATE", "customers", uuid, "Updated customer");
            }
            return ApiResponse.success("Customer updated", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update customer: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            customerService.delete(uuid);
            if (auditTrailService != null) {
                auditTrailService.record("customers", "DELETE", "customers", uuid, "Deleted customer");
            }
            return ApiResponse.success("Customer deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete customer: " + e.getMessage());
        }
    }
}
