package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** AUDITLOG.cpy AUD-ACTION level-88 values. */
public enum AuditAction implements CodedValue {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    INQUIRE("INQUIRE"),
    LOGIN("LOGIN"),
    LOGOUT("LOGOUT"),
    STARTUP("STARTUP"),
    SHUTDOWN("SHUTDOWN");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static AuditAction fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<AuditAction> {
        public Converter() {
            super(AuditAction.values());
        }
    }
}
