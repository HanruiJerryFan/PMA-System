package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CodeSequenceMapper;
import com.jerry.salesmanagement.service.CodeSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CodeSequenceServiceImpl implements CodeSequenceService {

    @Autowired
    private CodeSequenceMapper mapper;

    @Override
    @Transactional
    public int nextValue(String sequenceKey, int initialValue, int maxValue, String description) {
        if (!StringUtils.hasText(sequenceKey)) {
            throw new IllegalArgumentException("Sequence key is required");
        }
        if (initialValue < 0) {
            throw new IllegalArgumentException("Sequence initial value must not be negative");
        }
        if (maxValue <= 0) {
            throw new IllegalArgumentException("Sequence max value must be greater than 0");
        }
        if (initialValue > maxValue) {
            throw new IllegalArgumentException("Sequence initial value exceeds max value");
        }

        mapper.insertIfAbsent(sequenceKey, initialValue, description);
        Integer currentValue = mapper.selectCurrentValueForUpdate(sequenceKey);
        if (currentValue == null) {
            throw new IllegalStateException("Sequence row was not created: " + sequenceKey);
        }

        int nextValue = currentValue + 1;
        if (nextValue > maxValue) {
            throw new IllegalArgumentException("Sequence overflow for " + sequenceKey);
        }
        mapper.updateCurrentValue(sequenceKey, nextValue);
        return nextValue;
    }
}
