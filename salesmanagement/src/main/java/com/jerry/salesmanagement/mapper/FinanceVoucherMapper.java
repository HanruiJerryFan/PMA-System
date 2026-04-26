package com.jerry.salesmanagement.mapper;

import com.jerry.salesmanagement.pojo.FinanceVoucher;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FinanceVoucherMapper {
    FinanceVoucher selectByUuid(String uuid);
    FinanceVoucher selectByVoucherNo(String voucherNo);
    List<FinanceVoucher> selectAll();
    Integer selectMaxVoucherSequenceByMonthPrefix(String monthPrefix);
    void insert(FinanceVoucher voucher);
    void updateByUuid(FinanceVoucher voucher);
    void deleteByUuid(String uuid);
}
