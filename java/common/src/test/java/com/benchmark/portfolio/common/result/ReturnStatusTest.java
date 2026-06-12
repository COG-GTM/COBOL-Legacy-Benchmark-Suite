package com.benchmark.portfolio.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ReturnStatusTest {

    @Test
    void mapsEveryCobolStatusCharacter() {
        Map<Character, ReturnStatus> cobolStatuses = Map.of(
            'S', ReturnStatus.SUCCESS,
            'W', ReturnStatus.WARNING,
            'E', ReturnStatus.ERROR,
            'F', ReturnStatus.SEVERE);
        assertEquals(cobolStatuses.size(), ReturnStatus.values().length);
        cobolStatuses.forEach((c, expected) -> assertSame(expected, ReturnStatus.fromChar(c)));
    }

    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    void roundTripsByCharacter(ReturnStatus status) {
        assertSame(status, ReturnStatus.fromChar(status.getStatusChar()));
    }

    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    void everyStatusHasDescription(ReturnStatus status) {
        assertFalse(status.getDescription().isBlank());
    }

    @Test
    void rejectsUnknownCharacter() {
        assertThrows(IllegalArgumentException.class, () -> ReturnStatus.fromChar('X'));
    }
}
