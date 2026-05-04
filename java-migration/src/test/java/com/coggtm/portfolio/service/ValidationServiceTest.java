package com.coggtm.portfolio.service;

import com.coggtm.portfolio.service.impl.ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationServiceImpl();
    }

    @Test
    void shouldAcceptValidPortfolioId() {
        assertTrue(validationService.validatePortfolioId("PORT0001"));
        assertTrue(validationService.validatePortfolioId("PORT9999"));
    }

    @Test
    void shouldRejectInvalidPortfolioId() {
        assertFalse(validationService.validatePortfolioId(null));
        assertFalse(validationService.validatePortfolioId(""));
        assertFalse(validationService.validatePortfolioId("INVALID1"));
        assertFalse(validationService.validatePortfolioId("PORT"));
    }

    @Test
    void shouldAcceptValidAccount() {
        assertTrue(validationService.validateAccount("ACCT000001"));
        assertTrue(validationService.validateAccount("A1"));
    }

    @Test
    void shouldRejectInvalidAccount() {
        assertFalse(validationService.validateAccount(null));
        assertFalse(validationService.validateAccount(""));
        assertFalse(validationService.validateAccount("   "));
    }

    @Test
    void shouldAcceptValidInvestmentTypes() {
        assertTrue(validationService.validateInvestmentType("STK"));
        assertTrue(validationService.validateInvestmentType("BND"));
        assertTrue(validationService.validateInvestmentType("MMF"));
        assertTrue(validationService.validateInvestmentType("ETF"));
    }

    @Test
    void shouldRejectInvalidInvestmentType() {
        assertFalse(validationService.validateInvestmentType(null));
        assertFalse(validationService.validateInvestmentType("XXX"));
    }

    @Test
    void shouldAcceptValidAmount() {
        assertTrue(validationService.validateAmount(BigDecimal.ZERO));
        assertTrue(validationService.validateAmount(new BigDecimal("1000.00")));
        assertTrue(validationService.validateAmount(new BigDecimal("-500.00")));
    }

    @Test
    void shouldRejectNullAmount() {
        assertFalse(validationService.validateAmount(null));
    }

    @Test
    void shouldRejectAmountOutOfRange() {
        assertFalse(validationService.validateAmount(new BigDecimal("99999999999999.00")));
        assertFalse(validationService.validateAmount(new BigDecimal("-99999999999999.00")));
    }
}
