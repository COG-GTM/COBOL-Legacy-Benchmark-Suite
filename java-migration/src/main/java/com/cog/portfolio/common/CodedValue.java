package com.cog.portfolio.common;

/** Enum translated from a COBOL level-88 condition name with its literal code. */
public interface CodedValue {

    String code();

    static <E extends Enum<E> & CodedValue> E fromCode(E[] values, String code) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        String trimmed = code.trim();
        for (E value : values) {
            if (value.code().trim().equals(trimmed)) {
                return value;
            }
        }
        throw new IllegalArgumentException(
                "Unknown code '" + code + "' for " + values[0].getDeclaringClass().getSimpleName());
    }
}
