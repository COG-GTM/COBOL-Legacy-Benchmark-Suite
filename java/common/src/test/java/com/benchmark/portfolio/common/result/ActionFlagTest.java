package com.benchmark.portfolio.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ActionFlagTest {

    @Test
    void mapsEveryCobolActionFlag() {
        Map<Character, ActionFlag> cobolFlags = Map.of(
            'C', ActionFlag.CONTINUE,
            'A', ActionFlag.ABORT,
            'R', ActionFlag.RETRY);
        assertEquals(cobolFlags.size(), ActionFlag.values().length);
        cobolFlags.forEach((c, expected) -> assertSame(expected, ActionFlag.fromChar(c)));
    }

    @ParameterizedTest
    @EnumSource(ActionFlag.class)
    void roundTripsByCharacter(ActionFlag flag) {
        assertSame(flag, ActionFlag.fromChar(flag.getFlagChar()));
    }

    @Test
    void maxRetriesMatchesCobolDefault() {
        assertEquals(3, ActionFlag.MAX_RETRIES);
    }

    @Test
    void rejectsUnknownCharacter() {
        assertThrows(IllegalArgumentException.class, () -> ActionFlag.fromChar('X'));
    }
}
