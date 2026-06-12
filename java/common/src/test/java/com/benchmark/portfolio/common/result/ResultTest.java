package com.benchmark.portfolio.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void successCarriesValueAndSuccessCode() {
        Result<String> result = Result.success("PORT1234");
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("PORT1234", result.getValue());
        assertSame(ReturnCode.SUCCESS, result.getReturnCode());
        assertEquals(Optional.empty(), result.getMessage());
    }

    @Test
    void successWithMessage() {
        Result<Integer> result = Result.success(42, "loaded");
        assertTrue(result.isSuccess());
        assertEquals(Optional.of("loaded"), result.getMessage());
    }

    @Test
    void failureCarriesCodeAndMessage() {
        Result<String> result = Result.failure(ReturnCode.ERROR, "Invalid portfolio ID");
        assertTrue(result.isFailure());
        assertSame(ReturnCode.ERROR, result.getReturnCode());
        assertEquals(Optional.of("Invalid portfolio ID"), result.getMessage());
        assertThrows(NoSuchElementException.class, result::getValue);
        assertEquals(Optional.empty(), result.toOptional());
        assertEquals("fallback", result.orElse("fallback"));
    }

    @Test
    void failureRejectsSuccessCode() {
        assertThrows(IllegalArgumentException.class,
            () -> Result.failure(ReturnCode.SUCCESS, "not a failure"));
    }

    @Test
    void failureAllowedForEveryNonSuccessCode() {
        for (ReturnCode rc : ReturnCode.values()) {
            if (rc != ReturnCode.SUCCESS) {
                assertSame(rc, Result.failure(rc, "msg").getReturnCode());
            }
        }
    }

    @Test
    void mapTransformsSuccess() {
        Result<Integer> result = Result.success(21).map(v -> v * 2);
        assertEquals(42, result.getValue());
    }

    @Test
    void mapPropagatesFailure() {
        Result<Integer> failure = Result.failure(ReturnCode.SEVERE, "boom");
        Result<String> mapped = failure.map(String::valueOf);
        assertTrue(mapped.isFailure());
        assertSame(ReturnCode.SEVERE, mapped.getReturnCode());
        assertEquals(Optional.of("boom"), mapped.getMessage());
    }

    @Test
    void flatMapChainsSuccess() {
        Result<Integer> result = Result.success("42").flatMap(s -> Result.success(Integer.parseInt(s)));
        assertEquals(42, result.getValue());
    }

    @Test
    void flatMapPropagatesFailure() {
        Result<String> failure = Result.failure(ReturnCode.WARNING, "warn");
        Result<Integer> chained = failure.flatMap(s -> Result.success(1));
        assertTrue(chained.isFailure());
        assertSame(ReturnCode.WARNING, chained.getReturnCode());
    }

    @Test
    void equalsAndHashCode() {
        assertEquals(Result.success("a"), Result.success("a"));
        assertEquals(Result.failure(ReturnCode.ERROR, "m"), Result.failure(ReturnCode.ERROR, "m"));
        assertFalse(Result.success("a").equals(Result.success("b")));
        assertEquals(Result.success("a").hashCode(), Result.success("a").hashCode());
    }
}
