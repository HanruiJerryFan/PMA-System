package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ContractClause;
import com.jerry.salesmanagement.service.AuditTrailService;
import com.jerry.salesmanagement.service.ContractClauseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contract-clauses")
public class ContractClauseController {

    @Autowired
    private ContractClauseService service;

    @Autowired(required = false)
    private AuditTrailService auditTrailService;

    @GetMapping("/{uuid}")
    public ContractClause getByUuid(@PathVariable String uuid) {
        return service.getByUuid(uuid);
    }

    @GetMapping("/byContract/{contractUuid}")
    public List<ContractClause> getByContractUuid(@PathVariable String contractUuid) {
        return service.getByContractUuid(contractUuid);
    }

    @PostMapping
    public ContractClause create(@RequestBody ContractClause clause) {
        ContractClause created = service.create(clause);
        if (auditTrailService != null && created != null) {
            auditTrailService.record("contract-clauses", "CREATE", "contract-clauses", created.getUuid(), "Created contract clause");
        }
        return created;
    }

    @PutMapping("/{uuid}")
    public ContractClause update(@PathVariable String uuid, @RequestBody ContractClause clause) {
        clause.setUuid(uuid);
        ContractClause updated = service.update(clause);
        if (auditTrailService != null) {
            auditTrailService.record("contract-clauses", "UPDATE", "contract-clauses", uuid, "Updated contract clause");
        }
        return updated;
    }

    @DeleteMapping("/{uuid}")
    public void delete(@PathVariable String uuid) {
        service.delete(uuid);
        if (auditTrailService != null) {
            auditTrailService.record("contract-clauses", "DELETE", "contract-clauses", uuid, "Deleted contract clause");
        }
    }
}
