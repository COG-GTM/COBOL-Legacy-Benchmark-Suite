package com.clbs.domain;

/** PORTFLIO.cpy PORT-CLIENT-TYPE 88-levels (I/C/T). */
public enum ClientType {
    INDIVIDUAL('I'),
    CORPORATE('C'),
    TRUST('T');

    private final char code;

    ClientType(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public static ClientType fromCode(char code) {
        for (ClientType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
