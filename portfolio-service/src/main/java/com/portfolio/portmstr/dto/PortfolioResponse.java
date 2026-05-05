package com.portfolio.portmstr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for Portfolio data.
 * Maps to COBOL PORTFOLIO-RECORD output in LS-PORTFOLIO.
 */
@Schema(description = "Portfolio data response")
public record PortfolioResponse(
        @Schema(description = "Portfolio ID") String portfolioId,
        @Schema(description = "Account number") String accountNo,
        @Schema(description = "Client name") String clientName,
        @Schema(description = "Client type") String clientType,
        @Schema(description = "Portfolio status") String status,
        @Schema(description = "Total value") BigDecimal totalValue,
        @Schema(description = "Cash balance") BigDecimal cashBalance,
        @Schema(description = "Currency code") String currencyCode,
        @Schema(description = "Creation date") LocalDate createDate,
        @Schema(description = "Last maintenance date") LocalDate lastMaintDate,
        @Schema(description = "Last maintenance timestamp") LocalDateTime lastMaintTimestamp,
        @Schema(description = "Last user") String lastUser,
        @Schema(description = "Return code (0=success)") int returnCode,
        @Schema(description = "Error message if any") String errorMessage
) {
}
