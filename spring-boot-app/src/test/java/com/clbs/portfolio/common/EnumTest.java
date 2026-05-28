package com.clbs.portfolio.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for enum constants matching COBOL copybook values.
 */
class EnumTest {

    @Test
    void returnCodeShouldMapFromCobolValues() {
        assertEquals(0, ReturnCode.SUCCESS.getCode());
        assertEquals(4, ReturnCode.WARNING.getCode());
        assertEquals(8, ReturnCode.ERROR.getCode());
        assertEquals(12, ReturnCode.SEVERE.getCode());
        assertEquals(16, ReturnCode.CRITICAL.getCode());

        assertEquals(ReturnCode.SUCCESS, ReturnCode.fromCode(0));
        assertEquals(ReturnCode.ERROR, ReturnCode.fromCode(8));
    }

    @Test
    void returnCodeFromCodeShouldThrowForUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> ReturnCode.fromCode(99));
    }

    @Test
    void transactionTypeShouldMapFromCobolCodes() {
        assertEquals("BU", TransactionType.BUY.getCode());
        assertEquals("SL", TransactionType.SELL.getCode());
        assertEquals("TR", TransactionType.TRANSFER.getCode());
        assertEquals("FE", TransactionType.FEE.getCode());

        assertEquals(TransactionType.BUY, TransactionType.fromCode("BU"));
        assertEquals(TransactionType.SELL, TransactionType.fromCode("SL"));
    }

    @Test
    void entityStatusShouldMapFromCobolCodes() {
        assertEquals("A", EntityStatus.ACTIVE.getCode());
        assertEquals("C", EntityStatus.CLOSED.getCode());
        assertEquals("P", EntityStatus.PENDING.getCode());
        assertEquals("S", EntityStatus.SUSPENDED.getCode());
        assertEquals("F", EntityStatus.FAILED.getCode());
        assertEquals("R", EntityStatus.REVERSED.getCode());
    }

    @Test
    void batchStatusShouldMapFromCobolCodes() {
        assertEquals("R", BatchStatus.READY.getCode());
        assertEquals("A", BatchStatus.ACTIVE.getCode());
        assertEquals("W", BatchStatus.WAITING.getCode());
        assertEquals("D", BatchStatus.DONE.getCode());
        assertEquals("E", BatchStatus.ERROR.getCode());
    }

    @Test
    void processTypeShouldMapFromCobolCodes() {
        assertEquals("INI", ProcessType.INITIAL.getCode());
        assertEquals("UPD", ProcessType.UPDATE.getCode());
        assertEquals("RPT", ProcessType.REPORT.getCode());
        assertEquals("CLN", ProcessType.CLEANUP.getCode());
    }

    @Test
    void currencyCodeShouldMatchCobolConstants() {
        assertEquals(5, CurrencyCode.values().length);
        assertNotNull(CurrencyCode.valueOf("USD"));
        assertNotNull(CurrencyCode.valueOf("EUR"));
        assertNotNull(CurrencyCode.valueOf("GBP"));
        assertNotNull(CurrencyCode.valueOf("JPY"));
        assertNotNull(CurrencyCode.valueOf("CAD"));
    }

    @Test
    void checkpointPhaseShouldMapFromCobolCodes() {
        assertEquals("00", CheckpointPhase.INIT.getCode());
        assertEquals("10", CheckpointPhase.READ.getCode());
        assertEquals("20", CheckpointPhase.PROCESS.getCode());
        assertEquals("30", CheckpointPhase.UPDATE.getCode());
        assertEquals("40", CheckpointPhase.TERMINATE.getCode());

        assertEquals(CheckpointPhase.INIT, CheckpointPhase.fromCode("00"));
        assertEquals(CheckpointPhase.PROCESS, CheckpointPhase.fromCode("20"));
    }
}
