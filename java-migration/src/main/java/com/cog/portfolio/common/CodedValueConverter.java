package com.cog.portfolio.common;

import jakarta.persistence.AttributeConverter;

/** Persists level-88 enums as their COBOL literal code. */
public abstract class CodedValueConverter<E extends Enum<E> & CodedValue>
        implements AttributeConverter<E, String> {

    private final E[] values;

    protected CodedValueConverter(E[] values) {
        this.values = values;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CodedValue.fromCode(values, dbData);
    }
}
