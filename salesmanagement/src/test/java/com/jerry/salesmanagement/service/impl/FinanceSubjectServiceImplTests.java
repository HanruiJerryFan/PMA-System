package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.FinanceSubjectMapper;
import com.jerry.salesmanagement.pojo.FinanceSubject;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinanceSubjectServiceImplTests {
    private final FinanceSubjectMapper mapper = mock(FinanceSubjectMapper.class);
    private final FinanceSubjectServiceImpl service = new FinanceSubjectServiceImpl(mapper);

    @Test
    void newSubjectGetsStableGeneratedCodeAndNormalizedName() {
        FinanceSubject subject = subject(null, 1, "CLIENT_CODE", "  自定义一级科目  ");
        service.create(subject);
        assertEquals("自定义一级科目", subject.getSubjectName());
        assertTrue(subject.getSubjectCode().matches("CUSTOM_[a-f0-9]{24}"));
        assertEquals(0, subject.getSortOrder());
        verify(mapper).insert(subject);
    }

    @Test
    void renamingKeepsIdentityLevelAndBuiltinRuleCode() {
        when(mapper.selectById(17L)).thenReturn(subject(17L, 2, "SALARY", "人员工资"));
        when(mapper.update(any())).thenReturn(1);
        FinanceSubject update = subject(17L, null, null, "工资支出");
        FinanceSubject result = service.update(update);
        assertEquals(17L, result.getId());
        assertEquals(2, result.getSubjectLevel());
        assertEquals("SALARY", result.getSubjectCode());
        assertEquals("工资支出", result.getSubjectName());
    }

    @Test
    void referencedSubjectCannotChangeItsLevelOrCode() {
        when(mapper.selectById(17L)).thenReturn(subject(17L, 2, "SALARY", "人员工资"));
        assertThrows(IllegalArgumentException.class, () -> service.update(subject(17L, 1, null, "工资")));
        assertThrows(IllegalArgumentException.class, () -> service.update(subject(17L, 2, "OTHER", "工资")));
        verify(mapper, never()).update(any());
    }

    @Test
    void duplicateNamesAndConcurrentDuplicateInsertAreRejected() {
        when(mapper.selectByName(1, "项目")).thenReturn(subject(2L, 1, "PROJECT", "项目"));
        assertThrows(IllegalArgumentException.class, () -> service.create(subject(null, 1, null, "项目")));
        when(mapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
        assertThrows(IllegalArgumentException.class, () -> service.create(subject(null, 2, null, "新增")));
    }

    @Test
    void validatesSubjectIdAndItsLevelRatherThanFixedCodes() {
        FinanceSubject custom = subject(101L, 1, "CUSTOM_TEST", "自定义");
        when(mapper.selectById(101L)).thenReturn(custom);
        assertSame(custom, service.validateSubject(1, 101L));
        assertThrows(IllegalArgumentException.class, () -> service.validateSubject(2, 101L));
        assertThrows(IllegalArgumentException.class, () -> service.validateSubject(1, null));
        assertThrows(IllegalArgumentException.class, () -> service.validateSubject(1, 999L));
    }

    @Test
    void rejectsEmptyNamesAndUnsupportedLevels() {
        assertThrows(IllegalArgumentException.class, () -> service.create(subject(null, 1, null, "  ")));
        assertThrows(IllegalArgumentException.class, () -> service.create(subject(null, 3, null, "无效")));
        assertThrows(IllegalArgumentException.class, () -> service.create(subject(null, 2, null, "长".repeat(65))));
        verify(mapper, never()).insert(any());
    }

    private FinanceSubject subject(Long id, Integer level, String code, String name) {
        FinanceSubject subject = new FinanceSubject();
        subject.setId(id);
        subject.setSubjectLevel(level);
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        subject.setSortOrder(0);
        return subject;
    }
}
