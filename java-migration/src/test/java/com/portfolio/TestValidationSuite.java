package com.portfolio;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.enums.ClientType;
import com.portfolio.domain.enums.PortfolioStatus;
import com.portfolio.domain.enums.ReturnCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.portfolio.repository.PortfolioRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Validation Suite - migrated from COBOL TSTVAL00.cbl.
 * Integration tests to validate migration correctness.
 */
@SpringBootTest
@ActiveProfiles("test")
class TestValidationSuite {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Test
    void testReturnCodeValues() {
        assertEquals(0, ReturnCode.SUCCESS.getCode());
        assertEquals(4, ReturnCode.WARNING.getCode());
        assertEquals(8, ReturnCode.ERROR.getCode());
        assertEquals(12, ReturnCode.SEVERE.getCode());
        assertEquals(16, ReturnCode.CRITICAL.getCode());
    }

    @Test
    void testReturnCodeFromCode() {
        assertEquals(ReturnCode.SUCCESS, ReturnCode.fromCode(0));
        assertEquals(ReturnCode.ERROR, ReturnCode.fromCode(8));
        assertThrows(IllegalArgumentException.class, () -> ReturnCode.fromCode(99));
    }

    @Test
    void testClientTypeMapping() {
        assertEquals('I', ClientType.INDIVIDUAL.getCode());
        assertEquals('C', ClientType.CORPORATE.getCode());
        assertEquals('T', ClientType.TRUST.getCode());
        assertEquals(ClientType.INDIVIDUAL, ClientType.fromCode('I'));
    }

    @Test
    void testPortfolioStatusMapping() {
        assertEquals('A', PortfolioStatus.ACTIVE.getCode());
        assertEquals('C', PortfolioStatus.CLOSED.getCode());
        assertEquals('S', PortfolioStatus.SUSPENDED.getCode());
        assertEquals(PortfolioStatus.ACTIVE, PortfolioStatus.fromCode('A'));
    }

    @Test
    void testBigDecimalPrecision() {
        BigDecimal comp3Value = new BigDecimal("9999999999999.99");
        assertEquals(15, comp3Value.precision());
        assertEquals(2, comp3Value.scale());

        BigDecimal result = comp3Value.add(new BigDecimal("0.01"));
        assertEquals(new BigDecimal("10000000000000.00"), result);
    }

    @Test
    void testPortfolioCreateAndRead() {
        Portfolio p = TestDataGenerator.createTestPortfolio("TEST0001");
        Portfolio saved = portfolioRepository.save(p);
        assertNotNull(saved);
        assertEquals("TEST0001", saved.getPortfolioId());

        Portfolio found = portfolioRepository.findById("TEST0001").orElse(null);
        assertNotNull(found);
        assertEquals("Test Client TEST0001", found.getClientName());
        assertEquals("I", found.getClientType());
        assertEquals("A", found.getStatus());
        assertEquals(0, new BigDecimal("100000.00").compareTo(found.getTotalValue()));
    }

    @Test
    void testPortfolioStatusMethods() {
        Portfolio p = new Portfolio();
        p.setStatus("A");
        assertTrue(p.isActive());
        assertFalse(p.isClosed());
        assertFalse(p.isSuspended());

        p.setStatus("C");
        assertFalse(p.isActive());
        assertTrue(p.isClosed());
    }

    @Test
    void testFixedLengthFieldValidation() {
        String portfolioId = "TEST0001";
        assertTrue(portfolioId.length() <= 8);

        String longId = "TOOLONGID";
        assertTrue(longId.length() > 8);
    }

    @Test
    void testDateConversion() {
        String cobolDate = "20250101";
        int year = Integer.parseInt(cobolDate.substring(0, 4));
        int month = Integer.parseInt(cobolDate.substring(4, 6));
        int day = Integer.parseInt(cobolDate.substring(6, 8));

        java.time.LocalDate date = java.time.LocalDate.of(year, month, day);
        assertEquals(2025, date.getYear());
        assertEquals(1, date.getMonthValue());
        assertEquals(1, date.getDayOfMonth());
    }
}
