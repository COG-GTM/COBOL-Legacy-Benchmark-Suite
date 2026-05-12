package com.portfolio.service;

import com.portfolio.exception.InvalidPortfolioException;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.service.portfolio.PortfolioValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioValidatorTest {

    private PortfolioValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortfolioValidator();
    }

    @Test
    void validate_validPortfolio() {
        Portfolio p = createValidPortfolio();
        assertDoesNotThrow(() -> validator.validate(p));
    }

    @Test
    void validate_invalidPrefix() {
        Portfolio p = createValidPortfolio();
        p.setPortfolioId("ABCD0001");
        assertThrows(InvalidPortfolioException.class, () -> validator.validate(p));
    }

    @Test
    void validate_nullId() {
        Portfolio p = createValidPortfolio();
        p.setPortfolioId(null);
        assertThrows(InvalidPortfolioException.class, () -> validator.validate(p));
    }

    @Test
    void validate_shortId() {
        Portfolio p = createValidPortfolio();
        p.setPortfolioId("POR");
        assertThrows(InvalidPortfolioException.class, () -> validator.validate(p));
    }

    @Test
    void validate_blankName() {
        Portfolio p = createValidPortfolio();
        p.setPortfolioName("   ");
        assertThrows(InvalidPortfolioException.class, () -> validator.validate(p));
    }

    @Test
    void validate_invalidStatus() {
        Portfolio p = createValidPortfolio();
        p.setStatus('X');
        assertThrows(InvalidPortfolioException.class, () -> validator.validate(p));
    }

    @Test
    void validate_validStatuses() {
        Portfolio p = createValidPortfolio();

        p.setStatus('A');
        assertDoesNotThrow(() -> validator.validate(p));

        p.setStatus('C');
        assertDoesNotThrow(() -> validator.validate(p));

        p.setStatus('S');
        assertDoesNotThrow(() -> validator.validate(p));
    }

    private Portfolio createValidPortfolio() {
        Portfolio p = new Portfolio();
        p.setPortfolioId("PORT0001");
        p.setAccountType("GN");
        p.setBranchId("01");
        p.setClientId("CLIENT001");
        p.setPortfolioName("Test Portfolio");
        p.setCurrencyCode("USD");
        p.setRiskLevel("M");
        p.setStatus('A');
        p.setOpenDate(LocalDate.now());
        p.setLastMaintDate(LocalDateTime.now());
        p.setLastMaintUser("TESTUSER");
        p.setTotalValue(new BigDecimal("50000.00"));
        return p;
    }
}
