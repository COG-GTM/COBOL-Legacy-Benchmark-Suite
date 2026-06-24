package com.portfolio.service;

import com.portfolio.dto.PortfolioResponse;
import com.portfolio.dto.TransactionRequest;
import com.portfolio.dto.TransactionResponse;
import com.portfolio.exception.InsufficientUnitsException;
import com.portfolio.exception.PortfolioNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for TransactionService.
 * Tests mirror PORTTRAN.cbl transaction processing:
 * - processBuy -> 2210-PROCESS-BUY
 * - processSell -> 2220-PROCESS-SELL
 * - processTransfer -> 2230-PROCESS-TRANSFER
 * - processFee -> 2240-PROCESS-FEE
 */
@SpringBootTest
@Transactional
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PortfolioService portfolioService;

    @Test
    void testProcessBuyTransaction() {
        PortfolioResponse before = portfolioService.readPortfolio("PORT0001");
        BigDecimal originalValue = before.getTotalValue();
        BigDecimal originalCash = before.getCashBalance();

        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT0001");
        request.setInvestmentId("TSLA");
        request.setTransactionType("BU");
        request.setQuantity(new BigDecimal("100.0000"));
        request.setPrice(new BigDecimal("250.0000"));
        request.setAmount(new BigDecimal("25000.00"));
        request.setCurrency("USD");

        TransactionResponse response = transactionService.processTransaction(request);
        assertNotNull(response.getId());
        assertEquals("BU", response.getTransactionType());
        assertEquals("D", response.getStatus());

        PortfolioResponse after = portfolioService.readPortfolio("PORT0001");
        assertEquals(0, originalValue.add(new BigDecimal("25000.00")).compareTo(after.getTotalValue()));
        assertEquals(0, originalCash.subtract(new BigDecimal("25000.00")).compareTo(after.getCashBalance()));
    }

    @Test
    void testProcessSellTransaction() {
        PortfolioResponse before = portfolioService.readPortfolio("PORT0001");
        BigDecimal originalValue = before.getTotalValue();
        BigDecimal originalCash = before.getCashBalance();

        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT0001");
        request.setInvestmentId("AAPL");
        request.setTransactionType("SL");
        request.setQuantity(new BigDecimal("100.0000"));
        request.setPrice(new BigDecimal("190.0000"));
        request.setAmount(new BigDecimal("19000.00"));
        request.setCurrency("USD");

        TransactionResponse response = transactionService.processTransaction(request);
        assertNotNull(response.getId());
        assertEquals("SL", response.getTransactionType());

        PortfolioResponse after = portfolioService.readPortfolio("PORT0001");
        assertEquals(0, originalValue.subtract(new BigDecimal("19000.00")).compareTo(after.getTotalValue()));
        assertEquals(0, originalCash.add(new BigDecimal("19000.00")).compareTo(after.getCashBalance()));
    }

    @Test
    void testProcessSellInsufficientUnits() {
        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT0006");
        request.setInvestmentId("AAPL");
        request.setTransactionType("SL");
        request.setQuantity(new BigDecimal("99999.0000"));
        request.setPrice(new BigDecimal("190.0000"));
        request.setAmount(new BigDecimal("99999999.00"));
        request.setCurrency("USD");

        assertThrows(InsufficientUnitsException.class, () ->
                transactionService.processTransaction(request));
    }

    @Test
    void testProcessFeeTransaction() {
        PortfolioResponse before = portfolioService.readPortfolio("PORT0001");
        BigDecimal originalCash = before.getCashBalance();

        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT0001");
        request.setInvestmentId("FEE");
        request.setTransactionType("FE");
        request.setQuantity(new BigDecimal("1.0000"));
        request.setPrice(new BigDecimal("50.0000"));
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");

        TransactionResponse response = transactionService.processTransaction(request);
        assertNotNull(response);
        assertEquals("FE", response.getTransactionType());

        PortfolioResponse after = portfolioService.readPortfolio("PORT0001");
        assertEquals(0, originalCash.subtract(new BigDecimal("50.00")).compareTo(after.getCashBalance()));
    }

    @Test
    void testProcessTransferTransaction() {
        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT0001");
        request.setInvestmentId("MSFT");
        request.setTransactionType("TR");
        request.setQuantity(new BigDecimal("50.0000"));
        request.setPrice(BigDecimal.ZERO);
        request.setAmount(BigDecimal.ZERO);
        request.setCurrency("USD");

        TransactionResponse response = transactionService.processTransaction(request);
        assertNotNull(response);
        assertEquals("TR", response.getTransactionType());
    }

    @Test
    void testProcessTransactionInvalidPortfolio() {
        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT9999");
        request.setInvestmentId("AAPL");
        request.setTransactionType("BU");
        request.setQuantity(new BigDecimal("10.0000"));
        request.setPrice(new BigDecimal("100.0000"));
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("USD");

        assertThrows(PortfolioNotFoundException.class, () ->
                transactionService.processTransaction(request));
    }

    @Test
    void testFindTransactionsByPortfolio() {
        List<TransactionResponse> transactions = transactionService.findByPortfolioId("PORT0001");
        assertEquals(3, transactions.size(), "PORT0001 should have 3 seed transactions");
    }

    @Test
    void testSeedTransactionData() {
        TransactionResponse txn = transactionService.findById(1L);
        assertNotNull(txn);
        assertEquals("PORT0001", txn.getPortfolioId());
        assertEquals("BU", txn.getTransactionType());
        assertEquals(0, new BigDecimal("87500.00").compareTo(txn.getAmount()));
    }
}
