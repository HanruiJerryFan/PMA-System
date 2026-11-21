package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import com.jerry.salesmanagement.pojo.dto.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MaterialMasterService {
    List<MaterialMaster> getAll();

    PageResult<MaterialMaster> getPage(
            String materialCode,
            String productName,
            String productModel,
            Long brandId,
            Boolean isActive,
            int page,
            int pageSize
    );

    MaterialMaster getByUuid(String uuid);

    MaterialMaster create(MaterialMaster materialMaster);

    MaterialMaster update(MaterialMaster materialMaster);

    void delete(String uuid);

    ExcelImportResult importFromExcel(MultipartFile file, String importMode);
}
