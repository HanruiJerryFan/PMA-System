package com.jerry.salesmanagement.service;

import com.jerry.salesmanagement.pojo.FinanceSubject;
import java.util.List;

public interface FinanceSubjectService {
    List<FinanceSubject> getAll();
    FinanceSubject create(FinanceSubject subject);
    FinanceSubject update(FinanceSubject subject);
    FinanceSubject validateSubject(int level, Long id);
}
