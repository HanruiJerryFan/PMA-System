package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import com.jerry.salesmanagement.service.ContractBasicInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin
public class ContractBasicInfoController {

    @Autowired
    private ContractBasicInfoService service;

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
        return service.create(info);
    }

    @PutMapping("/{uuid}")
    public ContractBasicInfo update(@PathVariable String uuid, @RequestBody ContractBasicInfo info) {
        info.setUuid(uuid);
        return service.update(info);
    }

    @DeleteMapping("/{uuid}")
    public void delete(@PathVariable String uuid) {
        service.delete(uuid);
    }
}
