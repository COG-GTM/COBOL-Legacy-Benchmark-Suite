package com.benchmark.portfolio.common.compare;

/** A single field-level difference between an expected and an actual record. */
public record FieldDiff(String fieldName, Object expectedValue, Object actualValue) {

    @Override
    public String toString() {
        return fieldName + ": expected=" + expectedValue + " actual=" + actualValue;
    }
}
