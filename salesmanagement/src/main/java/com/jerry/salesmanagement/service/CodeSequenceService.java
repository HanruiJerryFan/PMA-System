package com.jerry.salesmanagement.service;

import java.util.function.Supplier;

public interface CodeSequenceService {
    // The caller must save the numbered record in the same transaction; the supplier must use a locking read.
    int nextValue(String sequenceKey, Supplier<Integer> currentMaxValue, int maxValue, String description);

    void deleteAndReclaimTail(String sequenceKey, Runnable deleteAction, Supplier<Integer> remainingMaxValue);

    void withLock(String sequenceKey, Runnable action);
}
