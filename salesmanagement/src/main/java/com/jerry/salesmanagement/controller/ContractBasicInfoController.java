package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ContractBasicInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractBasicInfoController {

    @Autowired
    private ContractBasicInfoService service;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping
    public List<ContractBasicInfo> getAll() {
        return service.getAll();
    }

    @GetMapping("/options")
    public List<ContractBasicInfo> getOptions() {
        return service.getAll();
    }

    @GetMapping("/{uuid}")
    public ContractBasicInfo getByUuid(@PathVariable String uuid) {
        return service.getByUuid(uuid);
    }

    @PostMapping
    public ContractBasicInfo create(@RequestBody ContractBasicInfo info) {
        ContractBasicInfo created = service.create(info);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("contracts", "CREATE", "contracts", created.getUuid(), "Created contract");
        }
        return created;
    }

    @PutMapping("/{uuid}")
    public ContractBasicInfo update(@PathVariable String uuid, @RequestBody ContractBasicInfo info) {
        info.setUuid(uuid);
        ContractBasicInfo updated = service.update(info);
        if (auditTrailService != null) {
            auditTrailService.record("contracts", "UPDATE", "contracts", uuid, "Updated contract");
        }
        return updated;
    }

    @DeleteMapping("/{uuid}")
    public void delete(@PathVariable String uuid) {
        service.delete(uuid);
        if (auditTrailService != null) {
            auditTrailService.record("contracts", "DELETE", "contracts", uuid, "Deleted contract");
        }
    }
}
