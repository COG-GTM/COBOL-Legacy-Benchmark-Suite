package com.clbs.portfolio.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumCodeTest {
    @Test
    void transactionTypeCodesRoundTrip() {
        for (TransactionType value : TransactionType.values()) {
            assertEquals(value, TransactionType.fromCode(value.getCode()));
        }
        assertThrows(IllegalArgumentException.class, () -> TransactionType.fromCode("??"));
    }

    @Test
    void transactionStatusCodesRoundTrip() {
        for (TransactionStatus value : TransactionStatus.values()) {
            assertEquals(value, TransactionStatus.fromCode(value.getCode()));
        }
        assertThrows(IllegalArgumentException.class, () -> TransactionStatus.fromCode("??"));
    }

    @Test
    void positionStatusCodesRoundTrip() {
        for (PositionStatus value : PositionStatus.values()) {
            assertEquals(value, PositionStatus.fromCode(value.getCode()));
        }
        assertThrows(IllegalArgumentException.class, () -> PositionStatus.fromCode("??"));
    }

    @Test
    void historyRecordTypeCodesRoundTrip() {
        for (HistoryRecordType value : HistoryRecordType.values()) {
            assertEquals(value, HistoryRecordType.fromCode(value.getCode()));
        }
        assertThrows(IllegalArgumentException.class, () -> HistoryRecordType.fromCode("??"));
    }

    @Test
    void historyActionCodeCodesRoundTrip() {
        for (HistoryActionCode value : HistoryActionCode.values()) {
            assertEquals(value, HistoryActionCode.fromCode(value.getCode()));
        }
        assertThrows(IllegalArgumentException.class, () -> HistoryActionCode.fromCode("??"));
    }
}
