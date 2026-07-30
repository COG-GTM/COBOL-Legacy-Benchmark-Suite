package com.cognition.portfolio.transaction.web.dto;

import com.cognition.portfolio.transaction.service.TransactionProcessingResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Result of processing a transaction: the record with its new {@code TRN-STATUS}, the deltas the
 * COBOL would apply to the portfolio master, and the {@code ERR-TEXT} when the record failed.
 */
@Schema(name = "ProcessTransactionResponse", description = "Outcome of PORTTRAN 2100/2200 processing")
public record ProcessTransactionResponse(
    @Schema(description = "Transaction after processing") TransactionResponse transaction,
    @Schema(description = "Delta applied to PORT-TOTAL-UNITS", example = "150.0000") BigDecimal unitsDelta,
    @Schema(description = "Delta applied to PORT-TOTAL-COST", example = "28117.50") BigDecimal costDelta,
    @Schema(description = "Audit action written by PORTTRAN 2300-UPDATE-AUDIT-TRAIL", example = "CREATE")
        String auditAction,
    @Schema(description = "COBOL ERR-TEXT when the record failed", example = "Insufficient units for sale")
        String errorText,
    @Schema(description = "Business rule that rejected the record", example = "BR-10") String ruleId,
    @Schema(description = "Originating COBOL paragraph", example = "PORTTRAN 2220-PROCESS-SELL")
        String cobolParagraph) {

  public static ProcessTransactionResponse from(TransactionProcessingResult result) {
    return new ProcessTransactionResponse(
        TransactionResponse.from(result.transaction()),
        result.effect() == null ? null : result.effect().unitsDelta(),
        result.effect() == null ? null : result.effect().costDelta(),
        result.effect() == null ? null : result.effect().auditAction(),
        result.errorText(),
        result.ruleId(),
        result.cobolParagraph());
  }
}
