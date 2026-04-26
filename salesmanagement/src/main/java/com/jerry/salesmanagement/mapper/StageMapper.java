package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Stage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StageMapper {
    Stage selectById(Long id);
    List<Stage> selectAll();
    List<Stage> selectActive();
    int insert(Stage stage);
    int update(Stage stage);
    int deleteById(Long id);
}
