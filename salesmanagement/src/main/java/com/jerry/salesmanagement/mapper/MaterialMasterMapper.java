package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.MaterialMaster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialMasterMapper {
    MaterialMaster selectByUuid(String uuid);
    MaterialMaster selectByMaterialCode(String materialCode);
    List<MaterialMaster> selectAll();
    String selectMaxMaterialCodeByPrefix(String prefix);
    Integer selectMaxMaterialSequenceForUpdate(
            @Param("categoryCode") String categoryCode, @Param("subcategoryCode") String subcategoryCode,
            @Param("brandCode") String brandCode, @Param("bandCode") String bandCode,
            @Param("sequenceLength") int sequenceLength);
    int insert(MaterialMaster materialMaster);
    int updateByUuid(MaterialMaster materialMaster);
    int deleteByUuid(String uuid);
}
