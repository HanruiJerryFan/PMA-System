package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseDocumentItemMapper {
    List<WarehouseDocumentItem> selectByDocNumber(String docNumber);
    List<WarehouseDocumentItem> selectByDocNumbers(@Param("docNumbers") List<String> docNumbers);
    int insert(WarehouseDocumentItem warehouseDocumentItem);
    int deleteByDocNumber(String docNumber);
}
