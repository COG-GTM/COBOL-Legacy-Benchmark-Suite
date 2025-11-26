package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransferTransactionProcessorTest {

    private TransferTransactionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransferTransactionProcessor();
    }

    @Test
    void shouldReturnTransferAsSupportedType() {
        assertEquals(TransactionType.TRANSFER, processor.getSupportedType());
    }

    @Test
    void shouldThrowUnsupportedOperationException() {
        Portfolio portfolio = new Portfolio("PORT001", "ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100"));
        portfolio.setTotalCost(new BigDecimal("10000"));

        TransactionRequest request = new TransactionRequest(
            "PORT001", "TR", new BigDecimal("50"), null, null);

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> processor.process(request, portfolio));

        assertEquals("Transfer processing not implemented", exception.getMessage());
    }
}
