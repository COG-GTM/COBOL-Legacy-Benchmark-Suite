package com.cog.portfolio.common;

/**
 * Typed replacement for ERRHAND.cpy ERR-MESSAGE / RETHND.cpy RETURN-HANDLING:
 * a return code plus optional error classification and text.
 */
public record ProcessingResult(
        ReturnCode returnCode,
        ErrorCategory category,
        ErrorCode errorCode,
        String message) {

    public static ProcessingResult success() {
        return new ProcessingResult(ReturnCode.SUCCESS, null, null, null);
    }

    public static ProcessingResult warning(String message) {
        return new ProcessingResult(ReturnCode.WARNING, ErrorCategory.PROCESSING, null, message);
    }

    public static ProcessingResult error(ErrorCategory category, ErrorCode code, String message) {
        return new ProcessingResult(ReturnCode.ERROR, category, code, message);
    }

    public static ProcessingResult validationError(String message) {
        return error(ErrorCategory.VALIDATION, ErrorCode.VALIDATION, message);
    }

    public boolean isSuccess() {
        return returnCode == ReturnCode.SUCCESS;
    }

    public boolean isError() {
        return returnCode.code() >= ReturnCode.ERROR.code();
    }
}
