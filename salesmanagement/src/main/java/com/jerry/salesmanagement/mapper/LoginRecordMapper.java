package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.LoginRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface LoginRecordMapper {
    LoginRecord selectById(Long id);
    List<LoginRecord> selectAll();
    List<LoginRecord> selectByUserId(Long userId);
    List<LoginRecord> selectTimedOutActive(Date cutoffTime);
    List<LoginRecord> selectPage(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    long countPage(@Param("userId") Long userId);
    int insert(LoginRecord record);
    int update(LoginRecord record);
    int touchSession(@Param("id") Long id, @Param("lastSeenAt") Date lastSeenAt);
}
