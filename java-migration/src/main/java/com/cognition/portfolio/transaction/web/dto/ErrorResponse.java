package com.cognition.portfolio.transaction.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error payload carrying the COBOL {@code ERR-TEXT} verbatim plus the rule and paragraph it came
 * from, so a failure in the service can be reconciled with a failure in the legacy job log.
 */
@Schema(name = "Error", description = "Error carrying the original COBOL error text and its origin")
public record ErrorResponse(
    @Schema(description = "COBOL ERR-TEXT or equivalent", example = "Quantity must be greater than zero")
        String message,
    @Schema(description = "Business rule id from MIGRATION-NOTES.md", example = "BR-04") String ruleId,
    @Schema(description = "Originating COBOL paragraph", example = "PORTTRAN 2130-CHECK-AMOUNTS")
        String cobolParagraph) {

  public static ErrorResponse of(String message) {
    return new ErrorResponse(message, null, null);
  }
}
