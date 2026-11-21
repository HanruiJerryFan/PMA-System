package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.InventoryTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryTransactionMapper {
    List<InventoryTransaction> selectAll();
    List<InventoryTransaction> selectPage(
            @Param("materialCode") String materialCode,
            @Param("materialName") String materialName,
            @Param("category") String category,
            @Param("txnType") String txnType,
            @Param("projectId") String projectId,
            @Param("warehouseId") Long warehouseId,
            @Param("sourceRefId") String sourceRefId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    long countPage(
            @Param("materialCode") String materialCode,
            @Param("materialName") String materialName,
            @Param("category") String category,
            @Param("txnType") String txnType,
            @Param("projectId") String projectId,
            @Param("warehouseId") Long warehouseId,
            @Param("sourceRefId") String sourceRefId
    );
    InventoryTransaction selectByUuid(String uuid);
    List<InventoryTransaction> selectBySourceRef(@Param("sourceRefType") String sourceRefType, @Param("sourceRefId") String sourceRefId);
    int deleteBySourceRef(@Param("sourceRefType") String sourceRefType, @Param("sourceRefId") String sourceRefId);
    int insert(InventoryTransaction inventoryTransaction);
    int updateByUuid(InventoryTransaction inventoryTransaction);
    int deleteByUuid(String uuid);
}
