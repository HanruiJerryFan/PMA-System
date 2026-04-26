package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.FinanceVoucher;

import java.util.List;

public interface FinanceVoucherService {
    FinanceVoucher getByUuid(String uuid);
    List<FinanceVoucher> getAll();
    FinanceVoucher create(FinanceVoucher voucher);
    FinanceVoucher update(FinanceVoucher voucher);
    void delete(String uuid);
}
