package com.jerry.salesmanagement.service;

public interface CodeSequenceService {
    int nextValue(String sequenceKey, int initialValue, int maxValue, String description);
}
