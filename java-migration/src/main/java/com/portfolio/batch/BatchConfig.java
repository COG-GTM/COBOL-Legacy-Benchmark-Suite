package com.portfolio.batch;

import com.portfolio.batch.report.AuditReportService;
import com.portfolio.batch.report.PositionReportService;
import com.portfolio.batch.report.ReturnCodeAnalysisService;
import com.portfolio.batch.report.StatsReportService;
import com.portfolio.common.BatchConstants;
import com.portfolio.domain.Transaction;
import com.portfolio.domain.TransactionStatus;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.repository.TransactionRepository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TransactionRepository transactionRepository;
  private final PositionHistoryRepository historyRepository;
  private final DuplicateSkippingHistoryWriter historyWriter;
  private final TransactionToPositionHistoryProcessor historyProcessor;
  private final TransactionValidationTasklet validationTasklet;
  private final PositionUpdateTasklet positionUpdateTasklet;
  private final ReturnCodeRecordingListener returnCodeRecordingListener;
  private final PositionReportService positionReportService;
  private final AuditReportService auditReportService;
  private final StatsReportService statsReportService;
  private final ReturnCodeAnalysisService returnCodeAnalysisService;
  private final Path reportDirectory;

  public BatchConfig(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      TransactionRepository transactionRepository,
      PositionHistoryRepository historyRepository,
      TransactionToPositionHistoryProcessor historyProcessor,
      TransactionValidationTasklet validationTasklet,
      PositionUpdateTasklet positionUpdateTasklet,
      ReturnCodeRecordingListener returnCodeRecordingListener,
      PositionReportService positionReportService,
      AuditReportService auditReportService,
      StatsReportService statsReportService,
      ReturnCodeAnalysisService returnCodeAnalysisService,
      @Value("${portfolio.reports.dir:target/reports}") String reportDirectory) {
    this.jobRepository = jobRepository;
    this.transactionManager = transactionManager;
    this.transactionRepository = transactionRepository;
    this.historyRepository = historyRepository;
    this.historyWriter = new DuplicateSkippingHistoryWriter(historyRepository);
    this.historyProcessor = historyProcessor;
    this.validationTasklet = validationTasklet;
    this.positionUpdateTasklet = positionUpdateTasklet;
    this.returnCodeRecordingListener = returnCodeRecordingListener;
    this.positionReportService = positionReportService;
    this.auditReportService = auditReportService;
    this.statsReportService = statsReportService;
    this.returnCodeAnalysisService = returnCodeAnalysisService;
    this.reportDirectory = Paths.get(reportDirectory);
  }

  @Bean
  RepositoryItemReader<Transaction> historyReader() {
    Map<String, Sort.Direction> sorts = new LinkedHashMap<>();
    sorts.put("transactionDate", Sort.Direction.ASC);
    sorts.put("transactionTime", Sort.Direction.ASC);
    return new RepositoryItemReaderBuilder<Transaction>()
        .name("histLoadReader")
        .repository(transactionRepository)
        .methodName("findByStatus")
        .arguments(TransactionStatus.DONE)
        .pageSize(BatchConstants.HISTLD_COMMIT_THRESHOLD)
        .sorts(sorts)
        .build();
  }

  @Bean
  Step histLoadStep(RepositoryItemReader<Transaction> historyReader) {
    return new StepBuilder("histLoadStep", jobRepository)
        .<Transaction, com.portfolio.domain.PositionHistory>chunk(
            BatchConstants.HISTLD_COMMIT_THRESHOLD, transactionManager)
        .reader(historyReader)
        .processor(historyProcessor)
        .writer(historyWriter)
        .faultTolerant()
        .skip(DataIntegrityViolationException.class)
        .skipLimit(Integer.MAX_VALUE)
        .listener(new HistLoadStepListener(historyWriter))
        .listener(returnCodeRecordingListener)
        .build();
  }

  @Bean
  ExecutionContextPromotionListener validationPromotionListener() {
    return promotionListener();
  }

  @Bean
  ExecutionContextPromotionListener positionPromotionListener() {
    return promotionListener();
  }

  private ExecutionContextPromotionListener promotionListener() {
    ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {"returnCode"});
    return listener;
  }

  @Bean
  Step trnValStep(
      @Qualifier("validationPromotionListener")
          ExecutionContextPromotionListener promotionListener) {
    return new StepBuilder("trnValStep", jobRepository)
        .tasklet(validationTasklet, transactionManager)
        .listener(promotionListener)
        .listener(returnCodeRecordingListener)
        .build();
  }

  @Bean
  Step posUpdStep(
      @Qualifier("positionPromotionListener") ExecutionContextPromotionListener promotionListener) {
    return new StepBuilder("posUpdStep", jobRepository)
        .tasklet(positionUpdateTasklet, transactionManager)
        .listener(promotionListener)
        .listener(returnCodeRecordingListener)
        .build();
  }

  @Bean
  Step positionReportStep() {
    return reportStep(
        "positionReportStep", new ReportTasklet(positionReportService::generate, reportDirectory));
  }

  @Bean
  Step auditReportStep() {
    return reportStep(
        "auditReportStep", new ReportTasklet(auditReportService::generate, reportDirectory));
  }

  @Bean
  Step statsReportStep() {
    return reportStep(
        "statsReportStep", new ReportTasklet(statsReportService::generate, reportDirectory));
  }

  @Bean
  Step rtnAnalysisStep() {
    return reportStep(
        "rtnAnalysisStep", new ReportTasklet(returnCodeAnalysisService::generate, reportDirectory));
  }

  private Step reportStep(String name, ReportTasklet tasklet) {
    return new StepBuilder(name, jobRepository)
        .tasklet(tasklet, transactionManager)
        .listener(returnCodeRecordingListener)
        .build();
  }

  @Bean
  Job histLoadJob(@Qualifier("histLoadStep") Step histLoadStep) {
    return new JobBuilder("histLoadJob", jobRepository).start(histLoadStep).build();
  }

  @Bean
  Job positionReportJob(@Qualifier("positionReportStep") Step positionReportStep) {
    return new JobBuilder("positionReportJob", jobRepository).start(positionReportStep).build();
  }

  @Bean
  Job auditReportJob(@Qualifier("auditReportStep") Step auditReportStep) {
    return new JobBuilder("auditReportJob", jobRepository).start(auditReportStep).build();
  }

  @Bean
  Job statsReportJob(@Qualifier("statsReportStep") Step statsReportStep) {
    return new JobBuilder("statsReportJob", jobRepository).start(statsReportStep).build();
  }

  @Bean
  Job rtnAnalysisJob(@Qualifier("rtnAnalysisStep") Step rtnAnalysisStep) {
    return new JobBuilder("rtnAnalysisJob", jobRepository).start(rtnAnalysisStep).build();
  }

  @Bean
  Job dailyJob(
      @Qualifier("trnValStep") Step trnValStep,
      @Qualifier("posUpdStep") Step posUpdStep,
      @Qualifier("histLoadStep") Step histLoadStep,
      @Qualifier("positionReportStep") Step positionReportStep,
      @Qualifier("auditReportStep") Step auditReportStep,
      @Qualifier("statsReportStep") Step statsReportStep,
      @Qualifier("rtnAnalysisStep") Step rtnAnalysisStep) {
    ReturnCodeDecider validationDecider = new ReturnCodeDecider();
    ReturnCodeDecider updateDecider = new ReturnCodeDecider();
    ReturnCodeDecider historyDecider = new ReturnCodeDecider();
    Flow flow =
        new FlowBuilder<Flow>("dailyFlow")
            .start(trnValStep)
            .next(validationDecider)
            .on("CONTINUE")
            .to(posUpdStep)
            .from(validationDecider)
            .on("WARNING")
            .stopAndRestart(posUpdStep)
            .from(validationDecider)
            .on("ERROR")
            .fail()
            .from(posUpdStep)
            .next(updateDecider)
            .on("CONTINUE")
            .to(histLoadStep)
            .from(updateDecider)
            .on("WARNING")
            .stopAndRestart(histLoadStep)
            .from(updateDecider)
            .on("ERROR")
            .fail()
            .from(histLoadStep)
            .next(historyDecider)
            .on("CONTINUE")
            .to(positionReportStep)
            .from(historyDecider)
            .on("WARNING")
            .stopAndRestart(positionReportStep)
            .from(historyDecider)
            .on("ERROR")
            .fail()
            .from(positionReportStep)
            .next(auditReportStep)
            .next(statsReportStep)
            .next(rtnAnalysisStep)
            .end();
    return new JobBuilder("dailyJob", jobRepository).start(flow).end().build();
  }
}
