package com.portfolio.batch;

import com.portfolio.domain.BatchControl;
import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.TransactionHistoryFileRecord;
import com.portfolio.repository.BatchControlRepository;
import com.portfolio.repository.ErrorLogRepository;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.repository.TransactionHistoryFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the HISTLD00 Spring Batch migration against embedded H2.
 */
@SpringBootTest
class HistoryLoadJobTest {

    private static final String PROCESS_DATE = "20240320";
    private static final LocalDate TRANS_DATE = LocalDate.of(2024, 3, 20);

    @Autowired private JobLauncher jobLauncher;
    @Autowired private Job histld00Job;
    @Autowired private HistoryLoadStats stats;
    @Autowired private TransactionHistoryFileRepository tranHistRepository;
    @Autowired private PositionHistoryRepository positionHistoryRepository;
    @Autowired private BatchControlRepository batchControlRepository;
    @Autowired private ErrorLogRepository errorLogRepository;

    @BeforeEach
    void setUp() {
        cleanUp();
        BatchControl control = new BatchControl();
        control.setKey(new BatchControl.Key("HISTLD00", PROCESS_DATE, 1));
        control.setStatus("R");
        control.setStepName("STEP010");
        control.setProgramName("HISTLD00");
        batchControlRepository.save(control);
    }

    @AfterEach
    void cleanUp() {
        tranHistRepository.deleteAll();
        positionHistoryRepository.deleteAll();
        batchControlRepository.deleteAll();
        errorLogRepository.deleteAll();
    }

