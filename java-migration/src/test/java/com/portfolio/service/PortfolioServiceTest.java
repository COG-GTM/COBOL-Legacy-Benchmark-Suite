package com.portfolio.service;

import com.portfolio.TestDataGenerator;
import com.portfolio.domain.Portfolio;
import com.portfolio.exception.ProcessingException;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.service.common.AuditService;
import com.portfolio.service.portfolio.PortfolioService;
import com.portfolio.service.portfolio.PortfolioValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Portfolio Service Tests - migrated from COBOL PORTTEST.cbl.
 * Verifies CRUD operations match COBOL behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class PortfolioServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();
    }

    @Test
    void testCreatePortfolio() {
        Portfolio p = TestDataGenerator.createTestPortfolio("CREA0001");
        Portfolio created = portfolioService.createPortfolio(p);

        assertNotNull(created);
        assertEquals("CREA0001", created.getPortfolioId());
        assertEquals("A", created.getStatus());
        assertNotNull(created.getOpenDate());
    }

    @Test
    void testCreateDuplicatePortfolio() {
        Portfolio p1 = TestDataGenerator.createTestPortfolio("DUP00001");
        portfolioService.createPortfolio(p1);

        Portfolio p2 = TestDataGenerator.createTestPortfolio("DUP00001");
        assertThrows(ValidationException.class, () -> portfolioService.createPortfolio(p2));
    }

    @Test
    void testGetPortfolio() {
        Portfolio p = TestDataGenerator.createTestPortfolio("READ0001");
        portfolioService.createPortfolio(p);

        Optional<Portfolio> found = portfolioService.getPortfolio("READ0001");
        assertTrue(found.isPresent());
        assertEquals("READ0001", found.get().getPortfolioId());
    }

    @Test
    void testGetPortfolioNotFound() {
        Optional<Portfolio> found = portfolioService.getPortfolio("NOTFOUND");
        assertFalse(found.isPresent());
    }

    @Test
    void testUpdatePortfolio() {
        Portfolio p = TestDataGenerator.createTestPortfolio("UPDT0001");
        portfolioService.createPortfolio(p);

        p.setClientName("Updated Client Name");
        p.setTotalValue(new BigDecimal("200000.00"));
        Portfolio updated = portfolioService.updatePortfolio(p);

        assertEquals("Updated Client Name", updated.getClientName());
        assertEquals(0, new BigDecimal("200000.00").compareTo(updated.getTotalValue()));
    }

    @Test
    void testUpdateNonExistentPortfolio() {
        Portfolio p = TestDataGenerator.createTestPortfolio("NOEXIST1");
        assertThrows(ProcessingException.class, () -> portfolioService.updatePortfolio(p));
    }

    @Test
    void testDeletePortfolio() {
        Portfolio p = TestDataGenerator.createTestPortfolio("DEL00001");
        portfolioService.createPortfolio(p);

        portfolioService.deletePortfolio("DEL00001", "TESTUSER");

        Optional<Portfolio> found = portfolioService.getPortfolio("DEL00001");
        assertTrue(found.isPresent());
        assertEquals("C", found.get().getStatus());
        assertNotNull(found.get().getCloseDate());
    }

    @Test
    void testGetActivePortfolios() {
        portfolioService.createPortfolio(TestDataGenerator.createTestPortfolio("ACT00001"));
        portfolioService.createPortfolio(TestDataGenerator.createTestPortfolio("ACT00002"));

        List<Portfolio> active = portfolioService.getActivePortfolios();
        assertFalse(active.isEmpty());
        assertTrue(active.stream().allMatch(Portfolio::isActive));
    }

    @Test
    void testValidationRejectsInvalidClientType() {
        Portfolio p = TestDataGenerator.createTestPortfolio("VALD0001");
        p.setClientType("X");
        assertThrows(ValidationException.class, () -> portfolioService.createPortfolio(p));
    }

    @Test
    void testBigDecimalPreservation() {
        Portfolio p = TestDataGenerator.createTestPortfolio("PREC0001");
        p.setTotalValue(new BigDecimal("9999999999999.99"));
        p.setCashBalance(new BigDecimal("1234567890123.45"));
        portfolioService.createPortfolio(p);

        Optional<Portfolio> found = portfolioService.getPortfolio("PREC0001");
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("9999999999999.99").compareTo(found.get().getTotalValue()));
        assertEquals(0, new BigDecimal("1234567890123.45").compareTo(found.get().getCashBalance()));
    }
}
