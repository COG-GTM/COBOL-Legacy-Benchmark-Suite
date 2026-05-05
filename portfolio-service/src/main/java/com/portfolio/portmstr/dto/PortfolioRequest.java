package com.portfolio.portmstr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for Portfolio CRUD operations.
 * Maps to COBOL LINKAGE SECTION LS-COMMAND-AREA fields.
 */
@Schema(description = "Portfolio creation/update request")
public record PortfolioRequest(
        @Schema(description = "Portfolio ID (format: PORT + 4 digits)", example = "PORT0001")
        @NotBlank
        @Size(max = 8)
        @Pattern(regexp = "^PORT\\d{4}$", message = "Portfolio ID must match format PORT + 4 digits")
        String portfolioId,

        @Schema(description = "Account number (10 digits)", example = "1000000001")
        @Size(max = 10)
        String accountNo,

        @Schema(description = "Client name", example = "John Doe")
        @NotBlank
        @Size(max = 30)
        String clientName,

        @Schema(description = "Client type: I=Individual, C=Corporate, T=Trust", example = "I")
        @NotNull
        @Pattern(regexp = "^[ICT]$", message = "Client type must be I, C, or T")
        String clientType,

        @Schema(description = "Portfolio status: A=Active, C=Closed, S=Suspended", example = "A")
        @NotNull
        @Pattern(regexp = "^[ACS]$", message = "Status must be A, C, or S")
        String status,

        @Schema(description = "Total portfolio value", example = "1000000.00")
        BigDecimal totalValue,

        @Schema(description = "Cash balance", example = "100000.00")
        BigDecimal cashBalance,

        @Schema(description = "Currency code", example = "USD")
        @Size(max = 3)
        String currencyCode
) {
}
