package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.Stage;

import java.util.List;

public interface StageService {
    Stage getById(Long id);
    List<Stage> getAll();
    Stage create(Stage stage);
    Stage update(Stage stage);
    void delete(Long id);
}
