package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.FinanceVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FinanceVoucherMapper {
    FinanceVoucher selectByUuid(String uuid);
    FinanceVoucher selectByVoucherNo(String voucherNo);
    List<FinanceVoucher> selectAll();
    Integer selectMaxVoucherSequenceByMonthPrefix(String monthPrefix);
    void insert(FinanceVoucher voucher);
    void updateByUuid(FinanceVoucher voucher);
    void updateAuditorByUuid(
            @Param("uuid") String uuid,
            @Param("auditorUser") Long auditorUser,
            @Param("updatedBy") Long updatedBy
    );
    void deleteByUuid(String uuid);
}
