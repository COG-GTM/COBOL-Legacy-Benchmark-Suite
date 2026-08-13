package com.ipms.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainRecordRoundTripTest {

    @Test
    void transactionRecordRoundTrips() {
        TransactionRecord original = new TransactionRecord(
                "20240320", "093015", "PORT0001", "000001",
                "INV0000001", TransactionType.BUY,
                new BigDecimal("100.2500"), new BigDecimal("25.7500"),
                new BigDecimal("2581.44"), "USD", TransactionStatus.PENDING,
                "2024-03-20-09.30.15.000000", "TESTUSER");

        String record = original.toRecord();
        assertEquals(TransactionRecord.RECORD_LENGTH, record.length());
        assertEquals(original, TransactionRecord.parse(record));
    }

    @Test
    void positionRecordRoundTrips() {
        PositionRecord original = new PositionRecord(
                "PORT0001", "20240320", "INV0000001",
                new BigDecimal("500.0000"), new BigDecimal("12500.00"),
                new BigDecimal("13750.50"), "USD", PositionStatus.ACTIVE,
                "2024-03-20-17.00.00.000000", "TESTUSER");

        String record = original.toRecord();
        assertEquals(PositionRecord.RECORD_LENGTH, record.length());
        assertEquals(original, PositionRecord.parse(record));
    }

    @Test
    void portfolioRecordRoundTrips() {
        PortfolioRecord original = new PortfolioRecord(
                "PORT0001", "ACCT000001", "JOHN DOE", ClientType.INDIVIDUAL,
                "20240101", "20240320", PortfolioStatus.ACTIVE,
                new BigDecimal("100000.00"), new BigDecimal("-2500.00"),
                "TESTUSER", "00000042");

        String record = original.toRecord();
        assertEquals(PortfolioRecord.RECORD_LENGTH, record.length());
        assertEquals(original, PortfolioRecord.parse(record));
    }

    @Test
    void historyRecordRoundTrips() {
        HistoryRecord original = new HistoryRecord(
                "PORT0001", "20240320", "093015", "0001",
                HistoryRecordType.TRANSACTION, HistoryActionCode.ADD,
                "BEFORE", "AFTER", "RC01",
                "2024-03-20-09.30.15.000000", "TESTUSER");

        String record = original.toRecord();
        assertEquals(HistoryRecord.RECORD_LENGTH, record.length());
        assertEquals(original, HistoryRecord.parse(record));
    }

    @Test
    void auditRecordRoundTrips() {
        AuditRecord original = new AuditRecord(
                "2024-03-20-09.30.15.000000", "IPMS", "TESTUSER", "PORTMSTR", "TERM0001",
                AuditType.TRANSACTION, AuditAction.CREATE, AuditStatus.SUCCESS,
                "PORT0001", "ACCT000001", "BEFORE", "AFTER", "Created portfolio");

        String record = original.toRecord();
        assertEquals(AuditRecord.RECORD_LENGTH, record.length());
        assertEquals(original, AuditRecord.parse(record));
    }

    @Test
    void errorMessageRoundTrips() {
        ErrorMessage original = new ErrorMessage(
                "2024-03-20", "09:30:15", "PORTMSTR", ErrorCategory.VALIDATION,
                "E001", 8, "Invalid portfolio ID", "Portfolio ID must start with PORT");

        String record = original.toRecord();
        assertEquals(ErrorMessage.RECORD_LENGTH, record.length());
        assertEquals(original, ErrorMessage.parse(record));
    }
}
