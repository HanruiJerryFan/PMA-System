package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.MaterialMaster;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MaterialMasterMapper {
    MaterialMaster selectByUuid(String uuid);
    MaterialMaster selectByMaterialCode(String materialCode);
    List<MaterialMaster> selectAll();
    String selectMaxMaterialCodeByPrefix(String prefix);
    int insert(MaterialMaster materialMaster);
    int updateByUuid(MaterialMaster materialMaster);
    int deleteByUuid(String uuid);
}
