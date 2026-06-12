package com.benchmark.portfolio.common.compare;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A decoded record: an ordered map of field name to value (String for
 * character fields, java.math.BigDecimal for numeric fields) plus the list of
 * key field names used to align records between two streams.
 */
public record ComparableRecord(Map<String, Object> values, List<String> keyFields) {

    public ComparableRecord {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(keyFields, "keyFields");
        values = new LinkedHashMap<>(values);
        keyFields = List.copyOf(keyFields);
        for (String key : keyFields) {
            if (!values.containsKey(key)) {
                throw new IllegalArgumentException("key field missing from record: " + key);
            }
        }
    }

    /** Composite key string used for stream alignment. */
    public String key() {
        return keyFields.stream()
                .map(k -> String.valueOf(values.get(k)))
                .collect(Collectors.joining("|"));
    }

    public Object value(String fieldName) {
        return values.get(fieldName);
    }
}
