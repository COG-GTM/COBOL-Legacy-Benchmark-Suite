package com.portfolio.domain.model;

import com.portfolio.domain.enums.ClientType;
import com.portfolio.domain.enums.PortfolioStatus;
import com.portfolio.domain.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PortfolioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = new Portfolio();
        testPortfolio.setPortfolioId("PORT0001");
        testPortfolio.setAccountType("IN");
        testPortfolio.setBranchId("01");
        testPortfolio.setClientId("CLIENT0001");
        testPortfolio.setPortfolioName("Test Portfolio");
        testPortfolio.setCurrencyCode("USD");
        testPortfolio.setRiskLevel("M");
        testPortfolio.setStatus(PortfolioStatus.ACTIVE);
        testPortfolio.setOpenDate(LocalDate.of(2024, 1, 15));
        testPortfolio.setLastMaintDate(LocalDateTime.of(2024, 3, 20, 10, 30, 0));
        testPortfolio.setLastMaintUser("ADMIN001");
        testPortfolio.setClientName("John Doe");
        testPortfolio.setClientType(ClientType.INDIVIDUAL);
        testPortfolio.setTotalValue(new BigDecimal("1000000.50"));
        testPortfolio.setCashBalance(new BigDecimal("50000.25"));
    }

    @Test
    void shouldPersistAndReadPortfolio() {
        entityManager.persistAndFlush(testPortfolio);
        entityManager.clear();

        Optional<Portfolio> found = portfolioRepository.findById("PORT0001");

        assertThat(found).isPresent();
        Portfolio p = found.get();
        assertThat(p.getPortfolioId()).isEqualTo("PORT0001");
        assertThat(p.getClientId()).isEqualTo("CLIENT0001");
        assertThat(p.getPortfolioName()).isEqualTo("Test Portfolio");
        assertThat(p.getCurrencyCode()).isEqualTo("USD");
        assertThat(p.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);
        assertThat(p.getOpenDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(p.getClientName()).isEqualTo("John Doe");
        assertThat(p.getClientType()).isEqualTo(ClientType.INDIVIDUAL);
    }

    @Test
    void shouldStoreEnumAsCobolCode() {
        entityManager.persistAndFlush(testPortfolio);
        entityManager.clear();

        Object statusValue = entityManager.getEntityManager()
                .createNativeQuery("SELECT status FROM portfolio_master WHERE portfolio_id = 'PORT0001'")
                .getSingleResult();
        assertThat(statusValue.toString()).isEqualTo("A");

        Object clientTypeValue = entityManager.getEntityManager()
                .createNativeQuery("SELECT client_type FROM portfolio_master WHERE portfolio_id = 'PORT0001'")
                .getSingleResult();
        assertThat(clientTypeValue.toString()).isEqualTo("I");
    }

    @Test
    void shouldPreserveBigDecimalPrecision() {
        entityManager.persistAndFlush(testPortfolio);
        entityManager.clear();

        Portfolio found = portfolioRepository.findById("PORT0001").orElseThrow();
        assertThat(found.getTotalValue()).isEqualByComparingTo(new BigDecimal("1000000.50"));
        assertThat(found.getCashBalance()).isEqualByComparingTo(new BigDecimal("50000.25"));
    }

    @Test
    void shouldFindByClientIdAndStatus() {
        entityManager.persistAndFlush(testPortfolio);

        Portfolio closedPortfolio = new Portfolio();
        closedPortfolio.setPortfolioId("PORT0002");
        closedPortfolio.setAccountType("CO");
        closedPortfolio.setBranchId("02");
        closedPortfolio.setClientId("CLIENT0001");
        closedPortfolio.setPortfolioName("Closed Portfolio");
        closedPortfolio.setCurrencyCode("EUR");
        closedPortfolio.setRiskLevel("H");
        closedPortfolio.setStatus(PortfolioStatus.CLOSED);
        closedPortfolio.setOpenDate(LocalDate.of(2023, 6, 1));
        closedPortfolio.setCloseDate(LocalDate.of(2024, 1, 1));
        closedPortfolio.setLastMaintDate(LocalDateTime.now());
        closedPortfolio.setLastMaintUser("ADMIN002");
        entityManager.persistAndFlush(closedPortfolio);

        entityManager.clear();

        List<Portfolio> activePortfolios = portfolioRepository
                .findByClientIdAndStatus("CLIENT0001", PortfolioStatus.ACTIVE);
        assertThat(activePortfolios).hasSize(1);
        assertThat(activePortfolios.get(0).getPortfolioId()).isEqualTo("PORT0001");

        List<Portfolio> closedPortfolios = portfolioRepository
                .findByClientIdAndStatus("CLIENT0001", PortfolioStatus.CLOSED);
        assertThat(closedPortfolios).hasSize(1);
        assertThat(closedPortfolios.get(0).getPortfolioId()).isEqualTo("PORT0002");
    }
}
