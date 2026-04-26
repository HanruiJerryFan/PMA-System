package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.FinanceVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance-vouchers")
@CrossOrigin
public class FinanceVoucherController {

    @Autowired
    private FinanceVoucherService service;

    @Autowired
    private AuditTrailService auditTrailService;

    @GetMapping
    public ApiResponse<List<FinanceVoucher>> getAll() {
        try {
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query finance vouchers: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResponse<List<FinanceVoucher>> getOptions() {
        try {
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query finance voucher options: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<FinanceVoucher> getByUuid(@PathVariable String uuid) {
        try {
            FinanceVoucher voucher = service.getByUuid(uuid);
            return voucher != null ? ApiResponse.success(voucher) : ApiResponse.notFound("Finance voucher not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query finance voucher: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<FinanceVoucher> create(@RequestBody FinanceVoucher voucher) {
        try {
            FinanceVoucher created = service.create(voucher);
            auditTrailService.record("finance-vouchers", "CREATE", "finance-vouchers", created.getUuid(), "Created finance voucher");
            return ApiResponse.success("Finance voucher created", created);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create finance voucher: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<FinanceVoucher> update(@PathVariable String uuid, @RequestBody FinanceVoucher voucher) {
        try {
            voucher.setUuid(uuid);
            FinanceVoucher updated = service.update(voucher);
            auditTrailService.record("finance-vouchers", "UPDATE", "finance-vouchers", uuid, "Updated finance voucher");
            return ApiResponse.success("Finance voucher updated", updated);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update finance voucher: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            service.delete(uuid);
            auditTrailService.record("finance-vouchers", "DELETE", "finance-vouchers", uuid, "Deleted finance voucher");
            return ApiResponse.success("Finance voucher deleted", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete finance voucher: " + e.getMessage());
        }
    }
}
