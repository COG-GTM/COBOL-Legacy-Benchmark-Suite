package com.clbs.common.parallelrun;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generic parallel-run comparison harness (Phase 0, task 0.6).
 *
 * <p>Feeds identical input to the legacy COBOL system and the migrated Java
 * system, then diffs their outputs record-by-record and field-by-field. Each
 * side is supplied as a map of {@code recordKey -> (fieldName -> value)} so the
 * harness is agnostic to how the records were produced (fixed-width COBOL
 * output files, JPA entities, REST responses, etc.).
 */
public final class RecordComparator {

    /**
     * Compare expected (legacy/COBOL) output against actual (migrated/Java) output.
     *
     * @param expected keyed records produced by the COBOL system
     * @param actual   keyed records produced by the Java system
     * @return a {@link ComparisonResult} describing every discrepancy
     */
    public ComparisonResult compare(
            Map<String, Map<String, Object>> expected,
            Map<String, Map<String, Object>> actual) {

        ComparisonResult result = new ComparisonResult();

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(expected.keySet());
        allKeys.addAll(actual.keySet());

        for (String key : allKeys) {
            Map<String, Object> expectedFields = expected.get(key);
            Map<String, Object> actualFields = actual.get(key);

            if (expectedFields == null) {
                result.addExtraInActual(key);
                continue;
            }
            if (actualFields == null) {
                result.addMissingInActual(key);
                continue;
            }

            boolean recordMatches = true;
            Set<String> fieldNames = new LinkedHashSet<>();
            fieldNames.addAll(expectedFields.keySet());
            fieldNames.addAll(actualFields.keySet());

            for (String field : fieldNames) {
                Object expectedValue = expectedFields.get(field);
                Object actualValue = actualFields.get(field);
                if (!Objects.equals(expectedValue, actualValue)) {
                    result.addFieldDifference(
                            new FieldDifference(key, field, expectedValue, actualValue));
                    recordMatches = false;
                }
            }

            if (recordMatches) {
                result.incrementMatched();
            }
        }

        return result;
    }
}
