package com.benchmark.portfolio.common.compare;

import java.util.List;

/**
 * Structured result of a parallel-run comparison: per-record diffs plus
 * summary counts of matched, mismatched, and missing records.
 */
public record ComparisonReport(List<RecordDiff> recordDiffs) {

    public ComparisonReport {
        recordDiffs = List.copyOf(recordDiffs);
    }

    public long matchedCount() {
        return countByStatus(RecordDiff.Status.MATCHED);
    }

    public long mismatchedCount() {
        return countByStatus(RecordDiff.Status.MISMATCHED);
    }

    public long missingInActualCount() {
        return countByStatus(RecordDiff.Status.MISSING_IN_ACTUAL);
    }

    public long missingInExpectedCount() {
        return countByStatus(RecordDiff.Status.MISSING_IN_EXPECTED);
    }

    public long totalCount() {
        return recordDiffs.size();
    }

    /** True when every record matched and no records were missing. */
    public boolean identical() {
        return mismatchedCount() == 0 && missingInActualCount() == 0 && missingInExpectedCount() == 0;
    }

    public List<RecordDiff> diffsWithStatus(RecordDiff.Status status) {
        return recordDiffs.stream().filter(d -> d.status() == status).toList();
    }

    /** Human-readable report: summary line followed by one line per non-matching record. */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parallel-run comparison: total=").append(totalCount())
                .append(" matched=").append(matchedCount())
                .append(" mismatched=").append(mismatchedCount())
                .append(" missingInActual=").append(missingInActualCount())
                .append(" missingInExpected=").append(missingInExpectedCount())
                .append(System.lineSeparator());
        for (RecordDiff diff : recordDiffs) {
            if (diff.status() == RecordDiff.Status.MATCHED) {
                continue;
            }
            sb.append("  [").append(diff.status()).append("] key=").append(diff.key());
            for (FieldDiff fieldDiff : diff.fieldDiffs()) {
                sb.append(System.lineSeparator()).append("    ").append(fieldDiff);
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private long countByStatus(RecordDiff.Status status) {
        return recordDiffs.stream().filter(d -> d.status() == status).count();
    }
}
