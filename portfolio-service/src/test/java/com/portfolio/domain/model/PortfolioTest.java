package com.portfolio.domain.model;

import com.portfolio.domain.exception.InsufficientUnitsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioTest {

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio("PORT0001", "1234567890", "Test Client", ClientType.INDIVIDUAL);
    }

    @Test
    void newPortfolioHasZeroUnitsAndCost() {
        assertEquals(BigDecimal.ZERO, portfolio.getTotalUnits());
        assertEquals(BigDecimal.ZERO, portfolio.getTotalCost());
    }

    @Test
    void applyBuyAddsUnitsAndCost() {
        portfolio.applyBuy(new BigDecimal("100"), new BigDecimal("5000.00"));

        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("5000.00"), portfolio.getTotalCost());
    }

    @Test
    void applyBuyAccumulates() {
        portfolio.applyBuy(new BigDecimal("100"), new BigDecimal("5000.00"));
        portfolio.applyBuy(new BigDecimal("50"), new BigDecimal("2500.00"));

        assertEquals(new BigDecimal("150"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("7500.00"), portfolio.getTotalCost());
    }

    @Test
    void applySellSubtractsUnitsAndCost() {
        portfolio.applyBuy(new BigDecimal("100"), new BigDecimal("5000.00"));
        portfolio.applySell(new BigDecimal("30"), new BigDecimal("1500.00"));

        assertEquals(new BigDecimal("70"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("3500.00"), portfolio.getTotalCost());
    }

    @Test
    void applySellAllUnits() {
        portfolio.applyBuy(new BigDecimal("100"), new BigDecimal("5000.00"));
        portfolio.applySell(new BigDecimal("100"), new BigDecimal("5000.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(portfolio.getTotalUnits()));
        assertEquals(0, BigDecimal.ZERO.compareTo(portfolio.getTotalCost()));
    }

    @Test
    void applySellThrowsWhenInsufficientUnits() {
        portfolio.applyBuy(new BigDecimal("50"), new BigDecimal("2500.00"));

        InsufficientUnitsException ex = assertThrows(InsufficientUnitsException.class,
                () -> portfolio.applySell(new BigDecimal("100"), new BigDecimal("5000.00")));

        assertEquals(new BigDecimal("100"), ex.getRequested());
        assertEquals(new BigDecimal("50"), ex.getAvailable());
    }

    @Test
    void applySellThrowsOnZeroUnits() {
        assertThrows(InsufficientUnitsException.class,
                () -> portfolio.applySell(new BigDecimal("1"), new BigDecimal("50.00")));
    }

    @Test
    void applyFeeReducesCostOnly() {
        portfolio.applyBuy(new BigDecimal("100"), new BigDecimal("5000.00"));
        portfolio.applyFee(new BigDecimal("25.00"));

        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("4975.00"), portfolio.getTotalCost());
    }

    @Test
    void applyFeeOnZeroBalance() {
        portfolio.applyFee(new BigDecimal("10.00"));

        assertEquals(BigDecimal.ZERO, portfolio.getTotalUnits());
        assertEquals(new BigDecimal("-10.00"), portfolio.getTotalCost());
    }

    @Test
    void applyTransferThrowsUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class, () -> portfolio.applyTransfer());
    }

    @Test
    void statusDefaultsToActive() {
        assertEquals(PortfolioStatus.ACTIVE, portfolio.getStatus());
    }

    @Test
    void markMaintenanceUpdatesAuditFields() {
        portfolio.markMaintenance("TESTUSER");

        assertEquals("TESTUSER", portfolio.getLastUser());
        assertNotNull(portfolio.getLastMaintenance());
        assertNotNull(portfolio.getLastTransDate());
    }
}
