package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.Region;
import com.jerry.salesmanagement.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/region")
@CrossOrigin
public class RegionController {

    @Autowired
    private RegionService regionService;

    @GetMapping
    public ApiResponse<List<Region>> getRegions() {
        try {
            return ApiResponse.success(regionService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query regions: " + e.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResponse<List<Region>> getOptions() {
        try {
            return ApiResponse.success(regionService.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query region options: " + e.getMessage());
        }
    }

    @GetMapping("/provinces")
    public ApiResponse<List<Region>> getProvinces() {
        try {
            return ApiResponse.success(regionService.getProvinces());
        } catch (Exception e) {
            return ApiResponse.error("Failed to query provinces: " + e.getMessage());
        }
    }

    @GetMapping("/cities/{provinceAreaCode}")
    public ApiResponse<List<Region>> getCities(@PathVariable Long provinceAreaCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(provinceAreaCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query cities: " + e.getMessage());
        }
    }

    @GetMapping("/districts/{cityAreaCode}")
    public ApiResponse<List<Region>> getDistricts(@PathVariable Long cityAreaCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(cityAreaCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query districts: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Region> getById(@PathVariable Integer id) {
        try {
            Region region = regionService.getById(id);
            return region != null ? ApiResponse.success(region) : ApiResponse.error("Region not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query region: " + e.getMessage());
        }
    }

    @GetMapping("/byCodePrefix/{codePrefix}")
    public ApiResponse<Region> getByCodePrefix(@PathVariable String codePrefix) {
        try {
            Region region = regionService.getByCodePrefix(codePrefix);
            return region != null ? ApiResponse.success(region) : ApiResponse.error("Region not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query region by code prefix: " + e.getMessage());
        }
    }

    @GetMapping("/byParentCode/{parentCode}")
    public ApiResponse<List<Region>> getByParentCode(@PathVariable Long parentCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(parentCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query child regions: " + e.getMessage());
        }
    }

    @GetMapping("/children/{parentCode}")
    public ApiResponse<List<Region>> getChildren(@PathVariable Long parentCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(parentCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query child regions: " + e.getMessage());
        }
    }

    @GetMapping("/cities/byProvinceCode/{provinceAreaCode}")
    public ApiResponse<List<Region>> getCitiesByProvinceCode(@PathVariable Long provinceAreaCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(provinceAreaCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query cities by province code: " + e.getMessage());
        }
    }

    @GetMapping("/districts/byCityCode/{cityAreaCode}")
    public ApiResponse<List<Region>> getDistrictsByCityCode(@PathVariable Long cityAreaCode) {
        try {
            return ApiResponse.success(regionService.getByParentCode(cityAreaCode));
        } catch (Exception e) {
            return ApiResponse.error("Failed to query districts by city code: " + e.getMessage());
        }
    }
}
