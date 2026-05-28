package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository integration tests using H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @Autowired
    private BatchControlRecordRepository batchControlRecordRepository;

    @Autowired
    private ProcessSequenceRecordRepository processSequenceRecordRepository;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Autowired
    private HistoryRecordRepository historyRecordRepository;

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @Autowired
    private CheckpointControlRepository checkpointControlRepository;

    @Test
    void shouldSaveAndFindPortfolioByStatus() {
        Portfolio p1 = Portfolio.builder()
                .portfolioId("PORT0001")
                .accountNo("1234567890")
                .clientName("Active Client")
                .clientType(Portfolio.ClientType.INDIVIDUAL)
                .status(Portfolio.PortfolioStatus.ACTIVE)
                .totalValue(new BigDecimal("100000.00"))
                .cashBalance(new BigDecimal("5000.00"))
                .build();
        Portfolio p2 = Portfolio.builder()
                .portfolioId("PORT0002")
                .accountNo("0987654321")
                .clientName("Closed Client")
                .clientType(Portfolio.ClientType.CORPORATE)
                .status(Portfolio.PortfolioStatus.CLOSED)
                .totalValue(BigDecimal.ZERO)
                .cashBalance(BigDecimal.ZERO)
                .build();

        portfolioRepository.save(p1);
        portfolioRepository.save(p2);

        List<Portfolio> active = portfolioRepository.findByStatus(Portfolio.PortfolioStatus.ACTIVE);
        assertEquals(1, active.size());
        assertEquals("PORT0001", active.get(0).getPortfolioId());

        List<Portfolio> individuals = portfolioRepository.findByClientType(Portfolio.ClientType.INDIVIDUAL);
        assertEquals(1, individuals.size());
    }

    @Test
    void shouldSaveAndFindTransactionsByDateRange() {
        TransactionRecord txn1 = TransactionRecord.builder()
                .trnDate("20240315")
                .trnTime("100000")
                .portfolioId("PORT0001")
                .sequenceNo("000001")
                .type(TransactionRecord.TransactionType.BUY)
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("50.0000"))
                .amount(new BigDecimal("5000.00"))
                .status(TransactionRecord.TransactionStatus.DONE)
                .build();
        TransactionRecord txn2 = TransactionRecord.builder()
                .trnDate("20240320")
                .trnTime("143000")
                .portfolioId("PORT0001")
                .sequenceNo("000002")
                .type(TransactionRecord.TransactionType.SELL)
                .quantity(new BigDecimal("50.0000"))
                .price(new BigDecimal("55.0000"))
                .amount(new BigDecimal("2750.00"))
                .status(TransactionRecord.TransactionStatus.DONE)
                .build();

        transactionRecordRepository.save(txn1);
        transactionRecordRepository.save(txn2);

        List<TransactionRecord> results = transactionRecordRepository
                .findByPortfolioIdAndTrnDateBetween("PORT0001", "20240310", "20240325");
        assertEquals(2, results.size());

        List<TransactionRecord> narrowRange = transactionRecordRepository
                .findByPortfolioIdAndTrnDateBetween("PORT0001", "20240318", "20240325");
        assertEquals(1, narrowRange.size());
    }

    @Test
    void shouldSaveAndFindPositionsByPortfolioAndDate() {
        Position pos = Position.builder()
                .portfolioId("PORT0001")
                .posDate("20240320")
                .investmentId("INV0000001")
                .quantity(new BigDecimal("500.0000"))
                .costBasis(new BigDecimal("25000.00"))
                .marketValue(new BigDecimal("27500.00"))
                .currency("USD")
                .status(Position.PositionStatus.ACTIVE)
                .build();

        positionRepository.save(pos);

        List<Position> results = positionRepository
                .findByPortfolioIdAndPosDate("PORT0001", "20240320");
        assertEquals(1, results.size());
        assertEquals(new BigDecimal("500.0000"), results.get(0).getQuantity());
    }

    @Test
    void shouldSaveAndFindPositionHistory() {
        PositionHistory ph = PositionHistory.builder()
                .accountNo("ACCT0001")
                .portfolioId("PORT000001")
                .transDate(LocalDate.of(2024, 3, 20))
                .transTime(LocalTime.of(14, 30))
                .transType("BU")
                .securityId("SEC000000001")
                .quantity(new BigDecimal("100.000"))
                .price(new BigDecimal("50.250"))
                .amount(new BigDecimal("5025.00"))
                .fees(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("5035.00"))
                .costBasis(new BigDecimal("5025.00"))
                .gainLoss(BigDecimal.ZERO)
                .processDate(LocalDate.of(2024, 3, 20))
                .processTime(LocalTime.of(14, 31))
                .programId("POSUPD00")
                .userId("USER0001")
                .auditTimestamp(LocalDateTime.now())
                .build();

        positionHistoryRepository.save(ph);

        List<PositionHistory> results = positionHistoryRepository
                .findByAccountNoAndPortfolioId("ACCT0001", "PORT000001");
        assertEquals(1, results.size());
    }

    @Test
    void shouldSaveAndFindBatchControlRecordByStatus() {
        BatchControlRecord bcr = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlRecord.BatchControlStatus.READY)
                .programName("TRNVAL00")
                .prereqCount(0)
                .build();

        batchControlRecordRepository.save(bcr);

        List<BatchControlRecord> byStatus = batchControlRecordRepository
                .findByStatus(BatchControlRecord.BatchControlStatus.READY);
        assertEquals(1, byStatus.size());

        List<BatchControlRecord> byJobDate = batchControlRecordRepository
                .findByJobNameAndProcessDate("TRNVAL00", "20240320");
        assertEquals(1, byJobDate.size());
    }

    @Test
    void shouldSaveAndFindProcessSequenceByType() {
        ProcessSequenceRecord psr = ProcessSequenceRecord.builder()
                .processId("TRNVAL00")
                .version(1)
                .description("Transaction Validation")
                .type(ProcessSequenceRecord.SequenceType.PROCESS)
                .frequency(ProcessSequenceRecord.Frequency.DAILY)
                .startTime(800)
                .restartable(true)
                .build();

        processSequenceRecordRepository.save(psr);

        List<ProcessSequenceRecord> results = processSequenceRecordRepository
                .findByTypeOrderByStartTimeAsc(ProcessSequenceRecord.SequenceType.PROCESS);
        assertEquals(1, results.size());
        assertEquals("TRNVAL00", results.get(0).getProcessId());
    }

    @Test
    void shouldSaveAndFindAuditRecords() {
        LocalDateTime now = LocalDateTime.now();
        AuditRecord audit = AuditRecord.builder()
                .timestamp(now)
                .systemId("CLBS0001")
                .userId("USER0001")
                .program("PORTINQ0")
                .auditType(AuditRecord.AuditType.TRANSACTION)
                .action(AuditRecord.AuditAction.INQUIRE)
                .status(AuditRecord.AuditStatus.SUCCESS)
                .portfolioId("PORT0001")
                .build();

        auditRecordRepository.save(audit);

        List<AuditRecord> byPortfolio = auditRecordRepository.findByPortfolioId("PORT0001");
        assertEquals(1, byPortfolio.size());

        List<AuditRecord> byTimestamp = auditRecordRepository
                .findByTimestampBetween(now.minusMinutes(1), now.plusMinutes(1));
        assertEquals(1, byTimestamp.size());
    }

    @Test
    void shouldSaveAndFindHistoryRecords() {
        HistoryRecord hist = HistoryRecord.builder()
                .portfolioId("PORT0001")
                .histDate("20240320")
                .histTime("143052")
                .seqNo("0001")
                .recordType(HistoryRecord.RecordType.PORTFOLIO)
                .actionCode(HistoryRecord.ActionCode.ADD)
                .beforeImage("")
                .afterImage("test after image")
                .build();

        historyRecordRepository.save(hist);

        List<HistoryRecord> results = historyRecordRepository
                .findByPortfolioIdAndHistDateBetween("PORT0001", "20240315", "20240325");
        assertEquals(1, results.size());
    }

    @Test
    void shouldSaveAndFindErrorLogs() {
        ErrorLog error = ErrorLog.builder()
                .errorTimestamp(LocalDateTime.now())
                .programId("BCHCTL00")
                .errorType(ErrorLog.ErrorType.APPLICATION)
                .errorSeverity(ErrorLog.ErrorSeverity.WARNING)
                .errorCode("WARN0001")
                .errorMessage("Test warning")
                .processDate(LocalDate.of(2024, 3, 20))
                .processTime(LocalTime.now())
                .userId("USER0001")
                .build();

        errorLogRepository.save(error);

        List<ErrorLog> results = errorLogRepository
                .findByProgramIdAndProcessDate("BCHCTL00", LocalDate.of(2024, 3, 20));
        assertEquals(1, results.size());
    }

    @Test
    void shouldSaveAndFindCheckpointControl() {
        CheckpointControl ck = CheckpointControl.builder()
                .programId("POSUPD00")
                .runDate("20240320")
                .runTime("143052")
                .status(CheckpointControl.CheckpointStatus.INITIAL)
                .phase(CheckpointControl.CheckpointPhase.INIT)
                .recordsRead(0L)
                .recordsProcessed(0L)
                .recordsError(0L)
                .restartCount(0)
                .build();

        checkpointControlRepository.save(ck);

        var result = checkpointControlRepository.findByProgramIdAndRunDate("POSUPD00", "20240320");
        assertTrue(result.isPresent());
        assertEquals(1000, result.get().getCommitFrequency());
    }
}
