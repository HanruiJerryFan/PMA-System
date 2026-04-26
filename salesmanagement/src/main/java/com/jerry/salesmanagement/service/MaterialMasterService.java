package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.dto.ExcelImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MaterialMasterService {
    List<MaterialMaster> getAll();

    MaterialMaster getByUuid(String uuid);

    MaterialMaster create(MaterialMaster materialMaster);

    MaterialMaster update(MaterialMaster materialMaster);

    void delete(String uuid);

    ExcelImportResult importFromExcel(MultipartFile file, String importMode);
}
