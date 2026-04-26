package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ClauseType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ClauseTypeMapper {
    ClauseType selectById(Long id);
    List<ClauseType> selectAll();
}
