package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.GoodsInventory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoodsInventoryMapper {
    GoodsInventory selectByUuid(String uuid);
    List<GoodsInventory> selectAll();
}
