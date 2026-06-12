package com.benchmark.portfolio.common.compare;

import java.util.List;

/**
 * Per-record comparison outcome, keyed by the record's alignment key.
 */
public record RecordDiff(String key, Status status, List<FieldDiff> fieldDiffs) {

    public enum Status {
        /** Record present in both streams with all fields equal. */
        MATCHED,
        /** Record present in both streams with one or more field differences. */
        MISMATCHED,
        /** Record present only in the expected (legacy) stream. */
        MISSING_IN_ACTUAL,
        /** Record present only in the actual (new) stream. */
        MISSING_IN_EXPECTED
    }

    public RecordDiff {
        fieldDiffs = List.copyOf(fieldDiffs);
    }
}
