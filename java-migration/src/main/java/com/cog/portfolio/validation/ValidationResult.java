package com.cog.portfolio.validation;

/** PORTVAL.cpy VAL-RETURN-CODES + VAL-ERROR-MESSAGES. */
public record ValidationResult(Code code, String message) {

    public enum Code {
        SUCCESS(0),
        INVALID_ID(1),
        INVALID_ACCOUNT(2),
        INVALID_TYPE(3),
        INVALID_AMOUNT(4);

        private final int value;

        Code(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public static final String ERR_ID = "Invalid Portfolio ID format";
    public static final String ERR_ACCOUNT = "Invalid Account Number format";
    public static final String ERR_TYPE = "Invalid Investment Type";
    public static final String ERR_AMOUNT = "Amount outside valid range";

    public static ValidationResult success() {
        return new ValidationResult(Code.SUCCESS, "");
    }

    public boolean isValid() {
        return code == Code.SUCCESS;
    }
}
