package com.cog.portfolio.dto;

import com.cog.portfolio.domain.ClientType;
import com.cog.portfolio.domain.PortfolioStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** PORTMSTR LS-PORTFOLIO command area (create / update). */
public record PortfolioRequest(
        @NotBlank @Size(max = 8) String portfolioId,
        @NotBlank @Size(max = 10) String accountNo,
        @NotBlank @Size(max = 30) String clientName,
        @NotNull ClientType clientType,
        @NotNull PortfolioStatus status,
        BigDecimal totalValue,
        BigDecimal cashBalance) {
}
