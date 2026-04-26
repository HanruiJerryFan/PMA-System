package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ContractBasicInfoMapper {
    ContractBasicInfo selectByUuid(String uuid);
    List<ContractBasicInfo> selectAll();
    int insert(ContractBasicInfo info);
    int updateByUuid(ContractBasicInfo info);
    int deleteByUuid(String uuid);
}
