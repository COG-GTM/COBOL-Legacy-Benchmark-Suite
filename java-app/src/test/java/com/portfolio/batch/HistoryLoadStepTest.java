package com.portfolio.batch;

import com.portfolio.model.PositionHistory;
import com.portfolio.model.TransactionHistory;
import com.portfolio.service.AuditService;
import com.portfolio.service.HistoryLoadService;
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
 * Unit tests for HistoryLoadStep.
 * Tests the Spring Batch ItemProcessor that replaces HISTLD00.cbl batch step.
 */
@ExtendWith(MockitoExtension.class)
class HistoryLoadStepTest {

    @Mock
    private HistoryLoadService historyLoadService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private HistoryLoadStep historyLoadStep;

    @Test
    void process_validTransaction_returnsHistory() throws Exception {
        TransactionHistory txn = createTestTransaction();
        PositionHistory history = createTestHistory();
        when(historyLoadService.loadTransactionToHistory(any(TransactionHistory.class), anyString()))
                .thenReturn(history);

        PositionHistory result = historyLoadStep.process(txn);

        assertNotNull(result);
    }

    @Test
    void process_loadFails_returnsNull() throws Exception {
        TransactionHistory txn = createTestTransaction();
        when(historyLoadService.loadTransactionToHistory(any(TransactionHistory.class), anyString()))
                .thenThrow(new RuntimeException("Load failed"));

        PositionHistory result = historyLoadStep.process(txn);

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

    private PositionHistory createTestHistory() {
        PositionHistory history = new PositionHistory();
        history.setPortfolioId("PORT0001");
        history.setHistoryDate(LocalDate.of(2024, 1, 15));
        history.setHistoryTime(LocalTime.of(10, 0, 0));
        history.setSequenceNo("0001");
        history.setRecordType("TR");
        history.setActionCode("A");
        history.setInvestmentId("INV0000001");
        history.setQuantity(new BigDecimal("100.0000"));
        history.setCostBasis(new BigDecimal("2550.00"));
        history.setMarketValue(new BigDecimal("2600.00"));
        history.setProcessDate(LocalDateTime.now());
        history.setProcessUser("TEST");
        return history;
    }
}
