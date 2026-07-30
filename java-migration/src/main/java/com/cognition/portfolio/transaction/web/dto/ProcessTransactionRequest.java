package com.cognition.portfolio.transaction.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Processing payload for the {@code PORTTRAN 2200-UPDATE-POSITIONS} equivalent.
 *
 * @param availableUnits current {@code PORT-TOTAL-UNITS} of the portfolio, needed for the
 *     'Insufficient units for sale' check of {@code 2220-PROCESS-SELL} (BR-10)
 */
@Schema(name = "ProcessTransactionRequest", description = "Inputs for PORTTRAN 2200-UPDATE-POSITIONS")
public record ProcessTransactionRequest(
    @Schema(description = "Current PORT-TOTAL-UNITS, required for SL transactions", example = "500.0000")
        BigDecimal availableUnits) {}
