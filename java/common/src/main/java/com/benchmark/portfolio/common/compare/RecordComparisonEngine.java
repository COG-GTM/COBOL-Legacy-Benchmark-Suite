package com.benchmark.portfolio.common.compare;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parallel-run record comparison engine. Aligns an expected (legacy COBOL
 * output) and an actual (new Java output) record stream by key, then diffs
 * each aligned pair field-by-field. Numeric fields are compared with
 * BigDecimal exactness ({@code compareTo}, so 1.10 equals 1.1000); all other
 * values are compared with {@code equals}.
 *
 * <p>Records may come from any source — fixed-width files via
 * {@link FixedWidthRecordReader}, CSV files via {@link CsvRecordReader}, or
 * in-memory DTOs converted to {@link ComparableRecord}s.</p>
 */
public final class RecordComparisonEngine {

    private final Set<String> ignoredFields;

    public RecordComparisonEngine() {
        this(Set.of());
    }

    /** @param ignoredFields field names excluded from comparison (e.g. FILLER). */
    public RecordComparisonEngine(Set<String> ignoredFields) {
        this.ignoredFields = Set.copyOf(ignoredFields);
    }

    public ComparisonReport compare(List<ComparableRecord> expected, List<ComparableRecord> actual) {
        Map<String, ComparableRecord> expectedByKey = indexByKey(expected, "expected");
        Map<String, ComparableRecord> actualByKey = indexByKey(actual, "actual");

        Set<String> allKeys = new LinkedHashSet<>(expectedByKey.keySet());
        allKeys.addAll(actualByKey.keySet());

        List<RecordDiff> diffs = new ArrayList<>(allKeys.size());
        for (String key : allKeys) {
            ComparableRecord expectedRecord = expectedByKey.get(key);
            ComparableRecord actualRecord = actualByKey.get(key);
            if (expectedRecord == null) {
                diffs.add(new RecordDiff(key, RecordDiff.Status.MISSING_IN_EXPECTED, List.of()));
            } else if (actualRecord == null) {
                diffs.add(new RecordDiff(key, RecordDiff.Status.MISSING_IN_ACTUAL, List.of()));
            } else {
                List<FieldDiff> fieldDiffs = diffFields(expectedRecord, actualRecord);
                diffs.add(new RecordDiff(key,
                        fieldDiffs.isEmpty() ? RecordDiff.Status.MATCHED : RecordDiff.Status.MISMATCHED,
                        fieldDiffs));
            }
        }
        return new ComparisonReport(diffs);
    }

    private List<FieldDiff> diffFields(ComparableRecord expected, ComparableRecord actual) {
        Set<String> fieldNames = new LinkedHashSet<>(expected.values().keySet());
        fieldNames.addAll(actual.values().keySet());

        List<FieldDiff> fieldDiffs = new ArrayList<>();
        for (String fieldName : fieldNames) {
            if (ignoredFields.contains(fieldName)) {
                continue;
            }
            Object expectedValue = expected.value(fieldName);
            Object actualValue = actual.value(fieldName);
            if (!valuesEqual(expectedValue, actualValue)) {
                fieldDiffs.add(new FieldDiff(fieldName, expectedValue, actualValue));
            }
        }
        return fieldDiffs;
    }

    private boolean valuesEqual(Object expected, Object actual) {
        if (expected == null || actual == null) {
            return expected == actual;
        }
        if (expected instanceof BigDecimal e && actual instanceof BigDecimal a) {
            return e.compareTo(a) == 0;
        }
        return expected.equals(actual);
    }

    private Map<String, ComparableRecord> indexByKey(List<ComparableRecord> records, String side) {
        Map<String, ComparableRecord> byKey = new LinkedHashMap<>();
        for (ComparableRecord record : records) {
            ComparableRecord previous = byKey.put(record.key(), record);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate key in " + side + " stream: " + record.key());
            }
        }
        return byKey;
    }
}
