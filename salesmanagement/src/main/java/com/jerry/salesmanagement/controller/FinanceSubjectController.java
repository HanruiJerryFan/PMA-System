package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.FinanceSubject;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.FinanceSubjectService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance-subjects")
@CrossOrigin
public class FinanceSubjectController {
    private final FinanceSubjectService service;
    private final AuditTrailService auditTrailService;

    public FinanceSubjectController(FinanceSubjectService service, AuditTrailService auditTrailService) {
        this.service = service;
        this.auditTrailService = auditTrailService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance.access', 'finance.manage', 'finance.voucher.entry', 'finance.voucher.audit')")
    public ApiResponse<List<FinanceSubject>> getAll() {
        try {
            return ApiResponse.success(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("查询财务科目失败: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance.manage')")
    public ApiResponse<FinanceSubject> create(@RequestBody FinanceSubject subject) {
        try {
            FinanceSubject created = service.create(subject);
            auditTrailService.record("finance-subjects", "CREATE", "finance-subjects",
                    String.valueOf(created.getId()), "新增财务科目: " + created.getSubjectName());
            return ApiResponse.success("科目已新增", created);
        } catch (Exception e) {
            return ApiResponse.error("新增财务科目失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance.manage')")
    public ApiResponse<FinanceSubject> update(@PathVariable Long id, @RequestBody FinanceSubject subject) {
        try {
            subject.setId(id);
            FinanceSubject updated = service.update(subject);
            auditTrailService.record("finance-subjects", "UPDATE", "finance-subjects",
                    String.valueOf(id), "修改财务科目: " + updated.getSubjectName());
            return ApiResponse.success("科目已修改", updated);
        } catch (Exception e) {
            return ApiResponse.error("修改财务科目失败: " + e.getMessage());
        }
    }
}
