package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.CustomerActivityRule;
import com.jerry.salesmanagement.service.CustomerActivityRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-activity-rules")
@CrossOrigin
public class CustomerActivityRuleController {

    @Autowired
    private CustomerActivityRuleService customerActivityRuleService;

    @GetMapping
    public ApiResponse<List<CustomerActivityRule>> getAll() {
        return ApiResponse.success(customerActivityRuleService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerActivityRule> getById(@PathVariable Long id) {
        CustomerActivityRule item = customerActivityRuleService.getById(id);
        return item != null ? ApiResponse.success(item) : ApiResponse.notFound("Customer activity rule not found");
    }

    @PostMapping
    public ApiResponse<CustomerActivityRule> create(@RequestBody CustomerActivityRule rule) {
        try {
            return ApiResponse.success("Customer activity rule created", customerActivityRuleService.create(rule));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create customer activity rule: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerActivityRule> update(@PathVariable Long id, @RequestBody CustomerActivityRule rule) {
        try {
            rule.setId(id);
            return ApiResponse.success("Customer activity rule updated", customerActivityRuleService.update(rule));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update customer activity rule: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            customerActivityRuleService.delete(id);
            return ApiResponse.success("Customer activity rule deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete customer activity rule: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<Integer> refreshStatuses() {
        try {
            return ApiResponse.success("Customer activity statuses refreshed", customerActivityRuleService.refreshAllCustomerActivityStatuses());
        } catch (Exception e) {
            return ApiResponse.error("Failed to refresh customer activity statuses: " + e.getMessage());
        }
    }
}
