package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.dto.ProjectTrackingBoardDto;

public interface ProjectTrackingBoardService {
    ProjectTrackingBoardDto getTrackingBoard(String keyword, Long managerId, Long projectTypeId, String riskTag);
}
