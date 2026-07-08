package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.dto.ProjectListAggregateView;

import java.util.List;

public interface ProjectListService {
    ProjectList getByUuid(String uuid);
    List<ProjectList> getAll();
    List<ProjectList> getByProjectId(String projectId);
    ProjectListAggregateView getAggregateView(String projectId, String aggregateType);
    ProjectList create(ProjectList projectList);
    ProjectList update(ProjectList projectList);
    ProjectList audit(String uuid);
    void delete(String uuid);
}
