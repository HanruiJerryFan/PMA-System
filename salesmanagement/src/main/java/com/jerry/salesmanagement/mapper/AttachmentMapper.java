package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.Attachment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AttachmentMapper {
    Attachment selectByUuid(String uuid);
    List<Attachment> selectAll();
    List<Attachment> selectByBusiness(String businessType, String businessUuid);
    int insert(Attachment attachment);
    int updateByUuid(Attachment attachment);
    int deleteByUuid(String uuid);
}
