package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** AUDITLOG.cpy AUD-STATUS level-88 values. */
public enum AuditStatus implements CodedValue {
    SUCCESS("SUCC"),
    FAILURE("FAIL"),
    WARNING("WARN");

    private final String code;

    AuditStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static AuditStatus fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<AuditStatus> {
        public Converter() {
            super(AuditStatus.values());
        }
    }
}
