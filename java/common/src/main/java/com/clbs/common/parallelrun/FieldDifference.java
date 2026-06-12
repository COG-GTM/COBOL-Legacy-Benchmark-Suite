package com.clbs.common.parallelrun;

/**
 * A single field-level discrepancy found when diffing a legacy (COBOL) record
 * against the migrated (Java) record for the same key.
 */
public record FieldDifference(String recordKey, String fieldName, Object expected, Object actual) {

    @Override
    public String toString() {
        return "[" + recordKey + "] " + fieldName + ": expected=<" + expected + "> actual=<" + actual + ">";
    }
}
