package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.dto.PageResult;

import java.util.List;

public interface ProjectService {
    Project getByUuid(String uuid);

    List<Project> getAll();

    PageResult<Project> getPage(
            String projectNumber,
            String projectName,
            String customerId,
            Long managerId,
            int page,
            int pageSize
    );

    Project create(Project project);

    Project update(Project project);

    void delete(String uuid);
}
