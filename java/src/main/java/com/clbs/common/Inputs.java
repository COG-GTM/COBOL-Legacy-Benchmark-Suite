package com.clbs.common;

import java.util.List;
import java.util.Objects;

/**
 * Input normalisation for the REST adapters. A COBOL input file has no notion of an absent record,
 * so a missing or null JSON element is dropped before the migrated driver loops over it.
 */
public final class Inputs {

    private Inputs() {
    }

    /** The supplied list without null elements; an empty list when the list itself is absent. */
    public static <T> List<T> records(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).toList();
    }

    /** The supplied value, or the empty string when absent — MOVE SPACES semantics. */
    public static String text(String value) {
        return value == null ? "" : value;
    }
}
