package com.clbs.portfolio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "New portfolio (PORTFLIO record).")
public record PortfolioRequest(
        @NotBlank @Size(max = 8) String portfolioId,
        @NotBlank @Size(max = 10) String accountNo,
        @NotBlank @Size(max = 30) String clientName,
        @Schema(description = "I=Individual, C=Corporate, T=Trust") @Pattern(regexp = "[ICT]") String clientType,
        @Schema(description = "Must be A (active) for a new portfolio", example = "A") @NotBlank @Size(max = 1) String status,
        @Digits(integer = 13, fraction = 2) BigDecimal totalValue,
        @Digits(integer = 13, fraction = 2) BigDecimal cashBalance) {
}
