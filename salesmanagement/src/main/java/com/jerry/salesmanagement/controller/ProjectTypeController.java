package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.ProjectType;
import com.jerry.salesmanagement.service.ProjectTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-types")
@CrossOrigin
public class ProjectTypeController {

    @Autowired
    private ProjectTypeService service;

    @GetMapping
    public List<ProjectType> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProjectType getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ProjectType create(@RequestBody ProjectType projectType) {
        return service.create(projectType);
    }

    @PutMapping("/{id}")
    public ProjectType update(@PathVariable Long id, @RequestBody ProjectType projectType) {
        projectType.setId(id);
        return service.update(projectType);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
