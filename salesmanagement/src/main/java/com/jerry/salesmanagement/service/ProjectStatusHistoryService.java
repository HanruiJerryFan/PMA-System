package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProjectStatusHistory;

import java.util.List;

public interface ProjectStatusHistoryService {
    List<ProjectStatusHistory> getAll();

    ProjectStatusHistory getByUuid(String uuid);

    List<ProjectStatusHistory> getByProjectId(String projectId);

    ProjectStatusHistory create(ProjectStatusHistory history);

    ProjectStatusHistory update(ProjectStatusHistory history);

    void delete(String uuid);
}
