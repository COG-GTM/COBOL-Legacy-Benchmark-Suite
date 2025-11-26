package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResult;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.enums.TransactionType;
import com.portfolio.transaction.exception.InsufficientUnitsException;
import com.portfolio.transaction.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellTransactionProcessorTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    private SellTransactionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SellTransactionProcessor(portfolioRepository);
    }

    @Test
    void shouldReturnSellAsSupportedType() {
        assertEquals(TransactionType.SELL, processor.getSupportedType());
    }

    @Test
    void shouldRejectSellWhenInsufficientUnits() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("50"));
        portfolio.setTotalCost(new BigDecimal("5000"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "SL", new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("10000"));

        InsufficientUnitsException exception = assertThrows(
            InsufficientUnitsException.class,
            () -> processor.process(request, portfolio));

        assertTrue(exception.getMessage().contains("Insufficient units for sale"));
        assertTrue(exception.getMessage().contains("Available: 50"));
        assertTrue(exception.getMessage().contains("Requested: 100"));
    }

    @Test
    void shouldProcessSellWhenSufficientUnits() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "SL", new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("5000"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("50"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("5000"), portfolio.getTotalCost());
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void shouldAllowSellOfExactUnits() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "SL", new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("10000"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.ZERO, portfolio.getTotalUnits());
        assertEquals(BigDecimal.ZERO, portfolio.getTotalCost());
    }

    @Test
    void shouldHandleDecimalValues() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100.5000"));
        portfolio.setTotalCost(new BigDecimal("10050.00"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "SL", new BigDecimal("50.2500"), new BigDecimal("100"), new BigDecimal("5025.00"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("50.2500"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("5025.00"), portfolio.getTotalCost());
    }
}
