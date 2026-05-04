package com.coggtm.portfolio.repository;

import com.coggtm.portfolio.domain.Portfolio;
import com.coggtm.portfolio.domain.enums.AccountType;
import com.coggtm.portfolio.domain.enums.PortfolioStatus;
import com.coggtm.portfolio.domain.enums.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Test
    void shouldSaveAndFindPortfolio() {
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

        portfolioRepository.save(portfolio);

        Optional<Portfolio> found = portfolioRepository.findById("PORT0001");
        assertTrue(found.isPresent());
        assertEquals("Test Portfolio", found.get().getPortfolioName());
        assertEquals(PortfolioStatus.A, found.get().getStatus());
    }

    @Test
    void shouldFindByClientId() {
        Portfolio portfolio = Portfolio.builder()
                .portfolioId("PORT0002")
                .accountType(AccountType.CO)
                .branchId("02")
                .clientId("CLIENT0002")
                .portfolioName("Corporate Portfolio")
                .currencyCode("EUR")
                .riskLevel(RiskLevel.H)
                .status(PortfolioStatus.A)
                .totalValue(BigDecimal.ZERO)
                .cashBalance(BigDecimal.ZERO)
                .openDate(LocalDate.now())
                .lastMaintDate(LocalDateTime.now())
                .lastMaintUser("ADMIN")
                .build();

        portfolioRepository.save(portfolio);

        var results = portfolioRepository.findByClientId("CLIENT0002");
        assertEquals(1, results.size());
        assertEquals("PORT0002", results.get(0).getPortfolioId());
    }
}
