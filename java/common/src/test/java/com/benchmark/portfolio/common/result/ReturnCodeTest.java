package com.benchmark.portfolio.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ReturnCodeTest {

    @Test
    void mapsEveryCobolReturnCode() {
        Map<Integer, ReturnCode> cobolCodes = Map.of(
            0, ReturnCode.SUCCESS,
            4, ReturnCode.WARNING,
            8, ReturnCode.ERROR,
            12, ReturnCode.SEVERE,
            16, ReturnCode.CRITICAL);
        assertEquals(cobolCodes.size(), ReturnCode.values().length);
        cobolCodes.forEach((value, expected) -> assertSame(expected, ReturnCode.fromCode(value)));
    }

    @ParameterizedTest
    @EnumSource(ReturnCode.class)
    void roundTripsByNumericValue(ReturnCode rc) {
        assertSame(rc, ReturnCode.fromCode(rc.getCode()));
    }

    @ParameterizedTest
    @EnumSource(ReturnCode.class)
    void everyCodeHasDescription(ReturnCode rc) {
        assertFalse(rc.getDescription().isBlank());
    }

    @Test
    void rejectsUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> ReturnCode.fromCode(2));
        assertThrows(IllegalArgumentException.class, () -> ReturnCode.fromCode(-1));
    }

    @Test
    void onlySuccessIsSuccess() {
        assertTrue(ReturnCode.SUCCESS.isSuccess());
        assertFalse(ReturnCode.WARNING.isSuccess());
        assertFalse(ReturnCode.ERROR.isSuccess());
        assertFalse(ReturnCode.SEVERE.isSuccess());
        assertFalse(ReturnCode.CRITICAL.isSuccess());
    }

    @Test
    void statusForMatchesRtncde00SetReturnCodeRanges() {
        assertSame(ReturnStatus.SUCCESS, ReturnCode.statusFor(0));
        for (int code = 1; code <= 4; code++) {
            assertSame(ReturnStatus.WARNING, ReturnCode.statusFor(code));
        }
        for (int code = 5; code <= 8; code++) {
            assertSame(ReturnStatus.ERROR, ReturnCode.statusFor(code));
        }
        assertSame(ReturnStatus.SEVERE, ReturnCode.statusFor(9));
        assertSame(ReturnStatus.SEVERE, ReturnCode.statusFor(12));
        assertSame(ReturnStatus.SEVERE, ReturnCode.statusFor(16));
        assertSame(ReturnStatus.SEVERE, ReturnCode.statusFor(-1));
    }

    @Test
    void toStatusClassifiesEnumValues() {
        assertSame(ReturnStatus.SUCCESS, ReturnCode.SUCCESS.toStatus());
        assertSame(ReturnStatus.WARNING, ReturnCode.WARNING.toStatus());
        assertSame(ReturnStatus.ERROR, ReturnCode.ERROR.toStatus());
        assertSame(ReturnStatus.SEVERE, ReturnCode.SEVERE.toStatus());
        assertSame(ReturnStatus.SEVERE, ReturnCode.CRITICAL.toStatus());
    }
}
