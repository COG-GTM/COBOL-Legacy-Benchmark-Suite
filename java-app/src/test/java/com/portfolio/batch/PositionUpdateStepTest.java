package com.portfolio.batch;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.InvestmentPositionKey;
import com.portfolio.model.TransactionHistory;
import com.portfolio.service.AuditService;
import com.portfolio.service.PositionUpdateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PositionUpdateStep.
 * Tests the Spring Batch ItemProcessor that replaces POSUPD00.cbl batch step.
 */
@ExtendWith(MockitoExtension.class)
class PositionUpdateStepTest {

    @Mock
    private PositionUpdateService positionUpdateService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PositionUpdateStep positionUpdateStep;

    @Test
    void process_validTransaction_returnsUpdatedPosition() throws Exception {
        TransactionHistory txn = createTestTransaction();
        InvestmentPosition position = createTestPosition();
        when(positionUpdateService.updatePosition(any(TransactionHistory.class), anyString()))
                .thenReturn(position);

        InvestmentPosition result = positionUpdateStep.process(txn);

        assertNotNull(result);
    }

    @Test
    void process_updateFails_returnsNull() throws Exception {
        TransactionHistory txn = createTestTransaction();
        when(positionUpdateService.updatePosition(any(TransactionHistory.class), anyString()))
                .thenThrow(new RuntimeException("Update failed"));

        InvestmentPosition result = positionUpdateStep.process(txn);

        assertNull(result);
    }

    private TransactionHistory createTestTransaction() {
        TransactionHistory txn = new TransactionHistory();
        txn.setTransactionId("TXN00000001");
        txn.setPortfolioId("PORT0001");
        txn.setInvestmentId("INV0000001");
        txn.setTransactionType("BU");
        txn.setQuantity(new BigDecimal("100.0000"));
        txn.setPrice(new BigDecimal("25.50"));
        txn.setAmount(new BigDecimal("2550.00"));
        return txn;
    }

    private InvestmentPosition createTestPosition() {
        InvestmentPosition pos = new InvestmentPosition();
        InvestmentPositionKey key = new InvestmentPositionKey();
        key.setPortfolioId("PORT0001");
        key.setInvestmentId("INV0000001");
        key.setPositionDate(LocalDate.of(2024, 1, 15));
        pos.setKey(key);
        pos.setQuantity(new BigDecimal("300.0000"));
        pos.setCostBasis(new BigDecimal("7550.00"));
        pos.setMarketValue(new BigDecimal("8000.00"));
        pos.setCurrencyCode("USD");
        pos.setStatus("A");
        pos.setLastMaintDate(LocalDateTime.now());
        pos.setLastMaintUser("TEST");
        return pos;
    }
}
