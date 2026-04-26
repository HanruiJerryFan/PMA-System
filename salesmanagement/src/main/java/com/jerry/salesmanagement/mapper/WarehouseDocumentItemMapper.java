package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WarehouseDocumentItemMapper {
    List<WarehouseDocumentItem> selectByDocNumber(String docNumber);
    int insert(WarehouseDocumentItem warehouseDocumentItem);
    int deleteByDocNumber(String docNumber);
}
