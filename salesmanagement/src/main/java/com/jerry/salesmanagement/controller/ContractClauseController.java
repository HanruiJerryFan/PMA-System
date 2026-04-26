package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ContractClause;
import com.jerry.salesmanagement.service.ContractClauseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contract-clauses")
@CrossOrigin
public class ContractClauseController {

    @Autowired
    private ContractClauseService service;

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
        return service.create(clause);
    }

    @PutMapping("/{uuid}")
    public ContractClause update(@PathVariable String uuid, @RequestBody ContractClause clause) {
        clause.setUuid(uuid);
        return service.update(clause);
    }

    @DeleteMapping("/{uuid}")
    public void delete(@PathVariable String uuid) {
        service.delete(uuid);
    }
}
