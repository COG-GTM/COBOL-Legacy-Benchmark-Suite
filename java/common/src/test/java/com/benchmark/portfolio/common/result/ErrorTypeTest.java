package com.benchmark.portfolio.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ErrorTypeTest {

    @Test
    void mapsEveryCobolErrorType() {
        Map<Character, ErrorType> cobolTypes = Map.of(
            'V', ErrorType.VALIDATION,
            'P', ErrorType.PROCESSING,
            'D', ErrorType.DATABASE,
            'F', ErrorType.FILE,
            'S', ErrorType.SECURITY);
        assertEquals(cobolTypes.size(), ErrorType.values().length);
        cobolTypes.forEach((c, expected) -> assertSame(expected, ErrorType.fromChar(c)));
    }

    @ParameterizedTest
    @EnumSource(ErrorType.class)
    void roundTripsByCharacter(ErrorType type) {
        assertSame(type, ErrorType.fromChar(type.getTypeChar()));
    }

    @Test
    void rejectsUnknownCharacter() {
        assertThrows(IllegalArgumentException.class, () -> ErrorType.fromChar('X'));
    }
}
