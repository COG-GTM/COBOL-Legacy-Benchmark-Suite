package com.coggtm.portfolio.domain;

import com.coggtm.portfolio.domain.enums.AccountType;
import com.coggtm.portfolio.domain.enums.PortfolioStatus;
import com.coggtm.portfolio.domain.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PortfolioTest {

    @Test
    void shouldBuildPortfolioWithAllFields() {
        Portfolio portfolio = Portfolio.builder()
                .portfolioId("PORT0001")
                .accountType(AccountType.IN)
                .branchId("01")
                .clientId("CLIENT0001")
                .portfolioName("Test Portfolio")
                .currencyCode("USD")
                .riskLevel(RiskLevel.M)
                .status(PortfolioStatus.A)
                .totalValue(new BigDecimal("100000.00"))
                .cashBalance(new BigDecimal("25000.00"))
                .openDate(LocalDate.of(2024, 1, 15))
                .lastMaintDate(LocalDateTime.now())
                .lastMaintUser("TESTUSER")
                .build();

        assertNotNull(portfolio);
        assertEquals("PORT0001", portfolio.getPortfolioId());
        assertEquals(AccountType.IN, portfolio.getAccountType());
        assertEquals(PortfolioStatus.A, portfolio.getStatus());
        assertEquals(new BigDecimal("100000.00"), portfolio.getTotalValue());
    }

    @Test
    void portfolioIdShouldFollowFormat() {
        Portfolio portfolio = Portfolio.builder()
                .portfolioId("PORT1234")
                .build();
        assertEquals("PORT1234", portfolio.getPortfolioId());
        assertEquals(8, portfolio.getPortfolioId().length());
    }
}
