package com.portfolio.portmstr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for batch job execution results.
 * Provides equivalent information to COBOL batch job completion output.
 */
@Schema(description = "Batch job execution result")
public record BatchJobResponse(
        @Schema(description = "Job execution ID") long executionId,
        @Schema(description = "Job name") String jobName,
        @Schema(description = "Batch status") String status,
        @Schema(description = "Records read") long recordsRead,
        @Schema(description = "Records processed") long recordsProcessed,
        @Schema(description = "Records with errors") long recordsError,
        @Schema(description = "Return code (0=success, 4=warning, 8=error)") int returnCode,
        @Schema(description = "Descriptive message") String message
) {
}
