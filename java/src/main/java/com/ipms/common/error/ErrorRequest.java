package com.ipms.common.error;

import com.ipms.domain.ErrorCategory;

/**
 * LS-ERROR-REQUEST linkage structure from {@code src/programs/common/ERRPROC.cbl}:
 * LS-PROGRAM-ID X(8), LS-CATEGORY X(2), LS-ERROR-CODE X(4), LS-SEVERITY S9(4) COMP,
 * LS-ERROR-TEXT X(80), LS-ERROR-DETAILS X(256). The LS-RETURN-CODE output field is
 * returned by {@link ErrorLoggingService#processError(ErrorRequest)} instead.
 */
public record ErrorRequest(
        String programId,
        ErrorCategory category,
        String errorCode,
        int severity,
        String errorText,
        String errorDetails) {

    public ErrorRequest {
        if (programId == null || programId.isBlank()) {
            throw new IllegalArgumentException("programId is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode is required");
        }
    }
}
