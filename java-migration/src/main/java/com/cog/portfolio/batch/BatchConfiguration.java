package com.cog.portfolio.batch;

import com.cog.portfolio.common.BatchConstants;
import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.repository.ReturnCodeLogRepository;
import com.cog.portfolio.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Replaces BCHCTL00 / PRCSEQ00 / CKPRST / RCVPRC00: job state, checkpointing and
 * restart live in the Spring Batch {@code BATCH_*} tables. The daily flow
 * documented as {@code TRNVAL00 -> POSUPD00 -> HISTLD00 -> RPTPOS00} is one job
 * whose steps are chained through {@link ReturnCodeDecider} (RC &lt;= 4 continues).
 */
@Configuration
public class BatchConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

    @ConfigurationProperties(prefix = "portfolio.batch")
    public record BatchProperties(int commitInterval, int maxErrors) {
    }

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties properties;
    private final TransactionRepository transactionRepository;
    private final ReturnCodeLogRepository returnCodeLogRepository;

    public BatchConfiguration(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                              BatchProperties properties, TransactionRepository transactionRepository,
                              ReturnCodeLogRepository returnCodeLogRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.properties = properties;
        this.transactionRepository = transactionRepository;
        this.returnCodeLogRepository = returnCodeLogRepository;
    }

    // ---------------------------------------------------------------- readers/writers

    /** Snapshot of the transactions in a given status at step start (the legacy sequential input file). */
    @Bean
    @StepScope
    public ListItemReader<Transaction> pendingTransactionReader() {
        return statusReader(TransactionStatus.PENDING);
    }

    @Bean
    @StepScope
    public ListItemReader<Transaction> pendingForPositionUpdateReader() {
        return statusReader(TransactionStatus.PENDING);
    }

    /** HISTLD00 input: processed transactions (the TRANSACTION-HISTORY file). */
    @Bean
    @StepScope
    public ListItemReader<Transaction> processedTransactionReader() {
        return statusReader(TransactionStatus.DONE);
    }

    private ListItemReader<Transaction> statusReader(TransactionStatus status) {
        List<Transaction> items = transactionRepository
                .findByStatusOrderByIdTransactionDateAscIdTransactionTimeAscIdSequenceNoAsc(status);
        return new ListItemReader<>(items);
    }

    @Bean
    public RepositoryItemWriter<Transaction> transactionWriter() {
        RepositoryItemWriter<Transaction> writer = new RepositoryItemWriter<>();
        writer.setRepository(transactionRepository);
        writer.setMethodName("save");
        return writer;
    }

    // ---------------------------------------------------------------- steps

    // PROVISIONAL - requiere validación humana
    /** Role of TRNVAL00 ("TRNMAIN"). Reconstructed from PORTVALD.cbl; not a translation of TRNVAL00. */
    @Bean
    public Step transactionValidationStep(ListItemReader<Transaction> pendingTransactionReader,
                                          TransactionValidationProcessor processor,
                                          RepositoryItemWriter<Transaction> transactionWriter) {
        return new StepBuilder("transactionValidationStep", jobRepository)
                .<Transaction, Transaction>chunk(properties.commitInterval(), transactionManager)
                .reader(pendingTransactionReader)
                .processor(processor)
                .writer(transactionWriter)
                .listener(new ReturnCodeStepListener("TRNVAL00", returnCodeLogRepository, processor::getRejectedCount))
                .build();
    }

    // PROVISIONAL - requiere validación humana
    /** Role of POSUPD00 ("POSUPDT"). POSUPDT.cbl is empty in the repo; reconstructed from PORTTRAN maths. */
    @Bean
    public Step positionUpdateStep(ListItemReader<Transaction> pendingForPositionUpdateReader,
                                   PositionUpdateProcessor processor,
                                   RepositoryItemWriter<Transaction> transactionWriter) {
        return new StepBuilder("positionUpdateStep", jobRepository)
                .<Transaction, Transaction>chunk(properties.commitInterval(), transactionManager)
                .reader(pendingForPositionUpdateReader)
                .processor(processor)
                .writer(transactionWriter)
                .listener(new ReturnCodeStepListener("POSUPD00", returnCodeLogRepository, processor::getFailedCount))
                .build();
    }

    /** HISTLD00: chunk size = WS-COMMIT-THRESHOLD (1000); dup keys ignored in the writer. */
    @Bean
    public Step historyLoadStep(ListItemReader<Transaction> processedTransactionReader,
                                HistoryLoadProcessor processor, HistoryLoadWriter writer) {
        return new StepBuilder("historyLoadStep", jobRepository)
                .<Transaction, PositionHistory>chunk(properties.commitInterval(), transactionManager)
                .reader(processedTransactionReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(properties.maxErrors())
                .skip(DataAccessException.class)
                .listener(new ReturnCodeStepListener("HISTLD00", returnCodeLogRepository, () -> 0L))
                .build();
    }

    @Bean
    public Step positionReportStep(ReportService reportService) {
        return taskletStep("positionReportStep", "RPTPOS00", () -> {
            ReportService.PositionReport report = reportService.positionReport();
            log.info("RPTPOS00 report {}: {} positions, total value {}", report.reportDate(),
                    report.lines().size(), report.totalValue());
            report.lines().forEach(l -> log.info("  {} {} qty={} value={} chg={}%", l.portfolioId(),
                    l.description(), l.quantity(), l.currentValue(), l.changePct()));
        });
    }

    @Bean
    public Step auditReportStep(ReportService reportService) {
        return taskletStep("auditReportStep", "RPTAUD00", () -> {
            ReportService.AuditReport report = reportService.auditReport(
                    LocalDateTime.now().minusDays(1), LocalDateTime.now());
            log.info("RPTAUD00 audit records={} byStatus={} errors={} severe={}", report.auditRecords(),
                    report.byStatus(), report.errorRecords(), report.severeErrors());
        });
    }

    @Bean
    public Step statisticsReportStep(ReportService reportService) {
        return taskletStep("statisticsReportStep", "RPTSTA00",
                () -> log.info("RPTSTA00 {}", reportService.statisticsReport()));
    }

    @Bean
    public Step returnCodeAnalysisStep(ReportService reportService) {
        return taskletStep("returnCodeAnalysisStep", "RTNANA00", () -> {
            ReportService.ReturnCodeReport report = reportService.returnCodeReport();
            report.lines().forEach(l -> log.info("RTNANA00 {}", l));
            log.info("RTNANA00 {}", report.totals());
        });
    }

    private Step taskletStep(String name, String programId, Runnable body) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            body.run();
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder(name, jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(new ReturnCodeStepListener(programId, returnCodeLogRepository, () -> 0L))
                .build();
    }

    // ---------------------------------------------------------------- jobs

    /** PRCSEQ daily sequence: TRNVAL00 -> POSUPD00 -> HISTLD00 -> RPTPOS00, each gated on RC <= 4. */
    @Bean
    public Job dailyProcessingJob(Step transactionValidationStep, Step positionUpdateStep,
                                  Step historyLoadStep, Step positionReportStep) {
        // one decider state per gate: a shared instance would collapse into a single flow state
        ReturnCodeDecider afterValidation = new ReturnCodeDecider();
        ReturnCodeDecider afterPositionUpdate = new ReturnCodeDecider();
        ReturnCodeDecider afterHistoryLoad = new ReturnCodeDecider();
        return new JobBuilder(BatchConstants.JOB_DAILY, jobRepository)
                .incrementer(new RunIdIncrementer())
                // ".on(*)" so that COMPLETED_WITH_WARNINGS (RC=4) reaches the decider instead of failing the job
                .start(transactionValidationStep).on("*").to(afterValidation)
                .from(afterValidation).on(ReturnCodeDecider.STOP.getName()).fail()
                .from(afterValidation).on("*").to(positionUpdateStep)
                .from(positionUpdateStep).on("*").to(afterPositionUpdate)
                .from(afterPositionUpdate).on(ReturnCodeDecider.STOP.getName()).fail()
                .from(afterPositionUpdate).on("*").to(historyLoadStep)
                .from(historyLoadStep).on("*").to(afterHistoryLoad)
                .from(afterHistoryLoad).on(ReturnCodeDecider.STOP.getName()).fail()
                .from(afterHistoryLoad).on("*").to(positionReportStep)
                .from(positionReportStep).on("*").end()
                .end()
                .build();
    }

    @Bean
    public Job positionReportJob(Step positionReportStep) {
        return singleStepJob(BatchConstants.JOB_POSITION_REPORT, positionReportStep);
    }

    @Bean
    public Job auditReportJob(Step auditReportStep) {
        return singleStepJob(BatchConstants.JOB_AUDIT_REPORT, auditReportStep);
    }

    @Bean
    public Job statisticsReportJob(Step statisticsReportStep) {
        return singleStepJob(BatchConstants.JOB_STATISTICS_REPORT, statisticsReportStep);
    }

    @Bean
    public Job returnCodeAnalysisJob(Step returnCodeAnalysisStep) {
        return singleStepJob(BatchConstants.JOB_RETURN_CODE_ANALYSIS, returnCodeAnalysisStep);
    }

    @Bean
    public Job historyLoadJob(Step historyLoadStep) {
        return singleStepJob("historyLoadJob", historyLoadStep);
    }

    private Job singleStepJob(String name, Step step) {
        return new JobBuilder(name, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }
}
