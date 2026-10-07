package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.FinanceSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FinanceSubjectMapper {
    List<FinanceSubject> selectAll();
    FinanceSubject selectById(Long id);
    FinanceSubject selectByCode(@Param("subjectLevel") Integer subjectLevel, @Param("subjectCode") String subjectCode);
    FinanceSubject selectByName(@Param("subjectLevel") Integer subjectLevel, @Param("subjectName") String subjectName);
    int insert(FinanceSubject subject);
    int update(FinanceSubject subject);
}
