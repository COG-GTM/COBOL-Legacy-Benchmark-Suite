package com.clbs.portfolio.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for entity mappings, verifying BigDecimal precision and field correctness.
 */
class EntityMappingTest {

    @Test
    void portfolioShouldUseBigDecimalForFinancialFields() {
        Portfolio portfolio = Portfolio.builder()
                .portfolioId("PORT0001")
                .accountNo("1234567890")
                .clientName("Test Client")
                .clientType(Portfolio.ClientType.INDIVIDUAL)
                .status(Portfolio.PortfolioStatus.ACTIVE)
                .totalValue(new BigDecimal("1234567890123.45"))
                .cashBalance(new BigDecimal("9876543210987.65"))
                .build();

        assertEquals("PORT0001", portfolio.getPortfolioId());
        assertEquals(new BigDecimal("1234567890123.45"), portfolio.getTotalValue());
        assertEquals(new BigDecimal("9876543210987.65"), portfolio.getCashBalance());
        assertEquals(Portfolio.ClientType.INDIVIDUAL, portfolio.getClientType());
        assertEquals(Portfolio.PortfolioStatus.ACTIVE, portfolio.getStatus());
    }

    @Test
    void transactionRecordShouldUseBigDecimalForFinancialFields() {
        TransactionRecord txn = TransactionRecord.builder()
                .trnDate("20240320")
                .trnTime("143052")
                .portfolioId("PORT0001")
                .sequenceNo("000001")
                .investmentId("INV0000001")
                .type(TransactionRecord.TransactionType.BUY)
                .quantity(new BigDecimal("100.5000"))
                .price(new BigDecimal("50.2500"))
                .amount(new BigDecimal("5050.13"))
                .currency("USD")
                .status(TransactionRecord.TransactionStatus.DONE)
                .build();

        assertEquals(new BigDecimal("100.5000"), txn.getQuantity());
        assertEquals(new BigDecimal("50.2500"), txn.getPrice());
        assertEquals(new BigDecimal("5050.13"), txn.getAmount());
        assertEquals(TransactionRecord.TransactionType.BUY, txn.getType());
    }

    @Test
    void positionShouldUseBigDecimalForFinancialFields() {
        Position pos = Position.builder()
                .portfolioId("PORT0001")
                .posDate("20240320")
                .investmentId("INV0000001")
                .quantity(new BigDecimal("500.0000"))
                .costBasis(new BigDecimal("25125.00"))
                .marketValue(new BigDecimal("27500.00"))
                .currency("USD")
                .status(Position.PositionStatus.ACTIVE)
                .build();

        assertEquals(new BigDecimal("500.0000"), pos.getQuantity());
        assertEquals(new BigDecimal("25125.00"), pos.getCostBasis());
        assertEquals(new BigDecimal("27500.00"), pos.getMarketValue());
    }

    @Test
    void positionHistoryShouldUseBigDecimalForAllFinancialFields() {
        PositionHistory ph = PositionHistory.builder()
                .accountNo("ACCT0001")
                .portfolioId("PORT000001")
                .transDate(LocalDate.of(2024, 3, 20))
                .transTime(LocalTime.of(14, 30, 52))
                .transType("BU")
                .securityId("SEC000000001")
                .quantity(new BigDecimal("100.000"))
                .price(new BigDecimal("50.250"))
                .amount(new BigDecimal("5025.00"))
                .fees(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("5035.00"))
                .costBasis(new BigDecimal("5025.00"))
                .gainLoss(new BigDecimal("0.00"))
                .processDate(LocalDate.of(2024, 3, 20))
                .processTime(LocalTime.of(14, 31, 0))
                .programId("POSUPD00")
                .userId("USER0001")
                .auditTimestamp(LocalDateTime.now())
                .build();

        assertEquals(new BigDecimal("100.000"), ph.getQuantity());
        assertEquals(new BigDecimal("50.250"), ph.getPrice());
        assertEquals(new BigDecimal("5025.00"), ph.getAmount());
        assertEquals(new BigDecimal("10.00"), ph.getFees());
        assertEquals(new BigDecimal("5035.00"), ph.getTotalAmount());
        assertEquals(BigDecimal.ZERO, PositionHistory.builder().build().getFees());
    }

