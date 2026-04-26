package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.LoginRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface LoginRecordMapper {
    LoginRecord selectById(Long id);
    List<LoginRecord> selectAll();
    List<LoginRecord> selectByUserId(Long userId);
    List<LoginRecord> selectTimedOutActive(Date cutoffTime);
    int insert(LoginRecord record);
    int update(LoginRecord record);
}
