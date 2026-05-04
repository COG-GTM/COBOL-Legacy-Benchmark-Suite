package com.portfolio;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Position;
import com.portfolio.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Test Data Generator - migrated from COBOL TSTGEN00.cbl.
 * Generates test portfolios, transactions, and error scenarios.
 */
public class TestDataGenerator {

    public static Portfolio createTestPortfolio(String id) {
        Portfolio p = new Portfolio();
        p.setPortfolioId(id);
        p.setAccountType("IN");
        p.setBranchId("01");
        p.setClientId("CLIENT" + id.substring(0, Math.min(4, id.length())));
        p.setClientName("Test Client " + id);
        p.setClientType("I");
        p.setCurrencyCode("USD");
        p.setRiskLevel("M");
        p.setStatus("A");
        p.setOpenDate(LocalDate.now().minusYears(1));
        p.setTotalValue(new BigDecimal("100000.00"));
        p.setCashBalance(new BigDecimal("25000.00"));
        p.setLastMaintDate(LocalDateTime.now());
        p.setLastMaintUser("TESTUSER");
        return p;
    }

    public static Portfolio createCorporatePortfolio(String id) {
        Portfolio p = createTestPortfolio(id);
        p.setClientType("C");
        p.setAccountType("CO");
        p.setTotalValue(new BigDecimal("5000000.00"));
        p.setCashBalance(new BigDecimal("500000.00"));
        return p;
    }

    public static Portfolio createTrustPortfolio(String id) {
        Portfolio p = createTestPortfolio(id);
        p.setClientType("T");
        p.setAccountType("TR");
        p.setTotalValue(new BigDecimal("2500000.00"));
        p.setCashBalance(new BigDecimal("100000.00"));
        return p;
    }

    public static Position createTestPosition(String portfolioId, String investmentId) {
        Position pos = new Position();
        pos.setPortfolioId(portfolioId);
        pos.setInvestmentId(investmentId);
        pos.setPositionDate(LocalDate.now());
        pos.setQuantity(new BigDecimal("100.0000"));
        pos.setCostBasis(new BigDecimal("5000.00"));
        pos.setMarketValue(new BigDecimal("5500.00"));
        pos.setCurrencyCode("USD");
        pos.setStatus("A");
        pos.setLastMaintDate(LocalDateTime.now());
        pos.setLastMaintUser("TESTUSER");
        return pos;
    }

    public static Transaction createTestTransaction(String portfolioId, String type) {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN" + System.nanoTime());
        txn.setPortfolioId(portfolioId);
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime("120000");
        txn.setInvestmentId("INV0001");
        txn.setTransactionType(type);
        txn.setQuantity(new BigDecimal("50.0000"));
        txn.setPrice(new BigDecimal("100.0000"));
        txn.setAmount(new BigDecimal("5000.00"));
        txn.setCurrencyCode("USD");
        txn.setStatus("D");
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("TESTUSER");
        return txn;
    }
}
