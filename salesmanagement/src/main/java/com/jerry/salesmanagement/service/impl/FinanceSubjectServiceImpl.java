package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.FinanceSubjectMapper;
import com.jerry.salesmanagement.pojo.FinanceSubject;
import com.jerry.salesmanagement.service.FinanceSubjectService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class FinanceSubjectServiceImpl implements FinanceSubjectService {
    private final FinanceSubjectMapper mapper;

    public FinanceSubjectServiceImpl(FinanceSubjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<FinanceSubject> getAll() {
        return mapper.selectAll();
    }

    @Override
    @Transactional
    public FinanceSubject create(FinanceSubject subject) {
        subject.setId(null);
        subject.setSubjectCode("CUSTOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        validateDefinition(subject);
        try {
            mapper.insert(subject);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("同一级别的科目名称不能重复");
        }
        return subject;
    }

    @Override
    @Transactional
    public FinanceSubject update(FinanceSubject subject) {
        FinanceSubject existing = mapper.selectById(subject.getId());
        if (existing == null) {
            throw new IllegalArgumentException("科目不存在");
        }
        if ((subject.getSubjectLevel() != null && !subject.getSubjectLevel().equals(existing.getSubjectLevel()))
                || (subject.getSubjectCode() != null && !subject.getSubjectCode().equals(existing.getSubjectCode()))) {
            throw new IllegalArgumentException("科目级别和编码不能修改");
        }
        subject.setSubjectLevel(existing.getSubjectLevel());
        subject.setSubjectCode(existing.getSubjectCode());
        if (subject.getSortOrder() == null) {
            subject.setSortOrder(existing.getSortOrder());
        }
        validateDefinition(subject);
        try {
            if (mapper.update(subject) == 0) {
                throw new IllegalArgumentException("科目不存在或已被修改，请刷新后重试");
            }
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("同一级别的科目名称不能重复");
        }
        return subject;
    }

    @Override
    public FinanceSubject validateSubject(int level, Long id) {
        FinanceSubject subject = id == null ? null : mapper.selectById(id);
        if (subject == null || !Integer.valueOf(level).equals(subject.getSubjectLevel())) {
            throw new IllegalArgumentException(level == 1 ? "请选择有效的一级科目" : "请选择有效的二级科目");
        }
        return subject;
    }

    private void validateDefinition(FinanceSubject subject) {
        if (subject.getSubjectLevel() == null || (subject.getSubjectLevel() != 1 && subject.getSubjectLevel() != 2)) {
            throw new IllegalArgumentException("科目级别只能为一级或二级");
        }
        if (!StringUtils.hasText(subject.getSubjectName()) || subject.getSubjectName().trim().length() > 64) {
            throw new IllegalArgumentException("科目名称不能为空，且不能超过64个字符");
        }
        subject.setSubjectName(subject.getSubjectName().trim());
        if (subject.getSortOrder() == null) {
            subject.setSortOrder(0);
        }
        if (subject.getSortOrder() < 0) {
            throw new IllegalArgumentException("排序值不能为负数");
        }
        FinanceSubject duplicate = mapper.selectByName(subject.getSubjectLevel(), subject.getSubjectName());
        if (duplicate != null && !duplicate.getId().equals(subject.getId())) {
            throw new IllegalArgumentException("同一级别的科目名称不能重复");
        }
    }
}
