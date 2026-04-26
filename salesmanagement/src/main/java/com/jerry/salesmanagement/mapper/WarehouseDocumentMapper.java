package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.WarehouseDocument;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WarehouseDocumentMapper {
    List<WarehouseDocument> selectAll();
    WarehouseDocument selectByDocNumber(String docNumber);
    Integer selectMaxAnnualSequence(String docNumberPrefix);
    int insert(WarehouseDocument warehouseDocument);
    int update(WarehouseDocument warehouseDocument);
    int deleteByDocNumber(String docNumber);
}
