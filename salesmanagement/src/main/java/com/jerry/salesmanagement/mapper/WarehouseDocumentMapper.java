package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.WarehouseDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseDocumentMapper {
    List<WarehouseDocument> selectAll();
    WarehouseDocument selectByDocNumber(String docNumber);
    Integer selectMaxAnnualSequence(String docNumberPrefix);
    int insert(WarehouseDocument warehouseDocument);
    int update(WarehouseDocument warehouseDocument);
    int updateAuditorByDocNumber(
            @Param("docNumber") String docNumber,
            @Param("auditorUser") Long auditorUser,
            @Param("updateUser") Long updateUser
    );
    int deleteByDocNumber(String docNumber);
}
