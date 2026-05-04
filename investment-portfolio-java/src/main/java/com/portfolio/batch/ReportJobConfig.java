package com.portfolio.batch;

import com.portfolio.entity.AuditLog;
import com.portfolio.entity.InvestmentPosition;
import com.portfolio.repository.AuditLogRepository;
import com.portfolio.repository.InvestmentPositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class ReportJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ReportJobConfig.class);

    @Bean
    public Job positionReportJob(JobRepository jobRepository, Step positionReportStep) {
        return new JobBuilder("positionReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(positionReportStep)
                .build();
    }

    @Bean
    public Step positionReportStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   InvestmentPositionRepository positionRepository) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("Generating position report");
            List<InvestmentPosition> positions = positionRepository.findAll();
            log.info("Position report: {} records processed", positions.size());
            for (InvestmentPosition pos : positions) {
                log.info("Position: portfolio={} investment={} qty={} value={}",
                        pos.getPortfolioId(), pos.getInvestmentId(),
                        pos.getQuantity(), pos.getMarketValue());
            }
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("positionReportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job auditReportJob(JobRepository jobRepository, Step auditReportStep) {
        return new JobBuilder("auditReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(auditReportStep)
                .build();
    }

    @Bean
    public Step auditReportStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                AuditLogRepository auditLogRepository) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("Generating audit report");
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(1);
            List<AuditLog> audits = auditLogRepository.findByTimestampBetween(start, end);
            log.info("Audit report: {} records for last 24 hours", audits.size());
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("auditReportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job statisticsReportJob(JobRepository jobRepository, Step statisticsReportStep) {
        return new JobBuilder("statisticsReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(statisticsReportStep)
                .build();
    }

    @Bean
    public Step statisticsReportStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     InvestmentPositionRepository positionRepository) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info("Generating statistics report");
            long totalPositions = positionRepository.count();
            log.info("Statistics: total positions = {}", totalPositions);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("statisticsReportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