    private JobExecution runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("processDate", PROCESS_DATE)
                .addLong("startedAt", System.nanoTime())
                .toJobParameters();
        return jobLauncher.run(histld00Job, params);
    }

    private TransactionHistoryFileRecord validRecord(String portfolioId, String seq, LocalTime time) {
        TransactionHistoryFileRecord rec = new TransactionHistoryFileRecord();
        rec.setKey(new TransactionHistoryFileRecord.Key(TRANS_DATE, time, portfolioId, seq));
        rec.setAccountNo("ACCT0001");
        rec.setTransType("BU");
        rec.setSecurityId("IBM");
        rec.setQuantity(new BigDecimal("100.000"));
        rec.setPrice(new BigDecimal("185.500"));
        rec.setAmount(new BigDecimal("18550.00"));
        rec.setFees(new BigDecimal("9.99"));
        rec.setTotalAmount(new BigDecimal("18559.99"));
        rec.setCostBasis(new BigDecimal("18559.99"));
        rec.setGainLoss(BigDecimal.ZERO);
        return rec;
    }

    @Test
    void loadsValidRecordsIntoPositionHistory() throws Exception {
        tranHistRepository.save(validRecord("PORT00001", "000001", LocalTime.of(9, 30)));
        tranHistRepository.save(validRecord("PORT00002", "000002", LocalTime.of(10, 15)));

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(positionHistoryRepository.count()).isEqualTo(2);
        assertThat(stats.getRecordsRead()).isEqualTo(2);
        assertThat(stats.getRecordsWritten()).isEqualTo(2);
        assertThat(stats.getErrorCount()).isZero();

        Optional<PositionHistory> loaded = positionHistoryRepository.findById(
                new PositionHistory.Key("ACCT0001", "PORT00001", TRANS_DATE, LocalTime.of(9, 30)));
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getQuantity()).isEqualByComparingTo("100.000");
        assertThat(loaded.get().getTotalAmount()).isEqualByComparingTo("18559.99");
        assertThat(loaded.get().getProgramId()).isEqualTo("HISTLD00");
    }

    @Test
    void updatesBatchControlRecordWithCountersAndStatus() throws Exception {
        tranHistRepository.save(validRecord("PORT00001", "000001", LocalTime.of(9, 30)));

        runJob();

        BatchControl control = batchControlRepository
                .findById(new BatchControl.Key("HISTLD00", PROCESS_DATE, 1)).orElseThrow();
        assertThat(control.getStatus()).isEqualTo("D");
        assertThat(control.getRecordsRead()).isEqualTo(1);
        assertThat(control.getRecordsWritten()).isEqualTo(1);
        assertThat(control.getReturnCode()).isZero();
        assertThat(control.getRestartCount()).isEqualTo(1);
    }

    @Test
    void skipsDuplicateRecordsLikeSqlcodeMinus803() throws Exception {
        TransactionHistoryFileRecord rec = validRecord("PORT00001", "000001", LocalTime.of(9, 30));
        tranHistRepository.save(rec);

        // Pre-existing POSHIST row with the same key = duplicate insert (-803)
        PositionHistory existing = new PositionHistory();
        existing.setKey(new PositionHistory.Key("ACCT0001", "PORT00001", TRANS_DATE, LocalTime.of(9, 30)));
        existing.setTransType("BU");
        existing.setSecurityId("IBM");
        existing.setQuantity(BigDecimal.ONE);
        existing.setPrice(BigDecimal.ONE);
        existing.setAmount(BigDecimal.ONE);
        existing.setFees(BigDecimal.ZERO);
        existing.setTotalAmount(BigDecimal.ONE);
        existing.setCostBasis(BigDecimal.ONE);
        existing.setGainLoss(BigDecimal.ZERO);
        existing.setProcessDate(TRANS_DATE);
        existing.setProcessTime(LocalTime.NOON);
        existing.setProgramId("HISTLD00");
        existing.setUserId("TEST");
        existing.setAuditTimestamp(TRANS_DATE.atTime(LocalTime.NOON));
        positionHistoryRepository.save(existing);

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(positionHistoryRepository.count()).isEqualTo(1);
        // duplicate not counted as written and not counted as an error
        assertThat(stats.getRecordsWritten()).isZero();
        assertThat(stats.getErrorCount()).isZero();
        // original row untouched (COBOL CONTINUE on -803)
        PositionHistory unchanged = positionHistoryRepository.findById(existing.getKey()).orElseThrow();
        assertThat(unchanged.getQuantity()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void countsValidationErrorsAndLogsToErrlog() throws Exception {
        tranHistRepository.save(validRecord("PORT00001", "000001", LocalTime.of(9, 30)));
        TransactionHistoryFileRecord bad = validRecord("PORT00002", "000002", LocalTime.of(10, 0));
        bad.setTransType("XX");
        tranHistRepository.save(bad);

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(positionHistoryRepository.count()).isEqualTo(1);
        assertThat(stats.getErrorCount()).isEqualTo(1);
        assertThat(errorLogRepository.count()).isEqualTo(1);
        assertThat(errorLogRepository.findAll().get(0).getErrorMessage())
                .contains("Invalid transaction type");

        BatchControl control = batchControlRepository
                .findById(new BatchControl.Key("HISTLD00", PROCESS_DATE, 1)).orElseThrow();
        assertThat(control.getReturnCode()).isEqualTo(1);
        assertThat(control.getStatus()).isEqualTo("D");
    }

    @Test
    void abortsWhenErrorCountExceedsOneHundred() throws Exception {
        for (int i = 0; i < 105; i++) {
            TransactionHistoryFileRecord bad = validRecord("PORT00001",
                    String.format("%06d", i + 1),
                    LocalTime.of(9, 0).plusSeconds(i));
            bad.setTransType("XX");
            tranHistRepository.save(bad);
        }

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(stats.getErrorCount()).isEqualTo(101);
        // every counted error has its own ERRLOG row (no key collisions)
        assertThat(errorLogRepository.count()).isEqualTo(101);
        assertThat(execution.getAllFailureExceptions())
                .anyMatch(e -> e instanceof ErrorLimitExceededException);

        BatchControl control = batchControlRepository
                .findById(new BatchControl.Key("HISTLD00", PROCESS_DATE, 1)).orElseThrow();
        assertThat(control.getStatus()).isEqualTo("E");
        assertThat(control.getReturnCode()).isEqualTo(101);
    }

    @Test
    void failsWhenControlRecordMissing() throws Exception {
        batchControlRepository.deleteAll();
        tranHistRepository.save(validRecord("PORT00001", "000001", LocalTime.of(9, 30)));

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(positionHistoryRepository.count()).isZero();
    }
}
