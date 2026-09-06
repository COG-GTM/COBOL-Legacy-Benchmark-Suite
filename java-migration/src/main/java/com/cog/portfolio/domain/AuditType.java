package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** AUDITLOG.cpy AUD-TYPE level-88 values. */
public enum AuditType implements CodedValue {
    TRANSACTION("TRAN"),
    USER_ACTION("USER"),
    SYSTEM_EVENT("SYST");

    private final String code;

    AuditType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static AuditType fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<AuditType> {
        public Converter() {
            super(AuditType.values());
        }
    }
}
