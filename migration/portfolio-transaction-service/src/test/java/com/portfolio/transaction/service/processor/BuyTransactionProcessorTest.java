package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResult;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.enums.TransactionType;
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
class BuyTransactionProcessorTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    private BuyTransactionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BuyTransactionProcessor(portfolioRepository);
    }

    @Test
    void shouldReturnBuyAsSuportedType() {
        assertEquals(TransactionType.BUY, processor.getSupportedType());
    }

    @Test
    void shouldAddUnitsAndCostOnBuy() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("5000"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("150"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("15000"), portfolio.getTotalCost());
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void shouldHandleZeroInitialValues() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(BigDecimal.ZERO);
        portfolio.setTotalCost(BigDecimal.ZERO);

        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("5000"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("5000"), portfolio.getTotalCost());
    }

    @Test
    void shouldHandleDecimalValues() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100.5000"));
        portfolio.setTotalCost(new BigDecimal("10050.00"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", new BigDecimal("50.2500"), new BigDecimal("100"), new BigDecimal("5025.00"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("150.7500"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("15075.00"), portfolio.getTotalCost());
    }
}
