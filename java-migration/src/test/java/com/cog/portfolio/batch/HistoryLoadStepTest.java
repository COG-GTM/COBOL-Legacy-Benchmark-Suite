package com.cog.portfolio.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.cog.portfolio.TestData;
import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.domain.PositionHistoryId;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.domain.TransactionType;
import com.cog.portfolio.repository.PositionHistoryRepository;
import com.cog.portfolio.repository.PortfolioRepository;
import com.cog.portfolio.repository.ReturnCodeLogRepository;
import com.cog.portfolio.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * HISTLD00: domain (scale 4) -> POSHIST (scale 3) through ScaleConverter/HALF_UP,
 * duplicate keys (SQLCODE -803) ignored, others fail the chunk.
 */
@SpringBootTest
class HistoryLoadStepTest {

    @Autowired
    JobLauncher jobLauncher;
    @Autowired
    @Qualifier("historyLoadJob")
    Job historyLoadJob;
    @Autowired
    PortfolioRepository portfolioRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    PositionHistoryRepository positionHistoryRepository;
    @Autowired
    ReturnCodeLogRepository returnCodeLogRepository;
    @Autowired
    HistoryLoadProcessor processor;
    @Autowired
    HistoryLoadWriter writer;

    @BeforeEach
    void setUp() {
        cleanup();
        portfolioRepository.save(TestData.portfolio());
    }

    @AfterEach
    void cleanup() {
        positionHistoryRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    private Transaction done(String seq, String quantity, String price, String amount, LocalTime time) {
        Transaction trn = TestData.transaction(TransactionType.BUY, seq, quantity, price, amount);
        Transaction withTime = new Transaction(
                new com.cog.portfolio.domain.TransactionId(TestData.DATE, time, TestData.PORTFOLIO_ID, seq),
                TestData.INVESTMENT_ID, TransactionType.BUY);
        withTime.setQuantity(trn.getQuantity());
        withTime.setPrice(trn.getPrice());
        withTime.setAmount(trn.getAmount());
        withTime.setStatus(TransactionStatus.DONE);
        return transactionRepository.save(withTime);
    }

    private JobExecution run() throws Exception {
        return jobLauncher.run(historyLoadJob, new JobParametersBuilder()
                .addLong("run", System.nanoTime()).toJobParameters());
    }

    @Test
    void loadsHistoryWithExplicitHalfUpConversionAndReportsResidue() throws Exception {
        long lossBefore = processor.getPrecisionLossCount();
        done("000001", "100.1235", "10.0004", "1001.24", LocalTime.of(9, 0, 1));
        done("000002", "50.0000", "20.0000", "1000.00", LocalTime.of(9, 0, 2));

        JobExecution execution = run();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode());

        PositionHistory rounded = positionHistoryRepository.findById(new PositionHistoryId(
                TestData.ACCOUNT_NO, TestData.PORTFOLIO_ID, TestData.DATE, LocalTime.of(9, 0, 1))).orElseThrow();
        assertThat(rounded.getQuantity()).isEqualByComparingTo("100.124"); // .5 -> up
        assertThat(rounded.getQuantity().scale()).isEqualTo(3);
        assertThat(rounded.getPrice()).isEqualByComparingTo("10.000");    // .0004 -> down, not truncated to .000 by accident
        assertThat(rounded.getPrice().scale()).isEqualTo(3);
        assertThat(rounded.getAmount()).isEqualByComparingTo("1001.24");

        PositionHistory exact = positionHistoryRepository.findById(new PositionHistoryId(
                TestData.ACCOUNT_NO, TestData.PORTFOLIO_ID, TestData.DATE, LocalTime.of(9, 0, 2))).orElseThrow();
        assertThat(exact.getQuantity()).isEqualByComparingTo("50.000");

        // two lossy conversions (quantity and price of the first record), none for the second
        assertThat(processor.getPrecisionLossCount() - lossBefore).isEqualTo(2);
        assertThat(returnCodeLogRepository.findByIdProgramIdOrderByIdTimestampDesc("HISTLD00")).isNotEmpty();
    }

    @Test
    void duplicateKeyIsIgnoredLikeSqlcodeMinus803() throws Exception {
        long dupBefore = writer.getDuplicatesSkipped();
        done("000003", "1.0000", "1.0000", "1.00", LocalTime.of(9, 5, 0));
        run();
        assertThat(positionHistoryRepository.count()).isEqualTo(1);

        // second load of the same transaction -> same POSHIST key -> skipped, job still COMPLETED
        JobExecution second = run();
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(positionHistoryRepository.count()).isEqualTo(1);
        assertThat(writer.getDuplicatesSkipped() - dupBefore).isEqualTo(1);
        StepExecution step = second.getStepExecutions().iterator().next();
        assertThat(step.getWriteCount()).isEqualTo(1);
    }

    @Test
    void chunkSizeIsTheLegacyCommitThreshold(@Autowired BatchConfiguration.BatchProperties properties) {
        assertThat(properties.commitInterval()).isEqualTo(1000);
    }

    @Test
    void processorNeverProducesFloatingPoint() {
        Transaction trn = TestData.transaction(TransactionType.SELL, "000009", "3.3333", "0.3333", "1.11");
        PositionHistory ph = processor.process(trn);
        assertThat(ph.getQuantity()).isInstanceOf(BigDecimal.class).isEqualByComparingTo("3.333");
        assertThat(ph.getPrice()).isEqualByComparingTo("0.333");
    }
}
