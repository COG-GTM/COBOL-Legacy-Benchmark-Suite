package com.clbs.domain;

/** COMMON.cpy RETURN-CODES / BCHCON.cpy BCT-RC-* severity levels. */
public final class ReturnCode {

    public static final int SUCCESS = 0;
    public static final int WARNING = 4;
    public static final int ERROR = 8;
    public static final int SEVERE = 12;
    public static final int CRITICAL = 16;

    private ReturnCode() {
    }

    /** RTNCDE00 P200-SET-RETURN-CODE severity classification. */
    public static Status classify(int code) {
        if (code == 0) {
            return Status.SUCCESS;
        }
        if (code >= 1 && code <= 4) {
            return Status.WARNING;
        }
        if (code >= 5 && code <= 8) {
            return Status.ERROR;
        }
        return Status.SEVERE;
    }

    public enum Status {
        SUCCESS('S'),
        WARNING('W'),
        ERROR('E'),
        SEVERE('F');

        private final char code;

        Status(char code) {
            this.code = code;
        }

        public char code() {
            return code;
        }
    }
}
