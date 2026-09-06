package com.cog.portfolio.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.cog.portfolio.TestData;
import com.cog.portfolio.common.ReturnCode;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionId;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.domain.TransactionType;
import com.cog.portfolio.repository.PositionHistoryRepository;
import com.cog.portfolio.repository.PortfolioRepository;
import com.cog.portfolio.repository.PositionRepository;
import com.cog.portfolio.repository.ReturnCodeLogRepository;
import com.cog.portfolio.repository.TransactionRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * PROVISIONAL steps: transactionValidationStep (role of TRNVAL00) and
 * positionUpdateStep (role of POSUPD00) are RECONSTRUCTED from PORTVALD.cbl and
 * PORTTRAN.cbl. These tests pin the reconstructed behaviour; they do NOT assert
 * equivalence with the (missing) COBOL originals.
 */
@SpringBootTest
class DailyProcessingJobTest {

    @Autowired
    JobLauncher jobLauncher;
    @Autowired
    JobExplorer jobExplorer;
    @Autowired
    JobOperator jobOperator;
    @Autowired
    @Qualifier("dailyProcessingJob")
    Job dailyProcessingJob;
    @Autowired
    PortfolioRepository portfolioRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    PositionRepository positionRepository;
    @Autowired
    PositionHistoryRepository positionHistoryRepository;
    @Autowired
    ReturnCodeLogRepository returnCodeLogRepository;
    @Autowired
    JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        cleanup();
        portfolioRepository.save(TestData.portfolio());
    }

    @AfterEach
    void cleanup() {
        positionHistoryRepository.deleteAll();
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    private Transaction pending(TransactionType type, String portfolioId, String seq, String qty, String price,
                                String amount, int second) {
        Transaction trn = new Transaction(new TransactionId(TestData.DATE, LocalTime.of(8, 0, second), portfolioId, seq),
                TestData.INVESTMENT_ID, type);
        trn.setQuantity(new java.math.BigDecimal(qty).setScale(4));
        trn.setPrice(new java.math.BigDecimal(price).setScale(4));
        trn.setAmount(new java.math.BigDecimal(amount).setScale(2));
        trn.setStatus(TransactionStatus.PENDING);
        return transactionRepository.save(trn);
    }

    private JobExecution run(JobParameters params) throws Exception {
        return jobLauncher.run(dailyProcessingJob, params);
    }

    private static JobParameters params() {
        return new JobParametersBuilder().addLong("run", System.nanoTime()).toJobParameters();
    }

    @Test
    @DisplayName("[PROVISIONAL] TRNVAL00 -> POSUPD00 -> HISTLD00 -> RPTPOS00 runs end to end when RC <= 4")
    void dailyFlowCompletesAndChainsSteps() throws Exception {
        pending(TransactionType.BUY, TestData.PORTFOLIO_ID, "000001", "100", "10", "1000.00", 1);
        pending(TransactionType.SELL, TestData.PORTFOLIO_ID, "000002", "40", "12", "480.00", 2);
        // invalid portfolio id format -> rejected by the reconstructed validation step (RC=4, continue)
        pending(TransactionType.BUY, "BAD00001", "000003", "1", "1", "1.00", 3);

        JobExecution execution = run(params());

        assertThat(execution.getStatus()).as(() -> execution.getExitStatus() + " " + execution.getStepExecutions() + " " + execution.getAllFailureExceptions()).isEqualTo(BatchStatus.COMPLETED);
        List<String> steps = execution.getStepExecutions().stream().map(StepExecution::getStepName).toList();
        assertThat(steps).containsExactly("transactionValidationStep", "positionUpdateStep", "historyLoadStep",
                "positionReportStep");

        Map<String, Integer> rcByStep = execution.getStepExecutions().stream().collect(Collectors.toMap(
                StepExecution::getStepName, s -> s.getExecutionContext().getInt(ReturnCodeStepListener.RC_KEY, -1)));
        assertThat(rcByStep.get("transactionValidationStep")).isEqualTo(ReturnCode.WARNING.code());
        assertThat(rcByStep.get("positionUpdateStep")).isEqualTo(ReturnCode.SUCCESS.code());

        List<Transaction> all = transactionRepository.findAll();
        assertThat(all).filteredOn(t -> t.getId().getSequenceNo().equals("000003"))
                .extracting(Transaction::getStatus).containsExactly(TransactionStatus.FAILED);
        assertThat(all).filteredOn(t -> !t.getId().getSequenceNo().equals("000003"))
                .extracting(Transaction::getStatus).containsOnly(TransactionStatus.DONE);

        Position position = positionRepository.findAll().get(0);
        assertThat(position.getQuantity()).isEqualByComparingTo("60");
        assertThat(position.getCostBasis()).isEqualByComparingTo("520.00");
        assertThat(positionHistoryRepository.count()).isEqualTo(2);

        assertThat(returnCodeLogRepository.findAll()).extracting(r -> r.getId().getProgramId())
                .contains("TRNVAL00", "POSUPD00", "HISTLD00", "RPTPOS00");
    }

    @Test
    @DisplayName("[PROVISIONAL] positionUpdateStep marks a SELL without units as FAILED and continues (RC=4)")
    void positionUpdateFailureIsSoft() throws Exception {
        pending(TransactionType.SELL, TestData.PORTFOLIO_ID, "000010", "5", "1", "5.00", 10);

        JobExecution execution = run(params());

        assertThat(execution.getStatus()).as(() -> execution.getExitStatus() + " " + execution.getStepExecutions() + " " + execution.getAllFailureExceptions()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution posupd = execution.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals("positionUpdateStep")).findFirst().orElseThrow();
        assertThat(posupd.getExecutionContext().getInt(ReturnCodeStepListener.RC_KEY)).isEqualTo(ReturnCode.WARNING.code());
        assertThat(transactionRepository.findAll().get(0).getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(positionHistoryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Spring Batch restart replaces CKPRST: a failed instance can be restarted with the same parameters")
    void jobIsRestartable() throws Exception {
        pending(TransactionType.BUY, TestData.PORTFOLIO_ID, "000020", "10", "1", "10.00", 20);
        JobParameters params = params();
        JobExecution first = run(params);
        assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Simulate an abend after step 1 by marking the execution FAILED in the repository (what a crash leaves behind)
        first.setStatus(BatchStatus.FAILED);
        first.setEndTime(java.time.LocalDateTime.now());
        jobRepositoryUpdate(first);

        Long restartedId = jobOperator.restart(first.getId());
        JobExecution restarted = jobExplorer.getJobExecution(restartedId);
        assertThat(restarted.getJobInstance().getId()).isEqualTo(first.getJobInstance().getId());
        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // completed steps are not re-run on restart (allowStartIfComplete=false by default)
        assertThat(restarted.getStepExecutions()).allMatch(s -> s.getReadCount() == 0);
        assertThat(positionHistoryRepository.count()).isEqualTo(1);
    }

    private void jobRepositoryUpdate(JobExecution execution) {
        jobRepository.update(execution);
    }
}
