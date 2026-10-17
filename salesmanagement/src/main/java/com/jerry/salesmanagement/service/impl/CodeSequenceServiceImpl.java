package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CodeSequenceMapper;
import com.jerry.salesmanagement.service.CodeSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@Service
public class CodeSequenceServiceImpl implements CodeSequenceService {

    @Autowired
    private CodeSequenceMapper mapper;

    @Override
    @Transactional
    public int nextValue(String sequenceKey, Supplier<Integer> currentMaxValue, int maxValue, String description) {
        if (!StringUtils.hasText(sequenceKey)) {
            throw new IllegalArgumentException("Sequence key is required");
        }
        if (maxValue <= 0) {
            throw new IllegalArgumentException("Sequence max value must be greater than 0");
        }
        mapper.insertIfAbsent(sequenceKey, 0, description);
        if (mapper.selectCurrentValueForUpdate(sequenceKey) == null) {
            throw new IllegalStateException("Sequence row was not created: " + sequenceKey);
        }
        // Read after locking: stale snapshots and previously deleted tail numbers must not influence allocation.
        Integer remainingMax = currentMaxValue.get();
        int currentValue = remainingMax == null ? 0 : remainingMax;
        if (currentValue < 0 || currentValue > maxValue) {
            throw new IllegalArgumentException("Existing sequence value is out of range");
        }
        if (currentValue >= maxValue) {
            throw new IllegalArgumentException("Sequence overflow for " + sequenceKey);
        }
        int nextValue = currentValue + 1;
        mapper.updateCurrentValue(sequenceKey, nextValue);
        return nextValue;
    }

    @Override
    @Transactional
    public void deleteAndReclaimTail(String sequenceKey, Runnable deleteAction, Supplier<Integer> remainingMaxValue) {
        if (!StringUtils.hasText(sequenceKey)) {
            throw new IllegalArgumentException("Sequence key is required");
        }
        // Lock before deleting, in the same order as allocation. Keep the lock until the transaction commits.
        mapper.insertIfAbsent(sequenceKey, 0, sequenceKey);
        if (mapper.selectCurrentValueForUpdate(sequenceKey) == null) {
            throw new IllegalStateException("Sequence row was not created: " + sequenceKey);
        }
        deleteAction.run();
        // The caller uses a locking read to see committed rows even under MySQL REPEATABLE READ.
        Integer remainingMax = remainingMaxValue.get();
        if (remainingMax != null && remainingMax < 0) {
            throw new IllegalArgumentException("Remaining sequence value must not be negative");
        }
        mapper.updateCurrentValue(sequenceKey, remainingMax == null ? 0 : remainingMax);
    }
}
