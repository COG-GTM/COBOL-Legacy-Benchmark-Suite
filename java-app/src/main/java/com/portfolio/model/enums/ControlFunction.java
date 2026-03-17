package com.portfolio.model.enums;

/**
 * Batch control function codes.
 * Migrated from: BCHCTL00.cbl linkage section (lines 49-53).
 * INIT=Initialize, CHEK=Check Prerequisites, UPDT=Update Status, TERM=Terminate
 */
public enum ControlFunction {
    INIT("INIT", "Initialize batch control record"),
    CHEK("CHEK", "Check prerequisites"),
    UPDT("UPDT", "Update batch status"),
    TERM("TERM", "Terminate/finalize batch");

    private final String code;
    private final String description;

    ControlFunction(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ControlFunction fromCode(String code) {
        for (ControlFunction func : values()) {
            if (func.code.equals(code)) {
                return func;
            }
        }
        throw new IllegalArgumentException("Unknown control function code: " + code);
    }
}
