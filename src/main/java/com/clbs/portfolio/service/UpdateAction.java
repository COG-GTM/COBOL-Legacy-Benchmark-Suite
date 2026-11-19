package com.clbs.portfolio.service;

import java.util.Arrays;
import java.util.Optional;

/** UPDT-ACTION level-88 values from PORTUPDT. */
public enum UpdateAction {
    STATUS('S'), VALUE('V'), NAME('N');

    private final char code;

    UpdateAction(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public static Optional<UpdateAction> fromCode(char code) {
        return Arrays.stream(values()).filter(a -> a.code == code).findFirst();
    }
}
