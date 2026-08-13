package com.ipms.domain;

/** PORT-CLIENT-TYPE level-88 values from PORTFLIO.cpy (I=INDIVIDUAL, C=CORPORATE, T=TRUST). */
public enum ClientType {
    INDIVIDUAL("I"),
    CORPORATE("C"),
    TRUST("T");

    private final String code;

    ClientType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ClientType fromCode(String code) {
        for (ClientType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown client type: " + code);
    }
}
