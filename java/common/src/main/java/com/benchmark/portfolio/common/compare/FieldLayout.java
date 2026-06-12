package com.benchmark.portfolio.common.compare;

import java.util.Objects;

/**
 * Descriptor for a single field within a fixed-width record: byte offset,
 * byte length, data type, and (for numeric types) the implied decimal scale.
 */
public record FieldLayout(String name, int offset, int length, FieldType type, int scale) {

    public FieldLayout {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0: " + name);
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0: " + name);
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale must be >= 0: " + name);
        }
    }

    public static FieldLayout character(String name, int offset, int length) {
        return new FieldLayout(name, offset, length, FieldType.CHAR, 0);
    }

    public static FieldLayout zoned(String name, int offset, int length, int scale) {
        return new FieldLayout(name, offset, length, FieldType.ZONED, scale);
    }

    public static FieldLayout packed(String name, int offset, int length, int scale) {
        return new FieldLayout(name, offset, length, FieldType.PACKED, scale);
    }
}
