package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Region;
import com.jerry.salesmanagement.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ProvinceCityAliasController {

    @Autowired
    private RegionService regionService;

    @GetMapping("/provinces")
    public ApiResponse<List<Region>> getProvinces() {
        try {
            return ApiResponse.success(regionService.getProvinces());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query provinces: " + e.getMessage());
        }
    }

    @GetMapping("/cities")
    public ApiResponse<List<Region>> getCities(@RequestParam Long provinceAreaCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(provinceAreaCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query cities: " + e.getMessage());
        }
    }

    @GetMapping("/cities/{provinceAreaCode}")
    public ApiResponse<List<Region>> getCitiesByPath(@PathVariable Long provinceAreaCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(provinceAreaCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query cities: " + e.getMessage());
        }
    }
}
