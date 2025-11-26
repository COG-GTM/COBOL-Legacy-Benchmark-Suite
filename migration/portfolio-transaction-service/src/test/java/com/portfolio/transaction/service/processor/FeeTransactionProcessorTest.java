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
class FeeTransactionProcessorTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    private FeeTransactionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new FeeTransactionProcessor(portfolioRepository);
    }

    @Test
    void shouldReturnFeeAsSupportedType() {
        assertEquals(TransactionType.FEE, processor.getSupportedType());
    }

    @Test
    void shouldSubtractCostWithoutAffectingUnits() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "FE", new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("9950"), portfolio.getTotalCost());
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void shouldHandleDecimalFees() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000.00"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "FE", new BigDecimal("1"), new BigDecimal("25.50"), new BigDecimal("25.50"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("9974.50"), portfolio.getTotalCost());
    }

    @Test
    void shouldAllowNegativeCostAfterFee() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("50"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "FE", new BigDecimal("1"), new BigDecimal("100"), new BigDecimal("100"));

        TransactionResult result = processor.process(request, portfolio);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("-50"), portfolio.getTotalCost());
    }
}
