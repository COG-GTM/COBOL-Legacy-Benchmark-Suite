package com.clbs.portfolio.model;

/** Level-88 conditions on {@code PORT-CLIENT-TYPE PIC X(1)} in {@code PORTFLIO.cpy}. */
public enum ClientType {

    /** {@code 88 PORT-INDIVIDUAL VALUE 'I'}. */
    INDIVIDUAL("I"),
    /** {@code 88 PORT-CORPORATE VALUE 'C'}. */
    CORPORATE("C"),
    /** {@code 88 PORT-TRUST VALUE 'T'}. */
    TRUST("T");

    public static final int LENGTH = 1;

    private final String code;

    ClientType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The matching type, or {@code null} when the buffer holds an uncovered value. */
    public static ClientType fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (ClientType type : values()) {
            if (type.code.equals(stored)) {
                return type;
            }
        }
        return null;
    }
}
