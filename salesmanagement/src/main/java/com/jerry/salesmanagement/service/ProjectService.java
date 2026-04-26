package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Project;

import java.util.List;

public interface ProjectService {
    Project getByUuid(String uuid);

    List<Project> getAll();

    Project create(Project project);

    Project update(Project project);

    void delete(String uuid);
}
