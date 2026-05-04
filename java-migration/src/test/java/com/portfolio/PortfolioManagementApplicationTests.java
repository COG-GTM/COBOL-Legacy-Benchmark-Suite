package com.portfolio;

import com.portfolio.entity.*;
import com.portfolio.repository.*;
import com.portfolio.service.*;
import com.portfolio.util.PortfolioValidation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PortfolioManagementApplicationTests {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PortfolioMasterService portfolioService;

    @Autowired
    private DatabaseStatisticsService statsService;

    @Test
    void contextLoads() {
    }

    @Test
    void testDataLoaded() {
        assertTrue(portfolioRepository.count() > 0, "Portfolios should be seeded");
        assertTrue(positionRepository.count() > 0, "Positions should be seeded");
        assertTrue(transactionRepository.count() > 0, "Transactions should be seeded");
    }

    @Test
    void testPortfolioLookup() {
        var portfolio = portfolioRepository.findById("PORT0001");
        assertTrue(portfolio.isPresent(), "PORT0001 should exist");
        assertEquals("A", portfolio.get().getStatus());
    }

    @Test
    void testActivePortfolios() {
        List<Portfolio> active = portfolioRepository.findActivePortfolios();
        assertFalse(active.isEmpty());
        active.forEach(p -> assertEquals("A", p.getStatus()));
    }

    @Test
    void testPositionsByPortfolio() {
        List<PositionRecord> positions = positionRepository.findActivePositions("PORT0001");
        assertFalse(positions.isEmpty(), "PORT0001 should have positions");
    }

    @Test
    void testTransactionHistory() {
        List<TransactionRecord> history = transactionRepository
                .findHistoryByPortfolioId("PORT0001");
        assertFalse(history.isEmpty(), "PORT0001 should have transaction history");
    }

    @Test
    void testPortfolioValidation() {
        Portfolio valid = new Portfolio();
        valid.setPortfolioId("TEST0001");
        valid.setClientId("CLT0000001");
        valid.setPortfolioName("Test Portfolio");
        valid.setStatus("A");
        List<String> errors = PortfolioValidation.validatePortfolio(valid);
        assertTrue(errors.isEmpty(), "Valid portfolio should pass validation");
    }

    @Test
    void testPortfolioValidationFails() {
        Portfolio invalid = new Portfolio();
        List<String> errors = PortfolioValidation.validatePortfolio(invalid);
        assertFalse(errors.isEmpty(), "Invalid portfolio should fail validation");
    }

    @Test
    void testTransactionValidation() {
        TransactionRecord valid = new TransactionRecord();
        valid.setPortfolioId("PORT0001");
        valid.setTransactionType("BU");
        valid.setQuantity(BigDecimal.TEN);
        valid.setPrice(BigDecimal.valueOf(100));
        valid.setAmount(BigDecimal.valueOf(1000));
        List<String> errors = PortfolioValidation.validateTransaction(valid);
        assertTrue(errors.isEmpty(), "Valid transaction should pass validation");
    }

    @Test
    void testDatabaseStatistics() {
        var counts = statsService.getTableCounts();
        assertFalse(counts.isEmpty());
        assertTrue(counts.get("Portfolios") > 0);
    }
}
