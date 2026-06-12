package com.clbs.common.parallelrun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of a record-by-record parallel-run comparison between legacy COBOL
 * output and migrated Java output.
 */
public final class ComparisonResult {

    private int matchedRecords;
    private final List<String> missingInActual = new ArrayList<>();
    private final List<String> extraInActual = new ArrayList<>();
    private final List<FieldDifference> fieldDifferences = new ArrayList<>();

    void incrementMatched() {
        matchedRecords++;
    }

    void addMissingInActual(String key) {
        missingInActual.add(key);
    }

    void addExtraInActual(String key) {
        extraInActual.add(key);
    }

    void addFieldDifference(FieldDifference difference) {
        fieldDifferences.add(difference);
    }

    public int getMatchedRecords() {
        return matchedRecords;
    }

    /** Keys present in the expected (COBOL) output but absent from the Java output. */
    public List<String> getMissingInActual() {
        return Collections.unmodifiableList(missingInActual);
    }

    /** Keys present in the Java output but absent from the expected (COBOL) output. */
    public List<String> getExtraInActual() {
        return Collections.unmodifiableList(extraInActual);
    }

    public List<FieldDifference> getFieldDifferences() {
        return Collections.unmodifiableList(fieldDifferences);
    }

    /** True when both sides matched record-for-record and field-for-field. */
    public boolean isIdentical() {
        return missingInActual.isEmpty() && extraInActual.isEmpty() && fieldDifferences.isEmpty();
    }

    /** Human-readable diff report suitable for logs or CI output. */
    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parallel-run comparison: ")
                .append(isIdentical() ? "IDENTICAL" : "DIFFERENCES FOUND").append('\n');
        sb.append("  matched records : ").append(matchedRecords).append('\n');
        sb.append("  missing in Java : ").append(missingInActual.size()).append('\n');
        sb.append("  extra in Java   : ").append(extraInActual.size()).append('\n');
        sb.append("  field diffs     : ").append(fieldDifferences.size()).append('\n');
        for (String key : missingInActual) {
            sb.append("    - missing: ").append(key).append('\n');
        }
        for (String key : extraInActual) {
            sb.append("    + extra: ").append(key).append('\n');
        }
        for (FieldDifference diff : fieldDifferences) {
            sb.append("    ~ ").append(diff).append('\n');
        }
        return sb.toString();
    }
}
