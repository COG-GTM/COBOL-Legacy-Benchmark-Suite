package com.cognition.portfolio.transaction.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Status transition payload for {@code TRN-STATUS} (88-levels P/D/F/R). Allowed transitions are the
 * derived rule BR-23.
 */
@Schema(name = "StatusTransitionRequest", description = "TRN-STATUS transition (BR-23)")
public record StatusTransitionRequest(
    @Schema(description = "Target TRN-STATUS PIC X(01)", example = "D", allowableValues = {"P", "D", "F", "R"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String status,
    @Schema(description = "TRN-PROCESS-USER PIC X(08) applying the change", example = "BATCH001")
        @Size(max = 8) String processUser) {}
