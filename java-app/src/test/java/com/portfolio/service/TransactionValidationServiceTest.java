package com.portfolio.service;

import com.portfolio.model.Portfolio;
import com.portfolio.model.TransactionHistory;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TransactionValidationService.
 * Verifies validation logic migrated from TRNVAL00.cbl.
 */
@ExtendWith(MockitoExtension.class)
class TransactionValidationServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TransactionHistoryRepository transactionRepository;

    @InjectMocks
    private TransactionValidationService validationService;

    private TransactionHistory validTransaction;
    private Portfolio activePortfolio;

    @BeforeEach
    void setUp() {
        activePortfolio = new Portfolio();
        activePortfolio.setPortfolioId("PORT0001");
        activePortfolio.setStatus("A");

        validTransaction = new TransactionHistory();
        validTransaction.setTransactionId("TXN00000001");
        validTransaction.setPortfolioId("PORT0001");
        validTransaction.setInvestmentId("INV0000001");
        validTransaction.setTransactionType("BU");
        validTransaction.setQuantity(new BigDecimal("100.0000"));
        validTransaction.setPrice(new BigDecimal("25.50"));
        validTransaction.setAmount(new BigDecimal("2550.00"));
        validTransaction.setTransactionDate(LocalDate.of(2024, 1, 15));
        validTransaction.setTransactionTime(LocalTime.of(10, 0, 0));
        validTransaction.setCurrencyCode("USD");
        validTransaction.setProcessDate(LocalDateTime.now());
        validTransaction.setProcessUser("TEST");
        validTransaction.setStatus("N");
    }

    @Test
    void validate_validBuyTransaction_returnsNoErrors() {
        when(portfolioRepository.findById("PORT0001"))
                .thenReturn(Optional.of(activePortfolio));
        when(transactionRepository.findById("TXN00000001"))
                .thenReturn(Optional.empty());

        List<String> errors = validationService.validate(validTransaction);

        assertTrue(errors.isEmpty(), "Valid transaction should have no errors");
    }

    @Test
    void validate_nullPortfolioId_returnsError() {
        validTransaction.setPortfolioId(null);

        List<String> errors = validationService.validate(validTransaction);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Portfolio ID")));
    }

    @Test
    void validate_portfolioNotFound_returnsError() {
        when(portfolioRepository.findById("PORT0001"))
                .thenReturn(Optional.empty());

        List<String> errors = validationService.validate(validTransaction);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("not found")));
    }

    @Test
    void validate_closedPortfolio_returnsError() {
        activePortfolio.setStatus("C");
        when(portfolioRepository.findById("PORT0001"))
                .thenReturn(Optional.of(activePortfolio));

        List<String> errors = validationService.validate(validTransaction);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("not active")));
    }

    @Test
    void validate_invalidTransactionType_returnsError() {
        validTransaction.setTransactionType("XX");
        when(portfolioRepository.findById("PORT0001"))
                .thenReturn(Optional.of(activePortfolio));

        List<String> errors = validationService.validate(validTransaction);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("transaction type")));
    }

    @Test
    void validate_negativeQuantity_returnsError() {
        validTransaction.setQuantity(new BigDecimal("-10.0000"));
        when(portfolioRepository.findById("PORT0001"))
                .thenReturn(Optional.of(activePortfolio));

        List<String> errors = validationService.validate(validTransaction);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("positive")));
    }

    @Test
    void validate_duplicateTransaction_returnsError() {
        when(portfolioRepository.findById("PORT0001"))
                .thenReturn(Optional.of(activePortfolio));
        when(transactionRepository.findById("TXN00000001"))
                .thenReturn(Optional.of(validTransaction));

        List<String> errors = validationService.validate(validTransaction);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate")));
    }
}
