package com.portfolio.service;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.InvestmentPositionKey;
import com.portfolio.model.TransactionHistory;
import com.portfolio.repository.InvestmentPositionRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PositionUpdateService.
 * Verifies position update logic migrated from POSUPD00.cbl.
 * All financial calculations use BigDecimal with HALF_UP rounding.
 */
@ExtendWith(MockitoExtension.class)
class PositionUpdateServiceTest {

    @Mock
    private InvestmentPositionRepository positionRepository;

    @InjectMocks
    private PositionUpdateService positionUpdateService;

    private TransactionHistory buyTransaction;
    private TransactionHistory sellTransaction;
    private InvestmentPosition existingPosition;

    @BeforeEach
    void setUp() {
        buyTransaction = new TransactionHistory();
        buyTransaction.setTransactionId("TXN00000001");
        buyTransaction.setPortfolioId("PORT0001");
        buyTransaction.setInvestmentId("INV0000001");
        buyTransaction.setTransactionType("BU");
        buyTransaction.setQuantity(new BigDecimal("100.0000"));
        buyTransaction.setPrice(new BigDecimal("25.50"));
        buyTransaction.setAmount(new BigDecimal("2550.00"));
        buyTransaction.setTransactionDate(LocalDate.of(2024, 1, 15));
        buyTransaction.setTransactionTime(LocalTime.of(10, 0, 0));
        buyTransaction.setCurrencyCode("USD");
        buyTransaction.setProcessDate(LocalDateTime.now());
        buyTransaction.setProcessUser("TEST");
        buyTransaction.setStatus("P");

        sellTransaction = new TransactionHistory();
        sellTransaction.setTransactionId("TXN00000002");
        sellTransaction.setPortfolioId("PORT0001");
        sellTransaction.setInvestmentId("INV0000001");
        sellTransaction.setTransactionType("SL");
        sellTransaction.setQuantity(new BigDecimal("50.0000"));
        sellTransaction.setPrice(new BigDecimal("30.00"));
        sellTransaction.setAmount(new BigDecimal("1500.00"));
        sellTransaction.setTransactionDate(LocalDate.of(2024, 1, 20));
        sellTransaction.setTransactionTime(LocalTime.of(10, 0, 0));
        sellTransaction.setCurrencyCode("USD");
        sellTransaction.setProcessDate(LocalDateTime.now());
        sellTransaction.setProcessUser("TEST");
        sellTransaction.setStatus("P");

        existingPosition = new InvestmentPosition();
        InvestmentPositionKey key = new InvestmentPositionKey();
        key.setPortfolioId("PORT0001");
        key.setInvestmentId("INV0000001");
        key.setPositionDate(LocalDate.of(2024, 1, 1));
        existingPosition.setKey(key);
        existingPosition.setQuantity(new BigDecimal("200.0000"));
        existingPosition.setCostBasis(new BigDecimal("5000.00"));
        existingPosition.setMarketValue(new BigDecimal("5500.00"));
        existingPosition.setCurrencyCode("USD");
        existingPosition.setStatus("A");
        existingPosition.setLastMaintDate(LocalDateTime.now());
        existingPosition.setLastMaintUser("TEST");
    }

    @Test
    void updatePosition_buyTransaction_addsToPosition() {
        InvestmentPositionKey expectedKey = new InvestmentPositionKey(
                "PORT0001", "INV0000001", LocalDate.of(2024, 1, 15));
        when(positionRepository.findById(expectedKey))
                .thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(InvestmentPosition.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InvestmentPosition result = positionUpdateService.updatePosition(buyTransaction, "TEST");

        assertNotNull(result);
        // 200 + 100 = 300
        assertEquals(0, new BigDecimal("300.0000").compareTo(result.getQuantity()));
        // 5000 + 2550 = 7550
        assertEquals(0, new BigDecimal("7550.00").compareTo(result.getCostBasis()));
    }

    @Test
    void updatePosition_sellTransaction_subtractsFromPosition() {
        InvestmentPositionKey expectedKey = new InvestmentPositionKey(
                "PORT0001", "INV0000001", LocalDate.of(2024, 1, 20));
        when(positionRepository.findById(expectedKey))
                .thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(InvestmentPosition.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InvestmentPosition result = positionUpdateService.updatePosition(sellTransaction, "TEST");

        assertNotNull(result);
        // 200 - 50 = 150
        assertEquals(0, new BigDecimal("150.0000").compareTo(result.getQuantity()));
    }

    @Test
    void updatePosition_newPosition_createsRecord() {
        InvestmentPositionKey expectedKey = new InvestmentPositionKey(
                "PORT0001", "INV0000001", LocalDate.of(2024, 1, 15));
        when(positionRepository.findById(expectedKey))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any(InvestmentPosition.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InvestmentPosition result = positionUpdateService.updatePosition(buyTransaction, "TEST");

        assertNotNull(result);
        assertEquals(0, new BigDecimal("100.0000").compareTo(result.getQuantity()));
        assertEquals(0, new BigDecimal("2550.00").compareTo(result.getCostBasis()));
    }
}
