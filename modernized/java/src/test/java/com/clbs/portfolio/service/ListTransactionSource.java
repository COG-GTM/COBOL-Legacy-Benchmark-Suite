package com.clbs.portfolio.service;

import com.clbs.portfolio.model.TransactionRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Test double for the sequential {@code TRANSACTION-FILE}: hands out records in order and returns
 * {@code null} once they are exhausted, which is the {@code AT END} phrase of the {@code READ}.
 */
class ListTransactionSource implements TransactionSource {

    private final List<TransactionRecord> records = new ArrayList<>();

    private String openStatus = STATUS_SUCCESS;
    private int position;
    private int closeCount;

    ListTransactionSource(TransactionRecord... transactions) {
        this.records.addAll(Arrays.asList(transactions));
    }

    ListTransactionSource(Collection<TransactionRecord> transactions) {
        this.records.addAll(transactions);
    }

    /** Makes {@code OPEN INPUT} report a failure. */
    ListTransactionSource failingOpen(String status) {
        this.openStatus = status;
        return this;
    }

    @Override
    public String open() {
        return openStatus;
    }

    @Override
    public TransactionRecord read() {
        if (position >= records.size()) {
            return null;
        }
        return records.get(position++);
    }

    @Override
    public void close() {
        closeCount++;
    }

    /** How many records the program actually consumed. */
    int readCount() {
        return position;
    }

    int closeCount() {
        return closeCount;
    }
}