    @Test
    void errorLogShouldMapEnumsProperly() {
        ErrorLog error = ErrorLog.builder()
                .errorTimestamp(LocalDateTime.now())
                .programId("BCHCTL00")
                .errorType(ErrorLog.ErrorType.SYSTEM)
                .errorSeverity(ErrorLog.ErrorSeverity.ERROR)
                .errorCode("ERR00001")
                .errorMessage("Test error message")
                .processDate(LocalDate.now())
                .processTime(LocalTime.now())
                .userId("USER0001")
                .build();

        assertEquals(ErrorLog.ErrorType.SYSTEM, error.getErrorType());
        assertEquals(ErrorLog.ErrorSeverity.ERROR, error.getErrorSeverity());
    }

    @Test
    void checkpointControlShouldHaveCorrectDefaults() {
        CheckpointControl checkpoint = CheckpointControl.builder()
                .programId("POSUPD00")
                .runDate("20240320")
                .status(CheckpointControl.CheckpointStatus.INITIAL)
                .build();

        assertEquals(1000, checkpoint.getCommitFrequency());
        assertEquals(100, checkpoint.getMaxErrors());
        assertEquals(3, checkpoint.getMaxRestarts());
    }

    @Test
    void historyRecordShouldMapEnumsProperly() {
        HistoryRecord hist = HistoryRecord.builder()
                .portfolioId("PORT0001")
                .histDate("20240320")
                .histTime("143052")
                .seqNo("0001")
                .recordType(HistoryRecord.RecordType.PORTFOLIO)
                .actionCode(HistoryRecord.ActionCode.ADD)
                .beforeImage("")
                .afterImage("{\"portfolioId\":\"PORT0001\"}")
                .reasonCode("NEW")
                .build();

        assertEquals(HistoryRecord.RecordType.PORTFOLIO, hist.getRecordType());
        assertEquals(HistoryRecord.ActionCode.ADD, hist.getActionCode());
    }

    @Test
    void auditRecordShouldMapAllEnumsProperly() {
        AuditRecord audit = AuditRecord.builder()
                .timestamp(LocalDateTime.now())
                .systemId("CLBS0001")
                .userId("USER0001")
                .program("PORTINQ0")
                .auditType(AuditRecord.AuditType.TRANSACTION)
                .action(AuditRecord.AuditAction.CREATE)
                .status(AuditRecord.AuditStatus.SUCCESS)
                .portfolioId("PORT0001")
                .build();

        assertEquals(AuditRecord.AuditType.TRANSACTION, audit.getAuditType());
        assertEquals(AuditRecord.AuditAction.CREATE, audit.getAction());
        assertEquals(AuditRecord.AuditStatus.SUCCESS, audit.getStatus());
    }

    @Test
    void batchControlRecordShouldMapStatusEnums() {
        BatchControlRecord bcr = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlRecord.BatchControlStatus.READY)
                .build();

        assertEquals(BatchControlRecord.BatchControlStatus.READY, bcr.getStatus());
    }

    @Test
    void processSequenceRecordShouldMapEnums() {
        ProcessSequenceRecord psr = ProcessSequenceRecord.builder()
                .processId("TRNVAL00")
                .version(1)
                .description("Transaction Validation")
                .type(ProcessSequenceRecord.SequenceType.PROCESS)
                .frequency(ProcessSequenceRecord.Frequency.DAILY)
                .restartable(true)
                .build();

        assertEquals(ProcessSequenceRecord.SequenceType.PROCESS, psr.getType());
        assertEquals(ProcessSequenceRecord.Frequency.DAILY, psr.getFrequency());
        assertTrue(psr.getRestartable());
    }
}
