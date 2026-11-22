package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.CustomerActivityRule;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.CustomerActivityRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-activity-rules")
public class CustomerActivityRuleController {

    @Autowired
    private CustomerActivityRuleService customerActivityRuleService;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

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
            CustomerActivityRule created = customerActivityRuleService.create(rule);
            if (auditTrailService != null && created != null) {
                auditTrailService.record("customer-activity-rules", "CREATE", "customer-activity-rules", String.valueOf(created.getId()), "Created customer activity rule");
            }
            return ApiResponse.success("Customer activity rule created", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create customer activity rule: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerActivityRule> update(@PathVariable Long id, @RequestBody CustomerActivityRule rule) {
        try {
            rule.setId(id);
            CustomerActivityRule updated = customerActivityRuleService.update(rule);
            if (auditTrailService != null) {
                auditTrailService.record("customer-activity-rules", "UPDATE", "customer-activity-rules", String.valueOf(id), "Updated customer activity rule");
            }
            return ApiResponse.success("Customer activity rule updated", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update customer activity rule: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            customerActivityRuleService.delete(id);
            if (auditTrailService != null) {
                auditTrailService.record("customer-activity-rules", "DELETE", "customer-activity-rules", String.valueOf(id), "Deleted customer activity rule");
            }
            return ApiResponse.success("Customer activity rule deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete customer activity rule: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<Integer> refreshStatuses() {
        try {
            int updatedCount = customerActivityRuleService.refreshAllCustomerActivityStatuses();
            if (auditTrailService != null) {
                auditTrailService.record("customer-activity-rules", "REFRESH", "customer-activity-rules", "all",
                        "Refreshed customer activity statuses: " + updatedCount);
            }
            return ApiResponse.success("Customer activity statuses refreshed", updatedCount);
        } catch (Exception e) {
            return ApiResponse.error("Failed to refresh customer activity statuses: " + e.getMessage());
        }
    }
}
