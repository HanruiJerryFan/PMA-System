package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.InventoryTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryTransactionMapper {
    List<InventoryTransaction> selectAll();
    InventoryTransaction selectByUuid(String uuid);
    List<InventoryTransaction> selectBySourceRef(@Param("sourceRefType") String sourceRefType, @Param("sourceRefId") String sourceRefId);
    int deleteBySourceRef(@Param("sourceRefType") String sourceRefType, @Param("sourceRefId") String sourceRefId);
    int insert(InventoryTransaction inventoryTransaction);
    int updateByUuid(InventoryTransaction inventoryTransaction);
    int deleteByUuid(String uuid);
}
