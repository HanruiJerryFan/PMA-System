package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Region;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RegionMapper {
    List<Region> selectAll();
    List<Region> selectByLevel(Integer level);
    Region selectById(Integer id);
    Region selectByAreaCode(Long areaCode);
    Region selectByAreaCodePrefix(String codePrefix);
    List<Region> selectByParentCode(Long parentCode);
    int insert(Region region);
    int updateById(Region region);
}
