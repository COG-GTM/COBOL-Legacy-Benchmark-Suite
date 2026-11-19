package com.clbs.portfolio.service;

import java.util.Arrays;
import java.util.Optional;

/** DEL-REASON-CODE level-88 values from PORTDEL. */
public enum DeleteReason {
    CLOSED("01"), TRANSFERRED("02"), REQUESTED("03");

    private final String code;

    DeleteReason(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<DeleteReason> fromCode(String code) {
        return Arrays.stream(values()).filter(r -> r.code.equals(code)).findFirst();
    }
}
