package com.portfolio.service;

import com.portfolio.exception.InsufficientUnitsException;
import com.portfolio.exception.TransactionProcessingException;
import com.portfolio.model.dto.CommonConstants;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Transaction;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.common.AuditService;
import com.portfolio.service.transaction.TransactionProcessingService;
import com.portfolio.service.transaction.TransactionProcessingService.TransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProcessingServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AuditService auditService;

    private TransactionProcessingService service;

    @BeforeEach
    void setUp() {
        service = new TransactionProcessingService(transactionRepository, portfolioRepository, auditService);
    }

    @Test
    void processTransactions_buySuccess() {
        Portfolio portfolio = createTestPortfolio("PORT0001", new BigDecimal("100000.00"));
        Transaction txn = createTestTransaction("PORT0001", CommonConstants.TRN_TYPE_BUY, new BigDecimal("5000.00"));

        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any())).thenReturn(portfolio);

        TransactionResult result = service.processTransactions(List.of(txn));

        assertEquals(1, result.readCount());
        assertEquals(1, result.processCount());
        assertEquals(0, result.errorCount());
        verify(transactionRepository).save(any());
    }

    @Test
    void processTransactions_sellSuccess() {
        Portfolio portfolio = createTestPortfolio("PORT0001", new BigDecimal("100000.00"));
        Transaction txn = createTestTransaction("PORT0001", CommonConstants.TRN_TYPE_SELL, new BigDecimal("5000.00"));

        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any())).thenReturn(portfolio);

        TransactionResult result = service.processTransactions(List.of(txn));

        assertEquals(1, result.processCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void processTransactions_sellInsufficientUnits() {
        Portfolio portfolio = createTestPortfolio("PORT0001", new BigDecimal("1000.00"));
        Transaction txn = createTestTransaction("PORT0001", CommonConstants.TRN_TYPE_SELL, new BigDecimal("5000.00"));

        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        TransactionResult result = service.processTransactions(List.of(txn));

        assertEquals(1, result.readCount());
        assertEquals(0, result.processCount());
        assertEquals(1, result.errorCount());
    }

    @Test
    void processTransactions_invalidType() {
        Transaction txn = createTestTransaction("PORT0001", "XX", new BigDecimal("5000.00"));

        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);

        TransactionResult result = service.processTransactions(List.of(txn));

        assertEquals(0, result.processCount());
        assertEquals(1, result.errorCount());
    }

    @Test
    void processTransactions_feeSuccess() {
        Portfolio portfolio = createTestPortfolio("PORT0001", new BigDecimal("100000.00"));
        Transaction txn = createTestTransaction("PORT0001", CommonConstants.TRN_TYPE_FEE, new BigDecimal("50.00"));

        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any())).thenReturn(portfolio);

        TransactionResult result = service.processTransactions(List.of(txn));

        assertEquals(1, result.processCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void processTransactions_transferUnsupported() {
        Transaction txn = createTestTransaction("PORT0001", CommonConstants.TRN_TYPE_TRANSFER, new BigDecimal("5000.00"));

        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);

        TransactionResult result = service.processTransactions(List.of(txn));

        assertEquals(0, result.processCount());
        assertEquals(1, result.errorCount());
    }

    private Portfolio createTestPortfolio(String id, BigDecimal totalValue) {
        Portfolio p = new Portfolio();
        p.setPortfolioId(id);
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
        p.setTotalValue(totalValue);
        return p;
    }

    private Transaction createTestTransaction(String portfolioId, String type, BigDecimal amount) {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN" + System.nanoTime());
        txn.setPortfolioId(portfolioId);
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime(LocalTime.now());
        txn.setInvestmentId("INV001");
        txn.setTransactionType(type);
        txn.setQuantity(new BigDecimal("100.0000"));
        txn.setPrice(new BigDecimal("50.0000"));
        txn.setAmount(amount);
        txn.setCurrencyCode("USD");
        txn.setStatus('P');
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("TESTUSER");
        return txn;
    }
}
