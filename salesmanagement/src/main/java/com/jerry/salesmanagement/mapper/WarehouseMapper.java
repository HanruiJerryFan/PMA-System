package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Warehouse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WarehouseMapper {
    Warehouse selectById(Long id);
    List<Warehouse> selectAll();
    int insert(Warehouse warehouse);
    int update(Warehouse warehouse);
    int deleteById(Long id);
}
