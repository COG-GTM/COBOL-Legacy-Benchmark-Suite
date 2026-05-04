package com.portfolio.batch;

import com.portfolio.domain.AuditLog;
import com.portfolio.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Audit Report Job - migrated from COBOL RPTAUD00.cbl.
 * Reads audit data from DB, generates audit reports.
 */
@Configuration
public class AuditReportJob {

    private static final Logger log = LoggerFactory.getLogger(AuditReportJob.class);

    private final AuditLogRepository auditLogRepository;

    public AuditReportJob(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Bean
    public Job auditReportBatchJob(JobRepository jobRepository, Step auditReportStep) {
        return new JobBuilder("auditReportJob", jobRepository)
                .start(auditReportStep)
                .build();
    }

    @Bean
    public Step auditReportStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("auditReportStep", jobRepository)
                .<AuditLog, String>chunk(100, transactionManager)
                .reader(auditReportReader())
                .writer(auditReportWriter())
                .build();
    }

    @Bean
    public ItemReader<AuditLog> auditReportReader() {
        return new ListItemReader<>(auditLogRepository.findAll());
    }

    @Bean
    public ItemWriter<String> auditReportWriter() {
        return items -> {
            for (String line : items) {
                log.info("AUDIT REPORT: {}", line);
            }
            log.info("Audit Report completed: {} entries", items.size());
        };
    }
}
