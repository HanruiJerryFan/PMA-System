package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ClauseType;
import com.jerry.salesmanagement.service.ClauseTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clausetype")
@CrossOrigin
public class ClauseTypeController {

    @Autowired
    private ClauseTypeService service;

    @GetMapping
    public List<ClauseType> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ClauseType getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
