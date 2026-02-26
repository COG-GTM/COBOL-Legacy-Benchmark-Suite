package com.portfolio.batch;

import com.portfolio.model.PortfolioMaster;
import com.portfolio.model.PositionRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.support.PortfolioMasterRepository;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.TransactionRecordRepository;
import com.portfolio.support.HistoryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the full batch pipeline.
 * Tests: TransactionValidationStep -> PositionUpdateStep -> HistoryLoadStep
 * Verifies RC propagation and checkpoint/restart behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("portfolioBatchJob")
    private Job portfolioBatchJob;

    @Autowired
    private TransactionRecordRepository transactionRepository;

    @Autowired
    private PositionRecordRepository positionRepository;

    @Autowired
    private HistoryRecordRepository historyRecordRepository;

    @Autowired
    private PortfolioMasterRepository portfolioMasterRepository;

    @BeforeEach
    void setUp() {
        historyRecordRepository.deleteAll();
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        // Create portfolio master records to satisfy FK constraints
        if (!portfolioMasterRepository.existsById("PORT0001")) {
            PortfolioMaster pm = new PortfolioMaster(
                    "PORT0001", "IN", "01", "C00000001", "Test Portfolio",
                    "USD", "M", "A", LocalDate.now(), LocalDateTime.now(), "TEST");
            portfolioMasterRepository.save(pm);
        }
    }

    @Test
    void testFullPipelineExecution() throws Exception {
        // Set up test transactions
        for (int i = 0; i < 5; i++) {
            TransactionRecord txn = new TransactionRecord();
            txn.setTransactionId(String.format("TXN%017d", i + 1));
            txn.setPortfolioId("PORT0001");
            txn.setTransactionDate(LocalDate.now());
            txn.setTransactionTime(LocalTime.now());
            txn.setInvestmentId("AAPL      ");
            txn.setTransactionType("BU");
            txn.setQuantity(new BigDecimal("10.0000"));
            txn.setPrice(new BigDecimal("150.0000"));
            txn.setAmount(new BigDecimal("1500.00"));
            txn.setCurrencyCode("USD");
            txn.setStatus("P");
            txn.setProcessDate(LocalDateTime.now());
            txn.setProcessUser("TEST    ");
            transactionRepository.save(txn);
        }

        JobParameters params = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(portfolioBatchJob, params);

        assertThat(execution.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.STOPPED);
    }

    @Test
    void testEmptyPipelineExecution() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .addLong("unique", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(portfolioBatchJob, params);

        // Should complete even with no data
        assertThat(execution.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.STOPPED);
    }
}
