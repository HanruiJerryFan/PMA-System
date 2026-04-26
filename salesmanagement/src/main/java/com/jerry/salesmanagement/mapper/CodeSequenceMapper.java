package com.jerry.salesmanagement.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CodeSequenceMapper {
    int insertIfAbsent(
            @Param("sequenceKey") String sequenceKey,
            @Param("currentValue") Integer currentValue,
            @Param("description") String description
    );

    Integer selectCurrentValueForUpdate(@Param("sequenceKey") String sequenceKey);

    int updateCurrentValue(
            @Param("sequenceKey") String sequenceKey,
            @Param("currentValue") Integer currentValue
    );
}
