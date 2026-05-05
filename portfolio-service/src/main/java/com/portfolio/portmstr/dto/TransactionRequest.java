package com.portfolio.portmstr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for transaction processing.
 * Maps to COBOL TRNREC.cpy TRANSACTION-RECORD fields.
 */
@Schema(description = "Transaction processing request")
public record TransactionRequest(
        @Schema(description = "Portfolio ID", example = "PORT0001")
        @NotBlank
        @Size(max = 8)
        String portfolioId,

        @Schema(description = "Investment ID", example = "INV0000001")
        @NotBlank
        @Size(max = 10)
        String investmentId,

        @Schema(description = "Transaction type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee", example = "BU")
        @NotNull
        @Pattern(regexp = "^(BU|SL|TR|FE)$", message = "Transaction type must be BU, SL, TR, or FE")
        String transactionType,

        @Schema(description = "Quantity", example = "100.0000")
        @NotNull
        @Positive
        BigDecimal quantity,

        @Schema(description = "Price per unit", example = "50.2500")
        @NotNull
        BigDecimal price,

        @Schema(description = "Total amount", example = "5025.00")
        @NotNull
        BigDecimal amount,

        @Schema(description = "Currency code", example = "USD")
        @Size(max = 3)
        String currencyCode
) {
}
