package com.benchmark.portfolio.common.result;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generic success/failure wrapper carrying a {@link ReturnCode} and message,
 * replacing the COBOL convention of passing {@code RETURN-CODE} and
 * {@code RC-MESSAGE} fields (see {@code src/copybook/common/RTNCODE.cpy} and
 * {@code src/copybook/common/RETHND.cpy}) between programs.
 *
 * @param <T> the type of the value carried on success
 */
public final class Result<T> {

    private final T value;
    private final ReturnCode returnCode;
    private final String message;

    private Result(T value, ReturnCode returnCode, String message) {
        this.value = value;
        this.returnCode = Objects.requireNonNull(returnCode, "returnCode");
        this.message = message;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, ReturnCode.SUCCESS, null);
    }

    public static <T> Result<T> success(T value, String message) {
        return new Result<>(value, ReturnCode.SUCCESS, message);
    }

    public static <T> Result<T> failure(ReturnCode returnCode, String message) {
        if (returnCode == ReturnCode.SUCCESS) {
            throw new IllegalArgumentException("Failure result cannot use SUCCESS return code");
        }
        return new Result<>(null, returnCode, message);
    }

    public boolean isSuccess() {
        return returnCode.isSuccess();
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    /**
     * @throws NoSuchElementException if this result is a failure
     */
    public T getValue() {
        if (isFailure()) {
            throw new NoSuchElementException(
                "No value present; failed with " + returnCode + ": " + message);
        }
        return value;
    }

    public Optional<T> toOptional() {
        return isSuccess() ? Optional.ofNullable(value) : Optional.empty();
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public T orElse(T other) {
        return isSuccess() ? value : other;
    }

    /** Transforms the value on success; propagates the failure unchanged. */
    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isFailure()) {
            return new Result<>(null, returnCode, message);
        }
        return new Result<>(mapper.apply(value), returnCode, message);
    }

    /** Chains another result-producing operation on success; propagates the failure unchanged. */
    public <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isFailure()) {
            return new Result<>(null, returnCode, message);
        }
        return Objects.requireNonNull(mapper.apply(value), "mapper result");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Result<?> other)) {
            return false;
        }
        return Objects.equals(value, other.value)
            && returnCode == other.returnCode
            && Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, returnCode, message);
    }

    @Override
    public String toString() {
        return isSuccess()
            ? "Result.success(" + value + ")"
            : "Result.failure(" + returnCode + ", " + message + ")";
    }
}
