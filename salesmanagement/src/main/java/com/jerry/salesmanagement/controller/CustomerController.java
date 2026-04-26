package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@RequestMapping("/api/customers")
@CrossOrigin
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ApiResponse<List<Customer>> getAll() {
        try {
            return ApiResponse.success(customerService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query customers: " + e.getMessage());
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
            return ApiResponse.success("Customer created", customerService.create(customer));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create customer: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<Customer> update(@PathVariable String uuid, @RequestBody Customer customer) {
        try {
            customer.setUuid(uuid);
            return ApiResponse.success("Customer updated", customerService.update(customer));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update customer: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            customerService.delete(uuid);
            return ApiResponse.success("Customer deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete customer: " + e.getMessage());
        }
    }
}
