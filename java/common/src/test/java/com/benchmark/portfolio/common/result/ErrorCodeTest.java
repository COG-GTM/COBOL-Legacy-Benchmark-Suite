package com.benchmark.portfolio.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ErrorCodeTest {

    @Test
    void mapsEveryCobolStdErrorCode() {
        Map<String, ErrorCode> cobolCodes = Map.of(
            "E001", ErrorCode.INVALID_DATA,
            "E002", ErrorCode.NOT_FOUND,
            "E003", ErrorCode.DUPLICATE,
            "E004", ErrorCode.FILE_ERROR,
            "E005", ErrorCode.DB_ERROR,
            "E006", ErrorCode.SECURITY,
            "E007", ErrorCode.PROCESSING,
            "E008", ErrorCode.VALIDATION,
            "E009", ErrorCode.VERSION,
            "E010", ErrorCode.TIMEOUT);
        assertEquals(cobolCodes.size(), ErrorCode.values().length);
        cobolCodes.forEach((code, expected) -> assertSame(expected, ErrorCode.fromCode(code)));
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void roundTripsByCodeString(ErrorCode ec) {
        assertSame(ec, ErrorCode.fromCode(ec.getCode()));
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCodeHasDescription(ErrorCode ec) {
        assertFalse(ec.getDescription().isBlank());
    }

    @Test
    void rejectsUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> ErrorCode.fromCode("E011"));
    }
}
