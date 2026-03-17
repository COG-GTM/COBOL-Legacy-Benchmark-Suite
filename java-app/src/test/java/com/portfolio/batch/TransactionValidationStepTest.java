package com.portfolio.batch;

import com.portfolio.model.TransactionHistory;
import com.portfolio.service.AuditService;
import com.portfolio.service.TransactionValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TransactionValidationStep.
 * Tests the Spring Batch ItemProcessor that replaces TRNVAL00.cbl batch step.
 */
@ExtendWith(MockitoExtension.class)
class TransactionValidationStepTest {

    @Mock
    private TransactionValidationService validationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TransactionValidationStep validationStep;

    @Test
    void process_validTransaction_returnsProcessedTransaction() throws Exception {
        TransactionHistory txn = createTestTransaction();
        when(validationService.validate(any(TransactionHistory.class)))
                .thenReturn(Collections.emptyList());

        TransactionHistory result = validationStep.process(txn);

        assertNotNull(result);
        assertEquals("P", result.getStatus());
    }

    @Test
    void process_invalidTransaction_returnsNull() throws Exception {
        TransactionHistory txn = createTestTransaction();
        when(validationService.validate(any(TransactionHistory.class)))
                .thenReturn(List.of("Portfolio not found"));

        TransactionHistory result = validationStep.process(txn);

        assertNull(result, "Invalid transactions should be filtered (null)");
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
}
